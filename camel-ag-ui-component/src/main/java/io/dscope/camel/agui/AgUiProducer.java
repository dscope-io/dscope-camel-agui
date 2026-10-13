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
import io.dscope.camel.agui.config.AgUiProtocolDefaults;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultProducer;

public class AgUiProducer extends DefaultProducer {

  private final AgUiEndpoint endpoint;
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  private ProducerTemplate producerTemplate;
  private final java.net.http.HttpClient httpClient =
      java.net.http.HttpClient.newBuilder()
          .connectTimeout(java.time.Duration.ofSeconds(30))
          .build();
  private io.dscope.camel.agui.service.AgUiStateStore checkpoints;

  public AgUiProducer(AgUiEndpoint endpoint) {
    super(endpoint);
    this.endpoint = endpoint;
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    if ("httpSse".equals(endpoint.getConfiguration().getTransportMode())) {
      processHttpSse(exchange);
      return;
    }
    Map<String, Object> params = exchange.getIn().getBody(Map.class);
    if (params == null) {
      params = Map.of();
    }

    Map<String, Object> envelope = new LinkedHashMap<>();
    envelope.put("jsonrpc", AgUiProtocolDefaults.JSONRPC_VERSION);
    envelope.put("id", UUID.randomUUID().toString());
    envelope.put("method", endpoint.getConfiguration().getMethod());
    envelope.put("params", params);

    String target =
        buildHttpUri(
            endpoint.getConfiguration().getRemoteUrl(), endpoint.getConfiguration().getRpcPath());
    String json = mapper.writeValueAsString(envelope);
    Object response =
        producerTemplate.requestBodyAndHeader(
            target, json, Exchange.CONTENT_TYPE, "application/json");
    exchange.getMessage().setBody(response);
    exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "application/json");
  }

  private void processHttpSse(Exchange exchange) throws Exception {
    var schema = io.dscope.camel.agui.protocol.AgUiSchema.MAPPER;
    boolean validate = io.dscope.camel.agui.protocol.AgUiSchema.validationEnabled();
    Object body = exchange.getIn().getBody();
    var raw = body instanceof String text ? schema.readTree(text) : schema.valueToTree(body);
    var input =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            io.dscope.camel.agui.protocol.AgUiSchema.receive(
                "RunAgentInput",
                raw,
                validate,
                message -> org.slf4j.LoggerFactory.getLogger(AgUiProducer.class).warn(message));
    input.put("protocolVersion", "1.0");
    String thread = input.path("threadId").asText();
    String checkpointKey =
        "agui.client."
            + endpoint.getConfiguration().getRemoteUrl()
            + endpoint.getConfiguration().getAgentPath()
            + "."
            + thread;
    var checkpoint = checkpoints.get(checkpointKey);
    io.dscope.camel.agui.protocol.AgUiResumeSupport.check(
        input,
        checkpoint,
        message -> org.slf4j.LoggerFactory.getLogger(AgUiProducer.class).warn(message));
    if (checkpoint != null && !input.has("state") && checkpoint.has("state"))
      input.set("state", checkpoint.get("state"));
    if (checkpoint != null
        && input.path("messages").isEmpty()
        && checkpoint.has("messages")) input.set("messages", checkpoint.get("messages"));
    var pipeline =
        new io.dscope.camel.agui.protocol.AgUiEventPipeline(
            validate,
            message -> org.slf4j.LoggerFactory.getLogger(AgUiProducer.class).warn(message),
            ((AgUiComponent) endpoint.getComponent()).getEventMiddleware());
    pipeline.seed(input);
    var requestedResult =
        new java.util.concurrent.atomic.AtomicReference<
            io.dscope.camel.agui.protocol.AgUiRunResult>();
    var request =
        java.net.http.HttpRequest.newBuilder(
                java.net.URI.create(
                    buildHttpUri(
                        endpoint.getConfiguration().getRemoteUrl(),
                        endpoint.getConfiguration().getAgentPath())))
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(input.toString()));
    String authorization = exchange.getIn().getHeader("Authorization", String.class);
    if (authorization != null) request.header("Authorization", authorization);
    var response =
        httpClient.send(request.build(), java.net.http.HttpResponse.BodyHandlers.ofInputStream());
    try (var stream = response.body()) {
      if (response.statusCode() != 200)
        throw new IllegalStateException("AG-UI HTTP status " + response.statusCode());
      if (!response
          .headers()
          .firstValue("Content-Type")
          .orElse("")
          .toLowerCase(java.util.Locale.ROOT)
          .startsWith("text/event-stream"))
        throw new IllegalStateException("AG-UI server did not return SSE");
      io.dscope.camel.agui.protocol.AgUiSseReader.read(
          stream,
          event -> {
            for (var normalized : pipeline.accept(event)) {
              if (endpoint.getConfiguration().getEventsEndpoint() != null) {
                var delivered =
                    producerTemplate.send(
                        endpoint.getConfiguration().getEventsEndpoint(),
                        org.apache.camel.ExchangePattern.InOnly,
                        outgoing -> {
                          outgoing.getIn().setBody(normalized);
                          outgoing.getIn().setHeader("agui.runId", pipeline.result().runId());
                        });
                if (delivered.getException() != null)
                  throw new IllegalStateException(
                      "AG-UI event route failed", delivered.getException());
              }
            }
            if (pipeline.isTerminal()
                && input.path("runId").asText().equals(pipeline.result().runId()))
              requestedResult.set(pipeline.result());
          });
      pipeline.endOfStream();
      var completed = requestedResult.get();
      if (completed == null)
        throw new IllegalStateException("Requested AG-UI run missing from response");
      checkpoints.put(checkpointKey, schema.valueToTree(completed));
      if (completed.outcome().path("type").asText().equals("RUN_ERROR"))
        throw new IllegalStateException(
            "AG-UI run failed: " + completed.outcome().path("message").asText());
      exchange.getMessage().setBody(completed);
      exchange.getMessage().removeHeader(Exchange.CONTENT_TYPE);
    } finally {
      exchange.setProperty(
          "agui.runResult",
          requestedResult.get() == null ? pipeline.result() : requestedResult.get());
    }
  }

  @Override
  protected void doStart() throws Exception {
    super.doStart();
    checkpoints =
        endpoint
            .getCamelContext()
            .getRegistry()
            .lookupByNameAndType(
                "agUiStateStore", io.dscope.camel.agui.service.AgUiStateStore.class);
    if (checkpoints == null)
      checkpoints = ((AgUiComponent) endpoint.getComponent()).standardCheckpoints;
    producerTemplate = getEndpoint().getCamelContext().createProducerTemplate();
    producerTemplate.start();
  }

  @Override
  protected void doStop() throws Exception {
    if (producerTemplate != null) {
      producerTemplate.stop();
      producerTemplate = null;
    }
    httpClient.shutdownNow();
    super.doStop();
  }

  private String buildHttpUri(String base, String path) {
    String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    String normalizedPath = path.startsWith("/") ? path : "/" + path;
    if (normalizedBase.startsWith("http")) {
      return normalizedBase + normalizedPath;
    }
    if (normalizedBase.startsWith("undertow:")) {
      return normalizedBase.substring("undertow:".length()) + normalizedPath;
    }
    return "http://" + normalizedBase + normalizedPath;
  }
}
