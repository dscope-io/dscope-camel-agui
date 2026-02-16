package io.dscope.camel.agui;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dscope.camel.agui.service.AgUiStateDeltaCalculator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgUiStateDeltaCalculatorTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void returnsReplacePatchForDifferentStates() {
        AgUiStateDeltaCalculator calculator = new AgUiStateDeltaCalculator();
        var previous = mapper.valueToTree(java.util.Map.of("count", 1));
        var current = mapper.valueToTree(java.util.Map.of("count", 2));

        var patch = calculator.toJsonPatch(previous, current);

        Assertions.assertEquals(1, patch.size());
        Assertions.assertEquals("replace", patch.getFirst().get("op"));
    }
}
