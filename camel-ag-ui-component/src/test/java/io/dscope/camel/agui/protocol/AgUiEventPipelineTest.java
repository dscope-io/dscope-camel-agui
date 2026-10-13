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
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.junit.jupiter.api.Test;

class AgUiEventPipelineTest {
  @Test void echoedInputMustMatchLifecycleIdentity() {
    var p=new AgUiEventPipeline(true,s->{});
    var input=AgUiSchema.MAPPER.valueToTree(Map.of("threadId","other","runId","r","messages",List.of()));
    assertThrows(IllegalStateException.class,()->p.accept(e("RUN_STARTED","runId","r","threadId","t","input",input)));
  }
  @Test
  void sequentialRunsRetainConversationButResetUsageAndToolHandoff() {
    var p = started();
    p.accept(e("STATE_SNAPSHOT", "snapshot", Map.of("counter", 1)));
    p.accept(e("TEXT_MESSAGE_CHUNK", "messageId", "m", "delta", "first"));
    p.accept(e("TOOL_CALL_CHUNK", "toolCallId", "tool", "toolCallName", "frontend"));
    p.accept(
        e(
            "RUN_FINISHED",
            "runId",
            "r",
            "threadId",
            "t",
            "usage",
            List.of(Map.of("inputTokens", 1, "outputTokens", 2, "totalTokens", 3))));
    assertEquals(Set.of("tool"), p.pendingToolCallIds());
    p.accept(e("RUN_STARTED", "runId", "r2", "threadId", "t"));
    p.accept(e("RUN_FINISHED", "runId", "r2", "threadId", "t"));
    assertEquals(1, p.result().state().path("counter").asInt());
    assertEquals(2, p.result().messages().size());
    assertNull(p.result().usage());
    assertTrue(p.pendingToolCallIds().isEmpty());
  }

  @Test
  void omittedAttributionInheritsAndEmptyIdRemainsDistinctFromParent() {
    var p = started();
    p.accept(e("STEP_STARTED", "stepName", "tools"));
    p.accept(e("SUBAGENT_STARTED", "subagentRunId", "", "name", "empty-id"));
    p.accept(e("STEP_STARTED", "stepName", "tools", "subagentRunId", ""));
    p.accept(e("TEXT_MESSAGE_START", "messageId", "m", "role", "assistant", "subagentRunId", ""));
    p.accept(e("TEXT_MESSAGE_CONTENT", "messageId", "m", "delta", "inherited"));
    p.accept(e("TEXT_MESSAGE_END", "messageId", "m"));
    p.accept(e("STEP_FINISHED", "stepName", "tools", "subagentRunId", ""));
    p.accept(e("SUBAGENT_FINISHED", "subagentRunId", ""));
    assertThrows(IllegalStateException.class, () -> p.accept(end()));
    p.accept(e("STEP_FINISHED", "stepName", "tools"));
    p.accept(end());
    assertEquals("", p.result().messages().get(0).path("subagentRunId").asText());
    assertEquals("inherited", p.result().messages().get(0).path("content").asText());
  }

  @Test
  void encryptedReasoningAndHistoryCannotChangeOwnership() {
    var p = started();
    p.accept(
        e("REASONING_MESSAGE_CHUNK", "messageId", "m", "delta", "thinking", "subagentRunId", "s"));
    p.accept(e("CUSTOM", "name", "flush", "value", 1, "subagentRunId", "s"));
    assertThrows(
        IllegalStateException.class,
        () ->
            p.accept(
                e(
                    "REASONING_ENCRYPTED_VALUE",
                    "subtype",
                    "message",
                    "entityId",
                    "m",
                    "encryptedValue",
                    "opaque",
                    "subagentRunId",
                    "other")));
    p.accept(
        e(
            "REASONING_ENCRYPTED_VALUE",
            "subtype",
            "message",
            "entityId",
            "m",
            "encryptedValue",
            "opaque"));
    p.accept(end());
    assertEquals("opaque", p.result().messages().get(0).path("encryptedValue").asText());
  }

  @Test
  void pendingCallsAnsweredInInputAreNotNewFrontendHandoffs() {
    var input =
        AgUiSchema.MAPPER.valueToTree(
            Map.of(
                "threadId",
                "t",
                "runId",
                "r",
                "messages",
                List.of(
                    Map.of(
                        "id",
                        "parent",
                        "role",
                        "assistant",
                        "toolCalls",
                        List.of(
                            Map.of(
                                "id",
                                "call",
                                "type",
                                "function",
                                "function",
                                Map.of("name", "tool", "arguments", "{}")))),
                    Map.of(
                        "id",
                        "result",
                        "role",
                        "tool",
                        "toolCallId",
                        "call",
                        "content",
                        "answer"))));
    var p = new AgUiEventPipeline(true, s -> {});
    p.seed(input);
    p.accept(e("RUN_STARTED", "threadId", "t", "runId", "r"));
    p.accept(end());
    assertTrue(p.pendingToolCallIds().isEmpty());
  }

