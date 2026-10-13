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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Ordered, per-stream protocol interpreter; schema opt-out never disables behavioral verification.
 */
public final class AgUiEventPipeline {
  private final boolean validation;
  private final Consumer<String> warning;
  private final List<UnaryOperator<JsonNode>> middleware;
  private final Map<EntityKey, String> opened = new LinkedHashMap<>();
  private final Map<ChunkKey, Chunk> chunks = new LinkedHashMap<>();
  private final Set<String> announced = new HashSet<>(),
      active = new HashSet<>(),
      ended = new HashSet<>();
  private final Map<String, ObjectNode> messages = new LinkedHashMap<>(),
      calls = new LinkedHashMap<>();
  private final ObjectNode subagents = AgUiSchema.MAPPER.createObjectNode();
  private JsonNode state = AgUiSchema.MAPPER.createObjectNode(), outcome, result, usage;
  private String threadId, runId, oldThinkingId;
  private boolean started, terminal, failed;
  private JsonNode initialInput;
  private final Set<String> answeredCalls = new HashSet<>();
  private final Set<String> runCalls = new LinkedHashSet<>();
  private final Map<String, String> reasoningOwners = new HashMap<>();

  private record EntityKey(String kind, String id, String lane) {}

  private record ChunkKey(String kind, String lane) {}

  private record Chunk(String kind, String id, String owner, ObjectNode start) {}

  public AgUiEventPipeline(boolean validation, Consumer<String> warning) {
    this(validation, warning, List.of());
  }

  public AgUiEventPipeline(
      boolean validation, Consumer<String> warning, List<UnaryOperator<JsonNode>> middleware) {
    this.validation = validation;
    this.warning = Objects.requireNonNull(warning);
    this.middleware = List.copyOf(middleware);
  }

  public void seed(JsonNode input) {
    initialInput = input.deepCopy();
  }

  public synchronized List<JsonNode> accept(JsonNode original) {
    JsonNode e = translate(original);
    for (var stage : middleware) e = Objects.requireNonNull(stage.apply(e));
    e = AgUiSchema.receive("Event", e, validation, warning);
    if (e.isMissingNode()) return List.of();
    List<JsonNode> output = new ArrayList<>();
    String type = text(e, "type");
    if (type.endsWith("_CHUNK")) expand((ObjectNode) e, output);
    else {
      if (Set.of("RUN_STARTED", "RUN_FINISHED", "RUN_ERROR", "MESSAGES_SNAPSHOT").contains(type))
        closeAllChunks(output);
      else if (type.equals("SUBAGENT_FINISHED") || type.equals("SUBAGENT_ERROR"))
        closeChunks(owner(e), output);
      else if (!Set.of(
              "RAW",
              "ACTIVITY_SNAPSHOT",
              "ACTIVITY_DELTA",
              "REASONING_ENCRYPTED_VALUE",
              "SUBAGENT_STARTED")
          .contains(type)) closeChunks(owner(e), output);
      deliver(e, output);
    }
    return List.copyOf(output);
  }

  /**
   * Strict local emission uses the same sequencer, but must not silently strip producer defects.
   */
  public synchronized List<JsonNode> emit(JsonNode e) {
    AgUiSchema.validate("Event", e, validation);
    return accept(e);
  }

  private JsonNode translate(JsonNode source) {
    if (source == null || !source.isObject())
      throw new IllegalArgumentException("Expected AG-UI event object");
    ObjectNode e = (ObjectNode) source.deepCopy();
    String t = text(e, "type");
    if (t.startsWith("THINKING")) {
      warning.accept("Translating retired AG-UI event " + t);
      switch (t) {
        case "THINKING_START" -> {
          oldThinkingId = e.path("messageId").asText(UUID.randomUUID().toString());
          e.put("type", "REASONING_START");
          e.put("messageId", oldThinkingId);
        }
        case "THINKING_END" -> {
          e.put("type", "REASONING_END");
          e.put("messageId", oldThinkingId == null ? "" : oldThinkingId);
        }
        case "THINKING_TEXT_MESSAGE_START" -> {
          e.put("type", "REASONING_MESSAGE_START");
          e.put("role", "reasoning");
        }
        case "THINKING_TEXT_MESSAGE_CONTENT" -> e.put("type", "REASONING_MESSAGE_CONTENT");
        case "THINKING_TEXT_MESSAGE_END" -> e.put("type", "REASONING_MESSAGE_END");
        default -> {}
      }
    }
    return e;
  }

