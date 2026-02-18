/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dscope.camel.agui.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dscope.camel.agui.model.AgUiEvent;
import io.dscope.camel.agui.model.AgUiRunError;
import io.dscope.camel.persistence.core.FlowStateStore;
import io.dscope.camel.persistence.core.PersistedEvent;
import io.dscope.camel.persistence.core.RehydrationPolicy;
import io.dscope.camel.persistence.core.exception.OptimisticConflictException;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class PersistentAgUiSessionRegistry implements AgUiSessionRegistry {

    private static final String FLOW_TYPE = "agui.run";

    private final ConcurrentMap<String, SessionState> sessions = new ConcurrentHashMap<>();
    private final AgUiEventCodec codec;
    private final FlowStateStore stateStore;
    private final RehydrationPolicy policy;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public PersistentAgUiSessionRegistry(AgUiEventCodec codec, FlowStateStore stateStore, RehydrationPolicy policy) {
        this.codec = codec;
        this.stateStore = stateStore;
        this.policy = policy == null ? RehydrationPolicy.DEFAULT : policy;
    }

    @Override
    public AgUiSession getOrCreate(String runId, String sessionId) {
        return sessions.computeIfAbsent(runId, ignored -> hydrateOrCreate(runId, sessionId));
    }

    @Override
    public AgUiSession get(String runId) {
        SessionState state = sessions.get(runId);
        if (state != null) {
            return state;
        }
        SessionState hydrated = hydrateOrCreate(runId, null);
        return hydrated.loaded() ? sessions.computeIfAbsent(runId, ignored -> hydrated) : null;
    }

    @Override
    public List<AgUiSessionEventRecord> eventsSince(String runId, long afterSequence, int limit) {
        SessionState state = sessions.computeIfAbsent(runId, ignored -> hydrateOrCreate(runId, null));
        if (!state.loaded()) {
            return List.of();
        }
        return state.eventsSince(afterSequence, limit);
    }

    @Override
    public int activeSessionCount() {
        return sessions.size();
    }

    private SessionState hydrateOrCreate(String runId, String fallbackSessionId) {
        var rehydrated = stateStore.rehydrate(FLOW_TYPE, runId);
        JsonNode snapshot = rehydrated.envelope().snapshot();

        String sessionId = fallbackSessionId == null ? runId : fallbackSessionId;
        long sequence = 0L;
        boolean loaded = false;
        if (snapshot != null && !snapshot.isMissingNode() && !snapshot.isEmpty()) {
            sessionId = snapshot.path("sessionId").asText(sessionId);
            sequence = snapshot.path("sequence").asLong(0L);
            loaded = true;
        }

        SessionState state = new SessionState(runId, sessionId, sequence, loaded);
        for (PersistedEvent event : rehydrated.tailEvents()) {
            state.replay(event);
        }
        return state;
    }

    private final class SessionState implements AgUiSession {

        private final String runId;
        private final String sessionId;
        private final List<AgUiSessionEventRecord> events = new ArrayList<>();
        private final AtomicLong sequence;
        private volatile boolean loaded;

        private SessionState(String runId, String sessionId, long sequence, boolean loaded) {
            this.runId = runId;
            this.sessionId = sessionId;
            this.sequence = new AtomicLong(sequence);
            this.loaded = loaded;
        }

        private boolean loaded() {
            return loaded;
        }

        @Override
        public String getRunId() {
            return runId;
        }

        @Override
        public String getSessionId() {
            return sessionId;
        }

        @Override
        public synchronized long emit(AgUiEvent event) {
            if (event.getThreadId() == null || event.getThreadId().isBlank()) {
                event.setThreadId(sessionId);
            }
            long next = sequence.incrementAndGet();
            String json = codec.toJson(event);
            events.add(new AgUiSessionEventRecord(next, event.getType(), json));

            PersistedEvent persistedEvent = new PersistedEvent(
                "agui-" + runId + "-" + next,
                FLOW_TYPE,
                runId,
                next,
                event.getType(),
                mapper.valueToTree(Map.of("json", json, "eventType", event.getType())),
                Instant.now().toString(),
                null
            );

            try {
                stateStore.appendEvents(FLOW_TYPE, runId, next - 1, List.of(persistedEvent), null);
            } catch (OptimisticConflictException conflict) {
                long actual = stateStore.rehydrate(FLOW_TYPE, runId).tailEvents().size();
                stateStore.appendEvents(FLOW_TYPE, runId, actual, List.of(persistedEvent), null);
            }

            if (next % policy.snapshotEveryEvents() == 0) {
                writeSnapshot();
            }
            loaded = true;
            return next;
        }

        @Override
        public synchronized void complete() {
            writeSnapshot();
        }

        @Override
        public synchronized void fail(Throwable error) {
            AgUiRunError runError = new AgUiRunError(runId, sessionId, error == null ? "unknown" : error.getMessage());
            emit(runError);
        }

        private synchronized List<AgUiSessionEventRecord> eventsSince(long afterSequence, int limit) {
            int resolved = Math.max(1, limit);
            List<AgUiSessionEventRecord> selected = new ArrayList<>();
            for (AgUiSessionEventRecord event : events) {
                if (event.sequence() > afterSequence) {
                    selected.add(event);
                }
                if (selected.size() >= resolved) {
                    break;
                }
            }
            return selected;
        }

        private synchronized void replay(PersistedEvent persistedEvent) {
            JsonNode payload = persistedEvent.payload();
            if (payload == null || payload.path("json").isMissingNode()) {
                return;
            }
            long seq = persistedEvent.sequence();
            String eventType = payload.path("eventType").asText("message");
            String json = payload.path("json").asText("{}");
            events.add(new AgUiSessionEventRecord(seq, eventType, json));
            sequence.set(Math.max(sequence.get(), seq));
            loaded = true;
        }

        private void writeSnapshot() {
            JsonNode snapshot = mapper.valueToTree(Map.of("runId", runId, "sessionId", sessionId, "sequence", sequence.get()));
            stateStore.writeSnapshot(FLOW_TYPE, runId, sequence.get(), snapshot, Map.of("updatedAt", Instant.now().toString()));
        }
    }
}