  static ObjectNode e(String type, Object... fields) {
    ObjectNode event = AgUiSchema.MAPPER.createObjectNode().put("type", type);
    for (int i = 0; i < fields.length; i += 2)
      event.set(fields[i].toString(), AgUiSchema.MAPPER.valueToTree(fields[i + 1]));
    return event;
  }

  static AgUiEventPipeline started() {
    var p = new AgUiEventPipeline(true, s -> {});
    p.accept(e("RUN_STARTED", "runId", "r", "threadId", "t"));
    return p;
  }

  static ObjectNode end() {
    return e("RUN_FINISHED", "runId", "r", "threadId", "t");
  }

  @Test
  void expandsChunksAndMergesMetadataWithoutRecursion() {
    var p = started();
    var emitted =
        p.accept(
            e(
                "TEXT_MESSAGE_CHUNK",
                "messageId",
                "m",
                "delta",
                "hello",
                "metadata",
                Map.of("nested", Map.of("a", 1))));
    assertEquals(2, emitted.size());
    p.accept(
        e("TEXT_MESSAGE_CHUNK", "delta", " world", "metadata", Map.of("nested", Map.of("b", 2))));
    assertEquals(2, p.accept(end()).size());
    p.endOfStream();
    var message = p.result().messages().get(0);
    assertEquals("hello world", message.path("content").asText());
    assertFalse(message.path("metadata").path("nested").has("a"));
  }

  @Test
  void assemblesToolsFrontendHandoffAndMultimodalResults() {
    var p = started();
    p.accept(
        e(
            "TOOL_CALL_CHUNK",
            "toolCallId",
            "c",
            "toolCallName",
            "doc",
            "delta",
            "{}",
            "metadata",
            Map.of("trace", 1)));
    var parts =
        List.of(
            Map.of(
                "type",
                "document",
                "source",
                Map.of(
                    "type",
                    "url",
                    "value",
                    "https://example.com/a.pdf",
                    "mimeType",
                    "application/pdf")));
    p.accept(e("TOOL_CALL_RESULT", "toolCallId", "c", "messageId", "m", "content", parts));
    p.accept(end());
    assertEquals(0, p.pendingToolCallIds().size());
    assertTrue(p.result().messages().get(1).path("content").isArray());
    assertEquals(
        1,
        p.result()
            .messages()
            .get(0)
            .path("toolCalls")
            .get(0)
            .path("metadata")
            .path("trace")
            .asInt());
    var frontend = started();
    frontend.accept(e("TOOL_CALL_CHUNK", "toolCallId", "f", "toolCallName", "frontend"));
    frontend.accept(end());
    assertEquals(Set.of("f"), frontend.pendingToolCallIds());
  }

  @Test
  void rejectsMalformedOrderEvenWithValidationDisabled() {
    var p = new AgUiEventPipeline(false, s -> {});
    assertThrows(
        IllegalStateException.class,
        () -> p.accept(e("TEXT_MESSAGE_CONTENT", "messageId", "m", "delta", "x")));
    p.accept(e("RUN_STARTED", "runId", "r", "threadId", "t"));
    p.accept(e("TEXT_MESSAGE_START", "messageId", "m", "role", "assistant"));
    assertThrows(IllegalStateException.class, () -> p.accept(end()));
    assertThrows(IllegalStateException.class, p::endOfStream);
  }

  @Test
  void handlesParallelSubagentsAndRejectsAmbiguousChunksAndWrongOwnership() {
    var p = started();
    for (String id : List.of("a", "b")) {
      p.accept(e("SUBAGENT_STARTED", "subagentRunId", id, "name", id));
      p.accept(e("TEXT_MESSAGE_CHUNK", "subagentRunId", id, "messageId", id, "delta", id));
    }
    assertThrows(
        IllegalStateException.class, () -> p.accept(e("TEXT_MESSAGE_CHUNK", "delta", "ambiguous")));
    for (String id : List.of("a", "b")) p.accept(e("SUBAGENT_FINISHED", "subagentRunId", id));
    p.accept(end());
    assertEquals(2, p.result().messages().size());
    var wrong = started();
    wrong.accept(
        e("TEXT_MESSAGE_START", "messageId", "m", "role", "assistant", "subagentRunId", "a"));
    assertThrows(
        IllegalStateException.class,
        () ->
            wrong.accept(
                e("TEXT_MESSAGE_CONTENT", "messageId", "m", "delta", "x", "subagentRunId", "b")));
  }

