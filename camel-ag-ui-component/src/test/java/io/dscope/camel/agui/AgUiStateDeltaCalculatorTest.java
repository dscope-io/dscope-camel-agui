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
