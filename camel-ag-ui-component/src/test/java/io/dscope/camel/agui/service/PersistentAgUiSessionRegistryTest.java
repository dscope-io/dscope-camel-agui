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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.dscope.camel.agui.model.AgUiTextMessageContent;
import io.dscope.camel.persistence.core.AppendResult;
import io.dscope.camel.persistence.core.FlowStateStore;
import io.dscope.camel.persistence.core.PersistedEvent;
import io.dscope.camel.persistence.core.RehydratedState;
import io.dscope.camel.persistence.core.RehydrationPolicy;
import io.dscope.camel.persistence.core.StateEnvelope;
import io.dscope.camel.persistence.core.exception.OptimisticConflictException;

class PersistentAgUiSessionRegistryTest {

    @Test
    void optimisticConflictRetryUsesEnvelopeVersionAfterSnapshot() {
        String runId = "run-" + UUID.randomUUID();
        ConflictingSnapshottedStore store = new ConflictingSnapshottedStore(runId);
        PersistentAgUiSessionRegistry registry = new PersistentAgUiSessionRegistry(
            new JacksonAgUiEventCodec(),
            store,
            RehydrationPolicy.DEFAULT
        );

        AgUiSession session = registry.getOrCreate(runId, "ignored-thread");
        session.emit(new AgUiTextMessageContent(runId, session.getSessionId(), "after-conflict"));

        assertEquals(List.of(4L, 4L), store.expectedVersions);
    }

    private static final class ConflictingSnapshottedStore implements FlowStateStore {

        private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        private final String runId;
        private final List<Long> expectedVersions = new ArrayList<>();

        private ConflictingSnapshottedStore(String runId) {
            this.runId = runId;
        }

        @Override
        public RehydratedState rehydrate(String flowType, String flowId) {
            JsonNode snapshot = mapper.valueToTree(Map.of("runId", runId, "sessionId", "thread-1", "sequence", 4L));
            StateEnvelope envelope = new StateEnvelope(
                flowType,
                flowId,
                4L,
                4L,
                snapshot,
                Instant.now().toString(),
                Map.of()
            );
            return new RehydratedState(envelope, List.of());
        }

        @Override
        public AppendResult appendEvents(
            String flowType,
            String flowId,
            long expectedVersion,
            List<PersistedEvent> events,
            String idempotencyKey
        ) {
            expectedVersions.add(expectedVersion);
            if (expectedVersions.size() == 1) {
                throw new OptimisticConflictException("forced conflict");
            }
            return new AppendResult(expectedVersion, expectedVersion + events.size(), false);
        }

        @Override
        public void writeSnapshot(String flowType, String flowId, long version, JsonNode snapshot, Map<String, Object> metadata) {
        }

        @Override
        public List<PersistedEvent> readEvents(String flowType, String flowId, long afterSequence, int limit) {
            return List.of();
        }
    }
}