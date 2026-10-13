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
import io.dscope.camel.agui.model.*;
import io.dscope.camel.agui.model.protocol.Interrupt;
import io.dscope.camel.agui.service.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * Agent-facing ordered event sink. Completion is explicit or supplied by the Camel route lifecycle.
 */
public final class AgUiRunContext {
  public static final String EXCHANGE_PROPERTY = "agui.runContext";
  private final JsonNode input;
  private final AgUiEventPipeline pipeline;
  private final Consumer<JsonNode> sink;
  private final AgUiSession session;
  private final AgUiStateStore checkpoints;
  private final boolean validation;

  public AgUiRunContext(
      JsonNode input,
      boolean validation,
      Consumer<JsonNode> sink,
      AgUiSession session,
      AgUiStateStore checkpoints) {
    this(input, validation, sink, session, checkpoints, List.of());
  }

  public AgUiRunContext(
      JsonNode input,
      boolean validation,
      Consumer<JsonNode> sink,
      AgUiSession session,
      AgUiStateStore checkpoints,
      List<java.util.function.UnaryOperator<JsonNode>> middleware) {
    this.input = input.deepCopy();
    this.validation = validation;
    this.sink = sink;
    this.session = session;
    this.checkpoints = checkpoints;
    pipeline =
        new AgUiEventPipeline(
            validation,
            message -> org.slf4j.LoggerFactory.getLogger(AgUiRunContext.class).warn(message),
            middleware);
    pipeline.seed(input);
  }

  public synchronized void start() {
    ObjectNode event =
        AgUiSchema.MAPPER
            .createObjectNode()
            .put("type", "RUN_STARTED")
            .put("threadId", input.path("threadId").asText())
            .put("runId", input.path("runId").asText())
            .put("protocolVersion", "1.0");
    if (input.has("parentRunId")) event.set("parentRunId", input.get("parentRunId"));
    event.set("input", input);
    emit(event);
  }

  public synchronized void emit(AgUiEvent event) {
    emit(
        event instanceof AgUiJsonEvent json
            ? json.getPayload()
            : AgUiSchema.MAPPER.valueToTree(event));
  }

  public synchronized void emit(JsonNode event) {
    for (JsonNode normalized : pipeline.emit(event)) write(normalized);
  }

  private void write(JsonNode event) {
    if (session != null) session.emit(new AgUiJsonEvent(event));
    if (checkpoints != null
        && (pipeline.isTerminal() || event.path("type").asText().equals("RUN_STARTED"))) {
      ObjectNode checkpoint = AgUiSchema.MAPPER.valueToTree(pipeline.result());
      checkpoint.put(
          "status",
          pipeline.isFailed()
              ? "failed"
              : pipeline.isTerminal()
                  ? pipeline.result().outcome().path("type").asText()
                  : "running");
      if(input.has("resume"))checkpoint.set("resume",input.get("resume"));
      if(pipeline.result().outcome()!=null && pipeline.result().outcome().path("type").asText().equals("interrupt"))
        checkpoint.set("pendingInterrupts",pipeline.result().outcome().get("interrupts"));
      checkpoints.put(checkpointKey(input.path("threadId").asText()), checkpoint);
    }
    if (pipeline.isTerminal() && session != null) session.complete();
    sink.accept(event);
  }

  public static String checkpointKey(String threadId) {
    return "agui.thread." + threadId;
  }

  public synchronized boolean isTerminal() {
    return pipeline.isTerminal();
  }

  public synchronized AgUiRunResult result() {
    return pipeline.result();
  }

  public synchronized void finish() {
    finish(null, null);
  }

  public synchronized void finish(
      Object result, List<io.dscope.camel.agui.model.protocol.TokenUsage> usage) {
    ObjectNode outcome = AgUiSchema.MAPPER.createObjectNode().put("type", "success");
    if (!pipeline.pendingToolCallIds().isEmpty())
      outcome.set(
          "pendingToolCallIds", AgUiSchema.MAPPER.valueToTree(pipeline.pendingToolCallIds()));
    terminate(outcome, result, usage);
  }

  public synchronized void interrupt(List<Interrupt> interrupts) {
    ObjectNode outcome = AgUiSchema.MAPPER.createObjectNode().put("type", "interrupt");
    outcome.set("interrupts", AgUiSchema.MAPPER.valueToTree(interrupts));
    terminate(outcome, null, null);
  }

  public synchronized void cancel() {
    terminate(AgUiSchema.MAPPER.createObjectNode().put("type", "cancelled"), null, null);
  }

  private void terminate(JsonNode outcome, Object result, Object usage) {
    if (isTerminal()) throw new IllegalStateException("Run is already terminal");
    for (JsonNode event : pipeline.closeOpenItems(outcome)) write(event);
    ObjectNode end =
        AgUiSchema.MAPPER
            .createObjectNode()
            .put("type", "RUN_FINISHED")
            .put("threadId", input.path("threadId").asText())
            .put("runId", input.path("runId").asText());
    end.set("outcome", outcome);
    if (result != null) end.set("result", AgUiSchema.MAPPER.valueToTree(result));
    if (usage != null) end.set("usage", AgUiSchema.MAPPER.valueToTree(usage));
    emit(end);
  }

  public synchronized void fail(Throwable error) {
    if (pipeline.isFailed()) return;
    ObjectNode event =
        AgUiSchema.MAPPER
            .createObjectNode()
            .put("type", "RUN_ERROR")
            .put(
                "message",
                error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
    emit(event);
  }
}
