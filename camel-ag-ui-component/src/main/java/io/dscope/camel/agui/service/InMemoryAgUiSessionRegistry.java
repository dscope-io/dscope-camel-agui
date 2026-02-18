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

import io.dscope.camel.agui.model.AgUiEvent;
import io.dscope.camel.agui.model.AgUiRunError;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryAgUiSessionRegistry implements AgUiSessionRegistry {

    private final ConcurrentMap<String, SessionState> sessions = new ConcurrentHashMap<>();
    private final AgUiEventCodec codec;

    public InMemoryAgUiSessionRegistry(AgUiEventCodec codec) {
        this.codec = codec;
    }

    @Override
    public AgUiSession getOrCreate(String runId, String sessionId) {
        return sessions.computeIfAbsent(runId, ignored -> new SessionState(runId, sessionId, codec));
    }

    @Override
    public AgUiSession get(String runId) {
        return sessions.get(runId);
    }

    @Override
    public List<AgUiSessionEventRecord> eventsSince(String runId, long afterSequence, int limit) {
        SessionState state = sessions.get(runId);
        if (state == null) {
            return List.of();
        }
        return state.eventsSince(afterSequence, limit);
    }

    @Override
    public int activeSessionCount() {
        return sessions.size();
    }

    private static final class SessionState implements AgUiSession {

        private final String runId;
        private final String sessionId;
        private final List<AgUiSessionEventRecord> events = new ArrayList<>();
        private final AtomicLong sequence = new AtomicLong();
        private final AgUiEventCodec codec;

        private SessionState(String runId, String sessionId, AgUiEventCodec codec) {
            this.runId = runId;
            this.sessionId = sessionId;
            this.codec = codec;
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
            events.add(new AgUiSessionEventRecord(next, event.getType(), codec.toJson(event)));
            return next;
        }

        @Override
        public synchronized void complete() {
            sequence.incrementAndGet();
        }

        @Override
        public synchronized void fail(Throwable error) {
            AgUiRunError runError = new AgUiRunError(runId, sessionId, error.getMessage());
            long next = sequence.incrementAndGet();
            events.add(new AgUiSessionEventRecord(next, runError.getType(), codec.toJson(runError)));
        }

        private synchronized List<AgUiSessionEventRecord> eventsSince(long afterSequence, int limit) {
            List<AgUiSessionEventRecord> selected = new ArrayList<>();
            for (AgUiSessionEventRecord event : events) {
                if (event.sequence() > afterSequence) {
                    selected.add(event);
                }
                if (selected.size() >= limit) {
                    break;
                }
            }
            return selected;
        }
    }
}