  private static String text(JsonNode e, String field) {
    if (!e.path(field).isTextual()) throw new IllegalArgumentException("Expected string " + field);
    return e.get(field).asText();
  }

  private static String owner(JsonNode e) {
    return e.has("subagentRunId") ? e.get("subagentRunId").asText() : null;
  }

  private static EntityKey key(String kind, String id) {
    return new EntityKey(kind, id, null);
  }

  private static void require(boolean condition, String message) {
    if (!condition) throw new IllegalStateException("AG-UI protocol violation: " + message);
  }

  private void open(String kind, String id, String owner) {
    EntityKey key = kind.equals("STEP") ? new EntityKey(kind, id, owner) : key(kind, id);
    require(!opened.containsKey(key), "duplicate opener " + id);
    opened.put(key, owner);
  }

  private void continuation(String kind, String id, String owner, boolean close) {
    EntityKey k = kind.equals("STEP") ? new EntityKey(kind, id, owner) : key(kind, id);
    require(opened.containsKey(k), "no opener for " + id);
    require(owner == null || Objects.equals(opened.get(k), owner), "attribution differs for " + id);
    if (close) opened.remove(k);
  }

  private void closeChunks(String lane, List<JsonNode> output) {
    for (var entry : new ArrayList<>(chunks.entrySet())) {
      Chunk c = entry.getValue();
      if (!Objects.equals(lane, c.owner())) continue;
      chunks.remove(entry.getKey());
      ObjectNode end = AgUiSchema.MAPPER.createObjectNode();
      end.put("type", c.kind() + "_END");
      end.put(c.kind().equals("TOOL_CALL") ? "toolCallId" : "messageId", c.id());
      if (c.owner() != null) end.put("subagentRunId", c.owner());
      deliver(end, output);
    }
  }

  private void closeAllChunks(List<JsonNode> output) {
    for (Chunk chunk : new ArrayList<>(chunks.values())) closeChunks(chunk.owner(), output);
  }

  private void reasoningOwner(String id, String lane) {
    if (reasoningOwners.containsKey(id))
      require(
          lane == null || Objects.equals(reasoningOwners.get(id), lane), "reasoning owner differs");
    else reasoningOwners.put(id, lane);
  }

  private void expand(ObjectNode event, List<JsonNode> output) {
    String kind = text(event, "type").replace("_CHUNK", "");
    String field = kind.equals("TOOL_CALL") ? "toolCallId" : "messageId";
    String lane = owner(event), id = event.path(field).asText(null);
    if (!event.has("subagentRunId")
        && id == null
        && !chunks.containsKey(new ChunkKey(kind, null))) {
      List<Chunk> candidates = chunks.values().stream().filter(c -> c.kind().equals(kind)).toList();
      require(candidates.size() <= 1, "ambiguous chunk continuation");
      if (candidates.size() == 1) lane = candidates.get(0).owner();
    }
    if (!event.has("subagentRunId") && id != null) {
      List<Chunk> candidates =
          chunks.values().stream().filter(c -> c.kind().equals(kind) && c.id().equals(id)).toList();
      if (candidates.size() == 1) lane = candidates.get(0).owner();
    }
    ChunkKey k = new ChunkKey(kind, lane);
    Chunk current = chunks.get(k);
    if (current != null && id != null && !id.equals(current.id())) {
      closeChunks(lane, output);
      current = null;
    }
    if (current == null) {
      require(id != null, "first chunk has no " + field);
      closeChunks(lane, output);
      ObjectNode start = AgUiSchema.MAPPER.createObjectNode();
      start.put("type", kind + "_START");
      start.put(field, id);
      if (kind.equals("TEXT_MESSAGE")) start.put("role", event.path("role").asText("assistant"));
      if (kind.equals("REASONING_MESSAGE")) start.put("role", "reasoning");
      if (kind.equals("TOOL_CALL")) {
        require(event.has("toolCallName"), "first tool chunk has no name");
        start.set("toolCallName", event.get("toolCallName"));
      }
      for (String f : List.of("parentMessageId", "name"))
        if (event.has(f)) start.set(f, event.get(f));
      if (lane != null) start.put("subagentRunId", lane);
      copyEnvelope(event, start);
      deliver(start, output);
      current = new Chunk(kind, id, lane, start);
      chunks.put(k, current);
    } else {
      for (String f : List.of("role", "toolCallName", "parentMessageId", "name"))
        if (event.has(f))
          require(event.get(f).equals(current.start().get(f)), "conflicting chunk opener " + f);
    }
    if (event.has("delta") || event.has("metadata")) {
      ObjectNode content = AgUiSchema.MAPPER.createObjectNode();
      content.put("type", kind.equals("TOOL_CALL") ? "TOOL_CALL_ARGS" : kind + "_CONTENT");
      content.put(field, current.id());
      content.put("delta", event.path("delta").asText(""));
      if (current.owner() != null) content.put("subagentRunId", current.owner());
      copyEnvelope(event, content);
      deliver(content, output);
    }
  }

