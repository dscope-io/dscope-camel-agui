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
