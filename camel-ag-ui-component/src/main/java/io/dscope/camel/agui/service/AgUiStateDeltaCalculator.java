package io.dscope.camel.agui.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

public class AgUiStateDeltaCalculator {

    public List<Map<String, Object>> toJsonPatch(JsonNode previous, JsonNode current) {
        if (previous == null) {
            return List.of(Map.of("op", "add", "path", "/", "value", current));
        }
        if (previous.equals(current)) {
            return List.of();
        }
        return List.of(Map.of("op", "replace", "path", "/", "value", current));
    }
}