  private static void copyEnvelope(JsonNode from, ObjectNode to) {
    for (String f : List.of("metadata", "rawEvent")) if (from.has(f)) to.set(f, from.get(f));
  }

  private void deliver(JsonNode e, List<JsonNode> output) {
    AgUiSchema.validate("Event", e, validation);
    String t = text(e, "type"), lane = owner(e);
    if (t.equals("RUN_STARTED")) {
      require(!started || terminal, "run already active");
      String nextThread=text(e,"threadId");
      if(started && !Objects.equals(threadId,nextThread)) {
        messages.clear();state=AgUiSchema.MAPPER.createObjectNode();
      }
      reset();
      started = true;
      terminal = false;
      failed = false;
      runId = text(e, "runId");
      threadId = text(e, "threadId");
      JsonNode input = e.has("input") ? e.get("input") : initialInput;
      if(e.has("input"))require(input.path("runId").asText().equals(runId),"input run identity mismatch");
      if (input != null && input.path("runId").asText().equals(runId)) {
        require(input.path("threadId").asText().equals(threadId),"input thread identity mismatch");
        if (input.has("state")) state = input.get("state").deepCopy();
        for (JsonNode m : input.path("messages"))
          messages.putIfAbsent(text(m, "id"), (ObjectNode) m.deepCopy());
        reindexCalls();
      }
    } else if (t.equals("RUN_ERROR")) {
      require(!failed, "event after run error");
      failed = true;
      terminal = true;
      opened.clear();
      for(String id:active)((ObjectNode)subagents.get(id)).put("status","abandoned");
      active.clear();
      outcome = e.deepCopy();
      usage = e.get("usage");
    } else {
      require(started && !failed, "event outside active run");
      require(!terminal, "event after terminal event");
      switch (t) {
        case "RUN_FINISHED" -> {
          require(
              text(e, "runId").equals(runId) && text(e, "threadId").equals(threadId),
              "run identity mismatch");
          require(opened.isEmpty() && active.isEmpty(), "run finished with open items");
          terminal = true;
          outcome =
              e.has("outcome")
                  ? e.get("outcome").deepCopy()
                  : AgUiSchema.MAPPER.createObjectNode().put("type", "success");
          result = e.get("result");
          usage = e.get("usage");
          if (outcome.path("type").asText().equals("interrupt")) {
            Set<String> ids = new HashSet<>();
            for (JsonNode i : outcome.path("interrupts"))
              require(ids.add(text(i, "id")), "duplicate interrupt id");
          }
          JsonNode pending = outcome.get("pendingToolCallIds");
          if (pending != null && !pending.isEmpty())
            require(
                new HashSet<>(strings(pending)).equals(pendingToolCallIds()),
                "pending tool calls differ from stream");
        }
        case "STEP_STARTED" -> open("STEP", text(e, "stepName"), lane);
        case "STEP_FINISHED" -> continuation("STEP", text(e, "stepName"), lane, true);
        case "REASONING_START" -> {
          reasoningOwner(text(e, "messageId"), lane);
          open("SPAN", text(e, "messageId"), lane);
        }
        case "REASONING_END" -> continuation("SPAN", text(e, "messageId"), lane, true);
        case "TEXT_MESSAGE_START", "REASONING_MESSAGE_START" -> {
          String id = text(e, "messageId"),
              kind = t.equals("TEXT_MESSAGE_START") ? "TEXT" : "REASONING";
          open(kind, id, lane);
          if (kind.equals("REASONING")) reasoningOwner(id, lane);
          ObjectNode m =
              AgUiSchema.MAPPER
                  .createObjectNode()
                  .put("id", id)
                  .put("role", kind.equals("TEXT") ? text(e, "role") : "reasoning")
                  .put("content", "");
          if (messages.containsKey(id)) {
            m = messages.get(id);
            require(
                !m.path("role").asText().equals("activity"),
                "message id already names an activity");
            require(
                lane == null || Objects.equals(owner(m), lane),
                "message opener attribution differs");
          } else {
            if (e.has("name")) m.set("name", e.get("name"));
            if (lane != null) m.put("subagentRunId", lane);
            messages.put(id, m);
          }
          merge(m, e);
        }
        case "TEXT_MESSAGE_CONTENT",
            "REASONING_MESSAGE_CONTENT",
            "TEXT_MESSAGE_END",
            "REASONING_MESSAGE_END" -> {
          String id = text(e, "messageId"), kind = t.startsWith("TEXT") ? "TEXT" : "REASONING";
          continuation(kind, id, lane, t.endsWith("_END"));
          ObjectNode m = messages.get(id);
          if (t.endsWith("_CONTENT"))
            m.put("content", m.path("content").asText() + text(e, "delta"));
          merge(m, e);
        }
        case "TOOL_CALL_START" -> {
          String id = text(e, "toolCallId");
          open("TOOL", id, lane);
          runCalls.add(id);
          if (calls.containsKey(id)) {
            ObjectNode call = calls.get(id);
            require(
                call.path("function").path("name").asText().equals(text(e, "toolCallName")),
                "reopened tool name differs");
            require(
                lane == null || Objects.equals(toolOwner(id), lane),
                "reopened tool ownership differs");
            merge(call, e);
            break;
          }
          String parent = e.path("parentMessageId").asText("tool-parent-" + id);
          ObjectNode m = messages.get(parent);
          if (m == null) {
            m = AgUiSchema.MAPPER.createObjectNode().put("id", parent).put("role", "assistant");
            if (lane != null) m.put("subagentRunId", lane);
            messages.put(parent, m);
          }
          require(m.path("role").asText().equals("assistant"), "tool parent is not assistant");
          require(lane == null || Objects.equals(owner(m), lane), "tool parent ownership mismatch");
          ObjectNode call =
              AgUiSchema.MAPPER.createObjectNode().put("id", id).put("type", "function");
          call.set(
              "function",
              AgUiSchema.MAPPER
                  .createObjectNode()
                  .put("name", text(e, "toolCallName"))
                  .put("arguments", ""));
          ArrayNode list =
              m.has("toolCalls") ? (ArrayNode) m.get("toolCalls") : m.putArray("toolCalls");
          list.add(call);
          calls.put(id, call);
          merge(call, e);
        }
        case "TOOL_CALL_ARGS", "TOOL_CALL_END" -> {
          String id = text(e, "toolCallId");
          continuation("TOOL", id, lane, t.endsWith("_END"));
          ObjectNode call = calls.get(id);
          if (t.equals("TOOL_CALL_ARGS"))
            ((ObjectNode) call.get("function"))
                .put(
                    "arguments",
                    call.path("function").path("arguments").asText() + text(e, "delta"));
          merge(call, e);
        }
        case "TOOL_CALL_RESULT" -> {
          String id = text(e, "messageId"), call = text(e, "toolCallId");
          require(!opened.containsKey(key("TOOL", call)), "result before tool arguments closed");
          if (calls.containsKey(call))
            require(
                lane == null || Objects.equals(toolOwner(call), lane),
                "tool result attribution differs");
          require(!messages.containsKey(id), "duplicate result message");
          answeredCalls.add(call);
          ObjectNode m =
              AgUiSchema.MAPPER
                  .createObjectNode()
                  .put("id", id)
                  .put("role", "tool")
                  .put("toolCallId", call);
          m.set("content", e.get("content").deepCopy());
          if (lane != null) m.put("subagentRunId", lane);
          messages.put(id, m);
          merge(m, e);
        }
        case "STATE_SNAPSHOT" -> state = e.get("snapshot").deepCopy();
        case "STATE_DELTA" -> state = AgUiJsonPatch.apply(state, e.get("delta"));
        case "MESSAGES_SNAPSHOT" -> {
          Map<String, ObjectNode> snapshot = new LinkedHashMap<>();
          boolean hasReasoning = false, hasActivity = false;
          for (JsonNode message : e.get("messages")) {
            snapshot.put(text(message, "id"), (ObjectNode) message.deepCopy());
            hasReasoning |= message.path("role").asText().equals("reasoning");
            hasActivity |= message.path("role").asText().equals("activity");
          }
          JsonNode authority =
              e.path("metadata").path("@ag-ui/client").get("authoritativeActivityTypes");
          boolean reasoningAuthoritative = hasReasoning, activityAuthoritative = hasActivity;
          messages
              .entrySet()
              .removeIf(
                  entry -> {
                    ObjectNode message = entry.getValue();
                    String role = message.path("role").asText();
                    if (snapshot.containsKey(entry.getKey())) return false;
                    if (role.equals("reasoning") && !reasoningAuthoritative) return false;
                    if (role.equals("activity")) {
                      if (authority != null) {
                        if (authority.isNull()) return true;
                        if (authority.isArray())
                          return strings(authority).contains(message.path("activityType").asText());
                        return false;
                      }
                      return activityAuthoritative;
                    }
                    return true;
                  });
          messages.putAll(snapshot);
          reindexCalls();
        }
        case "ACTIVITY_SNAPSHOT" -> {
          String id = text(e, "messageId");
          ObjectNode m = messages.get(id);
          if (m == null) {
            m =
                AgUiSchema.MAPPER
                    .createObjectNode()
                    .put("id", id)
                    .put("role", "activity")
                    .put("activityType", text(e, "activityType"));
            if (lane != null) m.put("subagentRunId", lane);
            messages.put(id, m);
            m.set("content", e.get("content").deepCopy());
          } else {
            require(m.path("role").asText().equals("activity"), "activity identity differs");
            if (e.path("replace").asBoolean(true)) {
              m.set("content", e.get("content").deepCopy());
              m.set("activityType", e.get("activityType"));
              if (lane == null) m.remove("subagentRunId");
              else m.put("subagentRunId", lane);
            }
          }
          merge(m, e);
        }
        case "ACTIVITY_DELTA" -> {
          ObjectNode m = messages.get(text(e, "messageId"));
          require(
              m != null
                  && m.path("role").asText().equals("activity")
                  && (lane == null || Objects.equals(owner(m), lane)),
              "activity delta without matching snapshot");
          m.set("content", AgUiJsonPatch.apply(m.get("content"), e.get("patch")));
          merge(m, e);
        }
        case "SUBAGENT_STARTED" -> {
          require(!announced.contains(lane) && !ended.contains(lane), "duplicate subagent id");
          if (e.has("parentSubagentRunId"))
            require(
                active.contains(text(e, "parentSubagentRunId")), "subagent parent is not active");
          announced.add(lane);
          active.add(lane);
          subagents.set(lane, e.deepCopy());
        }
        case "SUBAGENT_FINISHED", "SUBAGENT_ERROR" -> {
          require(active.contains(lane), "subagent was not active");
          if (t.equals("SUBAGENT_ERROR"))
            opened.entrySet().removeIf(entry -> Objects.equals(entry.getValue(), lane));
          else require(opened.values().stream().noneMatch(lane::equals), "subagent has open items");
          require(
              active.stream()
                  .noneMatch(
                      id ->
                          subagents.path(id).path("parentSubagentRunId").isTextual()
                              && subagents
                                  .path(id)
                                  .path("parentSubagentRunId")
                                  .asText()
                                  .equals(lane)),
              "subagent has active children");
          active.remove(lane);
          ended.add(lane);
          ((ObjectNode) subagents.get(lane)).setAll((ObjectNode) e);
        }
        case "REASONING_ENCRYPTED_VALUE" -> {
          String id = text(e, "entityId");
          ObjectNode entity =
              e.path("subtype").asText().equals("tool-call") ? calls.get(id) : messages.get(id);
          if (entity == null) warning.accept("Encrypted reasoning has no referent " + id);
          else {
            String actualOwner =
                e.path("subtype").asText().equals("tool-call") ? toolOwner(id) : owner(entity);
            require(
                lane == null || Objects.equals(actualOwner, lane),
                "encrypted reasoning attribution differs");
            entity.set("encryptedValue", e.get("encryptedValue"));
            merge(entity, e);
          }
        }
        case "RAW", "CUSTOM" -> {}
        default -> throw new IllegalStateException("Unsupported standard event " + t);
      }
    }
    output.add(e.deepCopy());
  }

