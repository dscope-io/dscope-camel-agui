package io.dscope.camel.agui.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryAgUiStateStore implements AgUiStateStore {

    private final ConcurrentMap<String, JsonNode> states = new ConcurrentHashMap<>();

    @Override
    public JsonNode get(String runId) {
        return states.get(runId);
    }

    @Override
    public void put(String runId, JsonNode state) {
        states.put(runId, state);
    }
}
