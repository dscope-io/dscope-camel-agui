package io.dscope.camel.agui.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface AgUiStateStore {

    JsonNode get(String runId);

    void put(String runId, JsonNode state);
}