  private String toolOwner(String id) {
    for (ObjectNode m : messages.values())
      for (JsonNode c : m.path("toolCalls")) if (c.path("id").asText().equals(id)) return owner(m);
    return null;
  }

  private static List<String> strings(JsonNode array) {
    List<String> result = new ArrayList<>();
    array.forEach(v -> result.add(v.asText()));
    return result;
  }

  private void reindexCalls() {
    calls.clear();
    answeredCalls.clear();
    for (ObjectNode message : messages.values()) {
      if (message.path("role").asText().equals("reasoning"))
        reasoningOwners.put(text(message, "id"), owner(message));
      for (JsonNode call : message.path("toolCalls"))
        calls.put(text(call, "id"), (ObjectNode) call);
      if (message.path("role").asText().equals("tool"))
        answeredCalls.add(text(message, "toolCallId"));
    }
  }

  private static void merge(ObjectNode entity, JsonNode event) {
    if (!event.has("metadata")) return;
    ObjectNode metadata =
        entity.has("metadata") ? (ObjectNode) entity.get("metadata") : entity.putObject("metadata");
    metadata.setAll((ObjectNode) event.get("metadata"));
  }

  private void reset() {
    opened.clear();
    chunks.clear();
    announced.clear();
    active.clear();
    ended.clear();
    runCalls.clear();
    reasoningOwners.clear();
    reindexCalls();
    subagents.removeAll();
    outcome = null;
    result = null;
    usage = null;
  }

