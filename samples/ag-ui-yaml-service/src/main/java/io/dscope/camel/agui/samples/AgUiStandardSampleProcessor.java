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

package io.dscope.camel.agui.samples;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.dscope.camel.agui.config.AgUiExchangeProperties;
import io.dscope.camel.agui.model.*;
import io.dscope.camel.agui.model.protocol.Interrupt;
import io.dscope.camel.agui.protocol.*;
import io.dscope.camel.agui.service.AgUiSession;
import java.util.*;
import org.apache.camel.*;

/**
 * Deterministic demonstrations for the standard HTTP/SSE endpoint; no model credential is required.
 */
public final class AgUiStandardSampleProcessor implements Processor {
  public void process(Exchange exchange) throws Exception {
    AgUiRunContext run =
        exchange.getProperty(AgUiRunContext.EXCHANGE_PROPERTY, AgUiRunContext.class);
    JsonNode input = exchange.getIn().getBody(JsonNode.class);
    String runId = input.path("runId").asText(), threadId = input.path("threadId").asText();
    String text = "Acknowledged.";
    for (JsonNode message : input.path("messages"))
      if (message.path("role").asText().equals("user") && message.path("content").isTextual())
        text = message.get("content").asText();
    if (text.toLowerCase(Locale.ROOT).contains("approval") && !input.has("resume")) {
      Interrupt interrupt = new Interrupt();
      interrupt.setId("sample-approval");
      interrupt.setReason("approval");
      interrupt.setMessage("Approve the sample action?");
      run.interrupt(List.of(interrupt));
      return;
    }
    if (text.toLowerCase(Locale.ROOT).contains("demo 1.0")) {
      run.emit(
          event(
              "ACTIVITY_SNAPSHOT",
              "messageId",
              "progress",
              "activityType",
              "demo",
              "content",
              Map.of("progress", 0)));
      run.emit(event("REASONING_START", "messageId", "span"));
      run.emit(
          event(
              "REASONING_MESSAGE_CHUNK",
              "messageId",
              "thought",
              "delta",
              "Preparing two parallel demonstrations."));
      run.emit(event("REASONING_END", "messageId", "span"));
      run.emit(event("SUBAGENT_STARTED", "subagentRunId", "a", "name", "documents"));
      run.emit(event("SUBAGENT_STARTED", "subagentRunId", "b", "name", "summary"));
      run.emit(
          event(
              "TEXT_MESSAGE_CHUNK",
              "subagentRunId",
              "a",
              "messageId",
              "a-text",
              "delta",
              "Found a document."));
      run.emit(
          event(
              "TEXT_MESSAGE_CHUNK",
              "subagentRunId",
              "b",
              "messageId",
              "b-text",
              "delta",
              "Prepared a summary."));
      run.emit(event("SUBAGENT_FINISHED", "subagentRunId", "a"));
      run.emit(event("SUBAGENT_FINISHED", "subagentRunId", "b"));
      run.emit(event("TOOL_CALL_START", "toolCallId", "doc", "toolCallName", "document"));
      run.emit(event("TOOL_CALL_ARGS", "toolCallId", "doc", "delta", "{}"));
      run.emit(event("TOOL_CALL_END", "toolCallId", "doc"));
      run.emit(
          event(
              "TOOL_CALL_RESULT",
              "messageId",
              "document-result",
              "toolCallId",
              "doc",
              "content",
              List.of(
                  Map.of("type", "text", "text", "Sample document"),
                  Map.of(
                      "type",
                      "document",
                      "source",
                      Map.of(
                          "type",
                          "url",
                          "value",
                          "https://example.com/sample.pdf",
                          "mimeType",
                          "application/pdf")))));
      run.emit(
          event(
              "ACTIVITY_DELTA",
              "messageId",
              "progress",
              "activityType",
              "demo",
              "patch",
              List.of(Map.of("op", "replace", "path", "/progress", "value", 100))));
      text = "AG-UI 1.0 demonstration complete.";
    } else {
      Map<String, Object> params = new LinkedHashMap<>();
      params.put("text", text);
      params.put("runId", runId);
      params.put("threadId", threadId);
      params.put("sessionId", threadId);
      exchange.setProperty(AgUiExchangeProperties.PARAMS, params);
      exchange.setProperty(
          AgUiExchangeProperties.SESSION,
          new AgUiSession() {
            public String getRunId() {
              return runId;
            }

            public String getSessionId() {
              return threadId;
            }

            public long emit(AgUiEvent event) {
              run.emit(event);
              return 0;
            }

            public void complete() {}

            public void fail(Throwable error) {
              run.fail(error);
            }
          });
      new AgUiSampleWidgetProcessor().process(exchange);
      text = params.get("text").toString();
    }
    run.emit(new AgUiTextMessageStart(runId, threadId, "answer", "assistant"));
    run.emit(new AgUiTextMessageContent(runId, threadId, "answer", text));
    run.emit(new AgUiTextMessageEnd(runId, threadId, "answer"));
  }

  private static ObjectNode event(String type, Object... fields) {
    ObjectNode e = AgUiSchema.MAPPER.createObjectNode().put("type", type);
    for (int i = 0; i < fields.length; i += 2)
      e.set(fields[i].toString(), AgUiSchema.MAPPER.valueToTree(fields[i + 1]));
    return e;
  }
}
