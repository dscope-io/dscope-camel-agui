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
import io.dscope.camel.persistence.core.FlowStateStore;
import java.time.Instant;
import java.util.Map;

public class PersistentAgUiStateStore implements AgUiStateStore {

    private static final String FLOW_TYPE = "agui.state";
    private final FlowStateStore stateStore;

    public PersistentAgUiStateStore(FlowStateStore stateStore) {
        this.stateStore = stateStore;
    }

    @Override
    public JsonNode get(String runId) {
        JsonNode snapshot = stateStore.rehydrate(FLOW_TYPE, runId).envelope().snapshot();
        return snapshot == null || snapshot.isMissingNode() || snapshot.isEmpty() ? null : snapshot;
    }

    @Override
    public void put(String runId, JsonNode state) {
        stateStore.writeSnapshot(FLOW_TYPE, runId, 1L, state, Map.of("updatedAt", Instant.now().toString()));
    }
}