  @Test
  void reasoningActivityAndStateAreReconstructed() {
    var p = started();
    p.accept(e("REASONING_START", "messageId", "span"));
    p.accept(e("REASONING_MESSAGE_CHUNK", "messageId", "thought", "delta", "think"));
    p.accept(e("REASONING_END", "messageId", "span"));
    p.accept(
        e(
            "REASONING_ENCRYPTED_VALUE",
            "entityId",
            "thought",
            "subtype",
            "message",
            "encryptedValue",
            "opaque"));
    p.accept(
        e(
            "ACTIVITY_SNAPSHOT",
            "messageId",
            "activity",
            "activityType",
            "progress",
            "content",
            Map.of("n", 1)));
    p.accept(
        e(
            "ACTIVITY_DELTA",
            "messageId",
            "activity",
            "activityType",
            "progress",
            "patch",
            List.of(Map.of("op", "replace", "path", "/n", "value", 2))));
    p.accept(e("STATE_SNAPSHOT", "snapshot", Map.of("n", 1)));
    p.accept(e("STATE_DELTA", "delta", List.of(Map.of("op", "replace", "path", "/n", "value", 2))));
    p.accept(e("RAW", "event", Map.of("native", true)));
    p.accept(e("CUSTOM", "name", "custom", "value", Map.of("key", 1)));
    p.accept(end());
    assertEquals("opaque", p.result().messages().get(0).path("encryptedValue").asText());
    assertEquals(2, p.result().state().path("n").asInt());
    assertEquals(2, p.result().messages().get(1).path("content").path("n").asInt());
  }

  @Test
  void permitsLateErrorAndSequentialRunsButNeverReportsFailureAsSuccess() {
    var p = started();
    p.accept(end());
    p.accept(e("RUN_ERROR", "message", "late"));
    assertTrue(p.isFailed());
    assertThrows(
        IllegalStateException.class, () -> p.accept(e("CUSTOM", "name", "late", "value", 1)));
    p.accept(e("RUN_STARTED", "runId", "r2", "threadId", "t"));
    p.accept(
        e("RUN_FINISHED", "runId", "r2", "threadId", "t", "outcome", Map.of("type", "cancelled")));
    assertFalse(p.isFailed());
    assertEquals("cancelled", p.result().outcome().path("type").asText());
  }

  @Test
  void futureMaterialIsRemovedBeforeApplicationDeliveryButMalformedValuesFail() {
    List<String> warnings = new ArrayList<>();
    var p = new AgUiEventPipeline(true, warnings::add);
    p.accept(e("RUN_STARTED", "runId", "r", "threadId", "t", "future", 1));
    assertTrue(p.accept(e("FUTURE_EVENT", "data", 1)).isEmpty());
    var events =
        p.accept(e("TEXT_MESSAGE_START", "messageId", "m", "role", "assistant", "future", 1));
    assertFalse(events.get(0).has("future"));
    assertThrows(
        IllegalArgumentException.class,
        () -> p.accept(e("TEXT_MESSAGE_CONTENT", "messageId", 7, "delta", "x")));
    assertFalse(warnings.isEmpty());
  }

  @Test
  void inputSanitizerPreservesOpenDataAndRemovesUnknownNestedParts() throws Exception {
    JsonNode input =
        AgUiSchema.MAPPER.readTree(
            "{\"threadId\":\"t\",\"runId\":\"r\",\"messages\":[{\"id\":\"u\",\"role\":\"user\",\"content\":[{\"type\":\"future\",\"x\":1},{\"type\":\"text\",\"text\":\"hello\"}]}],\"forwardedProps\":{\"anything\":true},\"future\":1}");
    var clean = AgUiSchema.receive("RunAgentInput", input, true, s -> {});
    assertFalse(clean.has("future"));
    assertTrue(clean.path("forwardedProps").path("anything").asBoolean());
    assertEquals(1, clean.path("messages").get(0).path("content").size());
  }

  @Test
  void compatibilityTranslationPrecedesMiddleware() {
    List<String> types = new ArrayList<>();
    var p =
        new AgUiEventPipeline(
            true,
            s -> {},
            List.of(
                event -> {
                  types.add(event.path("type").asText());
                  return event;
                }));
    p.accept(e("RUN_STARTED", "runId", "r", "threadId", "t"));
    p.accept(e("THINKING_START"));
    p.accept(e("THINKING_END"));
    p.accept(end());
    assertTrue(types.contains("REASONING_START"));
    assertFalse(types.contains("THINKING_START"));
  }

  @Test
  void patchSupportsArraysEscapedPathsCopyMoveTestAndRoot() throws Exception {
    JsonNode original = AgUiSchema.MAPPER.readTree("{\"a/b\":[1,2]}");
    JsonNode patch =
        AgUiSchema.MAPPER.readTree(
            "[{\"op\":\"add\",\"path\":\"/a~1b/-\",\"value\":3},{\"op\":\"copy\",\"from\":\"/a~1b/0\",\"path\":\"/c\"},{\"op\":\"move\",\"from\":\"/a~1b/0\",\"path\":\"/a~1b/1\"},{\"op\":\"test\",\"path\":\"/c\",\"value\":1}]");
    assertEquals("{\"a/b\":[2,1,3],\"c\":1}", AgUiJsonPatch.apply(original, patch).toString());
    assertEquals(2, original.path("a/b").size());
    assertThrows(
        IllegalArgumentException.class,
        () ->
            AgUiJsonPatch.apply(
                original,
                AgUiSchema.MAPPER.valueToTree(
                    List.of(Map.of("op", "test", "path", "/a~1b/0", "value", 0)))));
  }
}
