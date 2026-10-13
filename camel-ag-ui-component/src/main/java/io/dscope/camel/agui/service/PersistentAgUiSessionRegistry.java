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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.dscope.camel.agui.model.AgUiEvent;
import io.dscope.camel.agui.model.AgUiRunError;
import io.dscope.camel.persistence.core.FlowStateStore;
import io.dscope.camel.persistence.core.PersistedEvent;
import io.dscope.camel.persistence.core.RehydrationPolicy;
import io.dscope.camel.persistence.core.exception.OptimisticConflictException;

public class PersistentAgUiSessionRegistry implements AgUiSessionRegistry {

    private static final String FLOW_TYPE = "agui.run";
    private static final int DEFAULT_MAX_SESSIONS = 10_000;

    private final ConcurrentMap<String, SessionState> sessions = new ConcurrentHashMap<>();
    private final AgUiEventCodec codec;
    private final FlowStateStore stateStore;
    private final RehydrationPolicy policy;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final int maxSessions;

    public PersistentAgUiSessionRegistry(AgUiEventCodec codec, FlowStateStore stateStore, RehydrationPolicy policy) {
        this(codec, stateStore, policy, DEFAULT_MAX_SESSIONS);
    }

    public PersistentAgUiSessionRegistry(AgUiEventCodec codec, FlowStateStore stateStore, RehydrationPolicy policy, int maxSessions) {
        this.codec = codec;
        this.stateStore = stateStore;
        this.policy = policy == null ? RehydrationPolicy.DEFAULT : policy;
        this.maxSessions = Math.max(1, maxSessions);
    }

    @Override
    public AgUiSession getOrCreate(String runId, String sessionId) {
        if (!sessions.containsKey(runId) && sessions.size() >= maxSessions) {
            throw new IllegalStateException("AG-UI session capacity has been reached");
        }
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
        int resolved = Math.max(1, limit);
        List<AgUiSessionEventRecord> persisted = new ArrayList<>();
        for (PersistedEvent event : stateStore.readEvents(FLOW_TYPE, runId, afterSequence, resolved)) {
            AgUiSessionEventRecord record = toRecord(event);
            if (record != null) {
                persisted.add(record);
            }
        }
        if (!persisted.isEmpty()) {
            return persisted;
        }

        SessionState state = sessions.get(runId);
        if (state == null || !state.loaded()) {
            return List.of();
        }
        return state.eventsSince(afterSequence, resolved);
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
        long version = rehydrated.envelope().version();
        boolean loaded = false;
        if (snapshot != null && !snapshot.isMissingNode() && !snapshot.isEmpty()) {
            sessionId = snapshot.path("sessionId").asText(sessionId);
            sequence = snapshot.path("sequence").asLong(0L);
            loaded = true;
        }

        for (PersistedEvent event : rehydrated.tailEvents()) {
            String recoveredSessionId = sessionIdFrom(event);
            if (recoveredSessionId != null) {
                sessionId = recoveredSessionId;
                break;
            }
        }
        SessionState state = new SessionState(runId, sessionId, sequence, version, loaded);
        for (PersistedEvent event : rehydrated.tailEvents()) {
            state.replay(event);
        }
        return state;
    }

    private String sessionIdFrom(PersistedEvent event) {
        JsonNode payload = event.payload();
        if (payload != null) {
            String sessionId = payload.path("sessionId").asText();
            if (!sessionId.isBlank()) {
                return sessionId;
            }
        }
        AgUiSessionEventRecord record = toRecord(event);
        if (record == null) {
            return null;
        }
        try {
            JsonNode json = mapper.readTree(record.json());
            String sessionId = json.path("sessionId").asText(json.path("threadId").asText());
            return sessionId.isBlank() ? null : sessionId;
        } catch (Exception ignored) {
            return null;
        }
    }

    private AgUiSessionEventRecord toRecord(PersistedEvent persistedEvent) {
        JsonNode payload = persistedEvent.payload();
        if (payload == null || payload.path("json").isMissingNode()) {
            return null;
        }
        String eventType = payload.path("eventType").asText("message");
        String json = payload.path("json").asText("{}");
        return new AgUiSessionEventRecord(persistedEvent.sequence(), eventType, json);
    }

    private final class SessionState implements AgUiSession {

        private final String runId;
        private final String sessionId;
        private static final int MAX_CACHED_EVENTS = 1_000;
        private final List<AgUiSessionEventRecord> events = new ArrayList<>();
        private final AtomicLong sequence;
        private final AtomicLong version;
        private volatile boolean loaded;

        private SessionState(String runId, String sessionId, long sequence, long version, boolean loaded) {
            this.runId = runId;
            this.sessionId = sessionId;
            this.sequence = new AtomicLong(sequence);
            this.version = new AtomicLong(version);
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
            String json = codec.toJson(event);
            for (int attempt = 0; attempt < 3; attempt++) {
                long next = sequence.incrementAndGet();
                PersistedEvent persistedEvent = new PersistedEvent(
                    "agui-" + runId + "-" + next,
                    FLOW_TYPE, runId, next, event.getType(),
                    mapper.valueToTree(Map.of("json", json, "eventType", event.getType(), "sessionId", sessionId)),
                    Instant.now().toString(), null);
                try {
                    var result = stateStore.appendEvents(FLOW_TYPE, runId, version.get(), List.of(persistedEvent), null);
                    version.set(result.nextVersion());
                    addEvent(new AgUiSessionEventRecord(next, event.getType(), json));
                    if (next % policy.snapshotEveryEvents() == 0) {
                        writeSnapshot();
                    }
                    loaded = true;
                    return next;
                } catch (OptimisticConflictException conflict) {
                    var remote = stateStore.rehydrate(FLOW_TYPE, runId);
                    version.set(remote.envelope().version());
                    sequence.set(Math.max(sequence.get(), remote.envelope().version()));
                }
            }
            throw new IllegalStateException("Unable to persist AG-UI event after concurrent updates");
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

        private void addEvent(AgUiSessionEventRecord event) {
            events.add(event);
            while (events.size() > MAX_CACHED_EVENTS) {
                events.remove(0);
            }
        }

        private synchronized void replay(PersistedEvent persistedEvent) {
            AgUiSessionEventRecord record = toRecord(persistedEvent);
            if (record == null) {
                return;
            }
            addEvent(record);
            sequence.set(Math.max(sequence.get(), record.sequence()));
            loaded = true;
        }

        private void writeSnapshot() {
            JsonNode snapshot = mapper.valueToTree(Map.of("runId", runId, "sessionId", sessionId, "sequence", sequence.get()));
            stateStore.writeSnapshot(FLOW_TYPE, runId, sequence.get(), snapshot, Map.of("updatedAt", Instant.now().toString()));
        }
    }
}
