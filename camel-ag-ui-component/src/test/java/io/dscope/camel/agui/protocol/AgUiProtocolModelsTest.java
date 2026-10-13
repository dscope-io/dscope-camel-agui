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

package io.dscope.camel.agui.protocol;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AgUiProtocolModelsTest {
  @Test void requiredArbitraryJsonRetainsExplicitNull() throws Exception {
    var custom=new io.dscope.camel.agui.model.AgUiCustom("r","t","null",null);
    var raw=new io.dscope.camel.agui.model.AgUiRaw();raw.setEvent(null);
    for(var event:java.util.List.of(custom,raw)) {
      var encoded=AgUiSchema.MAPPER.valueToTree(event);
      AgUiSchema.validate("Event",encoded,true);
      assertTrue(encoded.has(event==custom?"value":"event"));
    }
    var input=AgUiSchema.MAPPER.readTree("{\"op\":\"add\",\"path\":\"/nullable\",\"value\":null}");
    var operation=AgUiSchema.MAPPER.treeToValue(input,io.dscope.camel.agui.model.protocol.AddOperation.class);
    JsonNode encoded=AgUiSchema.MAPPER.valueToTree(operation);
    assertEquals(input,encoded);AgUiSchema.validate("AddOperation",encoded,true);
  }
  @Test
  void everyEventAndSupportingModelRoundTripsAgainstPinnedSchema() throws Exception {
    JsonNode fixtures =
        AgUiSchema.MAPPER.readTree(getClass().getResourceAsStream("/agui-schema-fixtures.json"));
    AtomicInteger eventCount = new AtomicInteger();
    for (var entries = fixtures.fields(); entries.hasNext(); ) {
      var entry = entries.next();
      String name = entry.getKey();
      JsonNode fixture = entry.getValue();
      AgUiSchema.validate(name, fixture.get("value"), true);
      Class<?> type = Class.forName(fixture.get("javaClass").asText());
      Object model = AgUiSchema.MAPPER.treeToValue(fixture.get("value"), type);
      JsonNode encoded = AgUiSchema.MAPPER.valueToTree(model);
      assertDoesNotThrow(() -> AgUiSchema.validate(name, encoded, true), name);
      assertEquals(fixture.get("value"), AgUiSchema.MAPPER.readTree(encoded.toString()), name);
      if (name.endsWith("Event")) eventCount.incrementAndGet();
    }
    assertEquals(31, eventCount.get());
  }

  @Test
  void absentAndExplicitNullRemainDistinctAtSchemaBoundary() throws Exception {
    var input = AgUiSchema.MAPPER.readTree("{\"threadId\":\"t\",\"runId\":\"r\",\"messages\":[]}");
    AgUiSchema.validate("RunAgentInput", input, true);
    ((com.fasterxml.jackson.databind.node.ObjectNode) input).putNull("state");
    assertThrows(
        IllegalArgumentException.class, () -> AgUiSchema.validate("RunAgentInput", input, true));
    assertDoesNotThrow(() -> AgUiSchema.validate("RunAgentInput", input, false));
  }
}
