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

import static io.dscope.camel.agui.protocol.AgUiEventPipelineTest.e;
import static org.junit.jupiter.api.Assertions.*;

import io.dscope.camel.agui.AgUiComponent;
import io.dscope.camel.agui.model.protocol.Interrupt;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

class AgUiOfficialClientTest {
  @Test
  @EnabledIfSystemProperty(named = "agui.interop", matches = "true")
  void officialClientRunsAndResumesAgainstCamel() throws Exception {
    int port = AgUiStreamingIntegrationTest.port();
    try (var camel = new DefaultCamelContext()) {
      camel.addComponent("agui", new AgUiComponent());
      camel.addRoutes(
          new RouteBuilder() {
            public void configure() {
              from("agui:official?transportMode=httpSse&serverUrl=undertow:http://127.0.0.1:"
                      + port)
                  .process(
                      exchange -> {
                        var run =
                            exchange.getProperty(
                                AgUiRunContext.EXCHANGE_PROPERTY, AgUiRunContext.class);
                        var input =
                            exchange.getIn().getBody(com.fasterxml.jackson.databind.JsonNode.class);
                        if (input.path("runId").asText().equals("official-cancel")) {
                          run.cancel();
                          return;
                        }
                        if (!input.has("resume")) {
                          Interrupt interrupt = new Interrupt();
                          interrupt.setId("approval");
                          interrupt.setReason("approval");
                          run.interrupt(List.of(interrupt));
                          return;
                        }
                        run.emit(
                            e(
                                "STATE_SNAPSHOT",
                                "snapshot",
                                AgUiSchema.MAPPER.createObjectNode().put("counter", 1)));
                        run.emit(
                            e(
                                "ACTIVITY_SNAPSHOT",
                                "messageId",
                                "activity",
                                "activityType",
                                "progress",
                                "content",
                                java.util.Map.of("n", 1)));
                        run.emit(
                            e(
                                "ACTIVITY_DELTA",
                                "messageId",
                                "activity",
                                "activityType",
                                "progress",
                                "patch",
                                List.of(
                                    java.util.Map.of("op", "replace", "path", "/n", "value", 2))));
                        run.emit(e("REASONING_START", "messageId", "span"));
                        run.emit(
                            e(
                                "REASONING_MESSAGE_CHUNK",
                                "messageId",
                                "reasoning",
                                "delta",
                                "Preparing an answer"));
                        run.emit(e("REASONING_END", "messageId", "span"));
                        for (String id : List.of("a", "b"))
                          run.emit(e("SUBAGENT_STARTED", "subagentRunId", id, "name", id));
                        for (String id : List.of("a", "b"))
                          run.emit(
                              e(
                                  "TEXT_MESSAGE_CHUNK",
                                  "subagentRunId",
                                  id,
                                  "messageId",
                                  id,
                                  "delta",
                                  id));
                        for (String id : List.of("a", "b"))
                          run.emit(e("SUBAGENT_FINISHED", "subagentRunId", id));
                        run.emit(
                            e(
                                "TOOL_CALL_CHUNK",
                                "toolCallId",
                                "doc",
                                "toolCallName",
                                "document",
                                "delta",
                                "{}"));
                        run.emit(
                            e(
                                "TOOL_CALL_RESULT",
                                "messageId",
                                "doc-result",
                                "toolCallId",
                                "doc",
                                "content",
                                List.of(
                                    java.util.Map.of("type", "text", "text", "Document"),
                                    java.util.Map.of(
                                        "type",
                                        "document",
                                        "source",
                                        java.util.Map.of(
                                            "type",
                                            "url",
                                            "value",
                                            "https://example.com/demo.pdf")))));
                        run.emit(e("TEXT_MESSAGE_CHUNK", "messageId", "answer", "delta", "Hello "));
                        run.emit(e("TEXT_MESSAGE_CHUNK", "delta", "official client"));
                      });
            }
          });
      camel.start();
      Path script =
          Path.of("../tests/agui-client/interoperability.mjs").toAbsolutePath().normalize();
      Process process =
          new ProcessBuilder("node", script.toString(), "http://127.0.0.1:" + port + "/agui/agent")
              .redirectErrorStream(true)
              .start();
      assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Official client timed out");
      String output =
          new String(
              process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
      assertEquals(0, process.exitValue(), output);
    }
  }
}