  public synchronized Set<String> pendingToolCallIds() {
    Set<String> ids = new LinkedHashSet<>(runCalls);
    ids.removeAll(answeredCalls);
    return ids;
  }

  public synchronized boolean isTerminal() {
    return terminal;
  }

  public synchronized boolean isFailed() {
    return failed;
  }

  public synchronized void endOfStream() {
    require(terminal, "truncated stream without terminal event");
  }

  public synchronized AgUiRunResult result() {
    return new AgUiRunResult(
        threadId,
        runId,
        messages.values().stream().map(m -> (JsonNode) m.deepCopy()).toList(),
        state.deepCopy(),
        copy(outcome),
        copy(result),
        copy(usage),
        subagents.deepCopy());
  }

  private static JsonNode copy(JsonNode value) {
    return value == null ? null : value.deepCopy();
  }

  /** Closes open local entities before orderly cancellation/interrupt/completion. */
  public synchronized List<JsonNode> closeOpenItems() {
    return closeOpenItems(null);
  }

  public synchronized List<JsonNode> closeOpenItems(JsonNode runOutcome) {
    List<JsonNode> out = new ArrayList<>();
    closeAllChunks(out);
    for (var entry : new ArrayList<>(opened.entrySet())) {
      String kind = entry.getKey().kind(), id = entry.getKey().id();
      ObjectNode e = AgUiSchema.MAPPER.createObjectNode();
      String type =
          switch (kind) {
            case "TEXT" -> "TEXT_MESSAGE_END";
            case "REASONING" -> "REASONING_MESSAGE_END";
            case "TOOL" -> "TOOL_CALL_END";
            case "STEP" -> "STEP_FINISHED";
            default -> "REASONING_END";
          };
      e.put("type", type);
      e.put(
          kind.equals("TOOL") ? "toolCallId" : kind.equals("STEP") ? "stepName" : "messageId", id);
      if (entry.getValue() != null) e.put("subagentRunId", entry.getValue());
      deliver(e, out);
    }
    while (!active.isEmpty()) {
      String leaf =
          active.stream()
              .filter(
                  id ->
                      active.stream()
                          .noneMatch(
                              other ->
                                  subagents.path(other).path("parentSubagentRunId").isTextual()
                                      && subagents
                                          .path(other)
                                          .path("parentSubagentRunId")
                                          .asText()
                                          .equals(id)))
              .findFirst()
              .orElseThrow();
      ObjectNode end =
          AgUiSchema.MAPPER
              .createObjectNode()
              .put("type", "SUBAGENT_FINISHED")
              .put("subagentRunId", leaf);
      if (runOutcome != null && runOutcome.path("type").asText().equals("interrupt")) {
        ArrayNode ownInterrupts = AgUiSchema.MAPPER.createArrayNode();
        boolean suspended = false;
        for (JsonNode interrupt : runOutcome.path("interrupts")) {
          String id = owner(interrupt);
          if (Objects.equals(id, leaf)) ownInterrupts.add(interrupt.get("id"));
          Set<String> visited = new HashSet<>();
          while (id != null && visited.add(id)) {
            if (id.equals(leaf)) suspended = true;
            JsonNode parent = subagents.path(id).get("parentSubagentRunId");
            id = parent == null ? null : parent.asText();
          }
        }
        if (suspended) {
          ObjectNode outcome = AgUiSchema.MAPPER.createObjectNode().put("type", "suspended");
          if (!ownInterrupts.isEmpty()) outcome.set("interruptIds", ownInterrupts);
          end.set("outcome", outcome);
        }
      }
      deliver(end, out);
    }
    return out;
  }
}
