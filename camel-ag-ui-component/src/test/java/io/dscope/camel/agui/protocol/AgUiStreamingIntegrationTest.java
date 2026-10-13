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

import com.fasterxml.jackson.databind.JsonNode;
import io.dscope.camel.agui.AgUiComponent;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.Test;

class AgUiStreamingIntegrationTest {
  @Test
  void producerRejectsHttp200WithTruncatedRunWithoutRetry() throws Exception {
    int port = port();
    var requests = new java.util.concurrent.atomic.AtomicInteger();
    var server =
        com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
    server.createContext(
        "/agui/agent",
        request -> {
          requests.incrementAndGet();
          request.getRequestBody().readAllBytes();
          byte[] body =
              ("data: {\"type\":\"RUN_STARTED\",\"runId\":\"r\",\"threadId\":\"t\"}\n\n")
                  .getBytes();
          request.getResponseHeaders().add("Content-Type", "text/event-stream");
          request.sendResponseHeaders(200, body.length);
          request.getResponseBody().write(body);
          request.close();
        });
    server.start();
    try (var camel = new DefaultCamelContext()) {
      camel.addComponent("agui", new AgUiComponent());
      camel.start();
      try (var template = camel.createProducerTemplate()) {
        assertThrows(
            CamelExecutionException.class,
            () ->
                template.requestBody(
                    "agui:client?transportMode=httpSse&remoteUrl=http://127.0.0.1:" + port,
                    input("r")));
        assertEquals(1, requests.get());
      }
    } finally {
      server.stop(0);
    }
  }

  @Test
  void disconnectReleasesThreadAndWorkers() throws Exception {
    int port = port();
    var completed = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    try (var camel = new DefaultCamelContext()) {
      camel.addComponent("agui", new AgUiComponent());
      camel.addRoutes(
          new RouteBuilder() {
            public void configure() {
              errorHandler(noErrorHandler());
              from("agui:server?transportMode=httpSse&serverUrl=undertow:http://127.0.0.1:" + port)
                  .process(
                      exchange -> {
                        var run =
                            exchange.getProperty(
                                AgUiRunContext.EXCHANGE_PROPERTY, AgUiRunContext.class);
                        if (exchange
                            .getIn()
                            .getBody(JsonNode.class)
                            .path("runId")
                            .asText()
                            .equals("r")) {
                          try {
                            release.await(5, TimeUnit.SECONDS);
                            for (int i = 0; i < 10000; i++)
                              run.emit(
                                  e(
                                      "TEXT_MESSAGE_CHUNK",
                                      "messageId",
                                      "m",
                                      "delta",
                                      "x".repeat(1024)));
                          } finally {
                            completed.countDown();
                          }
                        }
                      });
            }
          });
      camel.start();
      var http = HttpClient.newHttpClient();
      var response =
          http.send(
              HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/agui/agent"))
                  .header("Content-Type", "application/json")
                  .POST(HttpRequest.BodyPublishers.ofString(input("r")))
                  .build(),
              HttpResponse.BodyHandlers.ofInputStream());
      response.body().close();
      release.countDown();
      assertTrue(completed.await(10, TimeUnit.SECONDS));
      HttpResponse<String> next = null;
      for (int attempt = 0; attempt < 100; attempt++) {
        next =
            http.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/agui/agent"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(input("new")))
                    .build(),
                HttpResponse.BodyHandlers.ofString());
        if (next.statusCode() != 409) break;
        Thread.sleep(25); // Wait for the disconnected request's finally block.
      }
      assertNotNull(next);
      assertEquals(200, next.statusCode());
      assertTrue(next.body().contains("RUN_FINISHED"));
    } finally {
      release.countDown();
    }
  }

  static int port() throws Exception {
    try (var server = new java.net.ServerSocket(0)) {
      return server.getLocalPort();
    }
  }

  static String input(String runId) {
    return "{\"threadId\":\"t\",\"runId\":\"" + runId + "\",\"messages\":[]}";
  }

  @Test
  void producerReceivesEventsBeforeCompletionAndBeyondReplayLimit() throws Exception {
    int port = port();
    CountDownLatch firstEvent = new CountDownLatch(1), release = new CountDownLatch(1);
    List<JsonNode> received = new CopyOnWriteArrayList<>();
    try (var camel = new DefaultCamelContext()) {
      camel.addComponent("agui", new AgUiComponent());
      camel.addRoutes(
          new RouteBuilder() {
            public void configure() {
              from("agui:server?transportMode=httpSse&serverUrl=undertow:http://127.0.0.1:" + port)
                  .process(
                      exchange -> {
                        var run =
                            exchange.getProperty(
                                AgUiRunContext.EXCHANGE_PROPERTY, AgUiRunContext.class);
                        run.emit(e("TEXT_MESSAGE_CHUNK", "messageId", "answer", "delta", "first"));
                        assertTrue(release.await(10, TimeUnit.SECONDS));
                        for (int i = 0; i < 1200; i++)
                          run.emit(e("TEXT_MESSAGE_CHUNK", "delta", "x"));
                      });
              from("direct:events")
                  .process(
                      exchange -> {
                        received.add(exchange.getIn().getBody(JsonNode.class));
                        firstEvent.countDown();
                      });
            }
          });
      camel.start();
      try (var template = camel.createProducerTemplate();
          var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        var result =
            executor.submit(
                () ->
                    template.requestBody(
                        "agui:client?transportMode=httpSse&remoteUrl=http://127.0.0.1:"
                            + port
                            + "&eventsEndpoint=direct:events",
                        input("r"),
                        AgUiRunResult.class));
        assertTrue(firstEvent.await(10, TimeUnit.SECONDS));
        assertFalse(result.isDone());
        release.countDown();
        AgUiRunResult state = result.get(20, TimeUnit.SECONDS);
        assertEquals(1205, received.size());
        assertEquals(1205, state.messages().get(0).path("content").asText().length());
        assertEquals("success", state.outcome().path("type").asText());
      } finally {
        release.countDown();
      }
    }
  }

  @Test
  void rejectionIsHttpErrorAndAgentFailureIsAnEvent() throws Exception {
    int port = port();
    try (var camel = new DefaultCamelContext()) {
      camel.addComponent("agui", new AgUiComponent());
      camel.addRoutes(
          new RouteBuilder() {
            public void configure() {
              from("agui:server?transportMode=httpSse&serverUrl=undertow:http://127.0.0.1:" + port)
                  .process(
                      exchange -> {
                        throw new IllegalStateException("agent failed");
                      });
            }
          });
      camel.start();
      var http = HttpClient.newHttpClient();
      var invalid =
          http.send(
              HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/agui/agent"))
                  .header("Content-Type", "application/json")
                  .POST(HttpRequest.BodyPublishers.ofString("{}"))
                  .build(),
              HttpResponse.BodyHandlers.ofString());
      assertEquals(400, invalid.statusCode());
      assertFalse(invalid.body().contains("RUN_STARTED"));
      try (var template = camel.createProducerTemplate()) {
        assertThrows(
            CamelExecutionException.class,
            () ->
                template.requestBody(
                    "agui:client?transportMode=httpSse&remoteUrl=http://127.0.0.1:" + port,
                    input("r")));
      }
    }
  }

  @Test
  void sseFramingIgnoresOtherFieldsAndSupportsMultilineUtf8() throws Exception {
    String frames =
        ": keep alive\r\n"
            + "id: 1\r\n"
            + "event: ignored\r\n"
            + "data: {\"type\":\"CUSTOM\",\r\n"
            + "data: \"name\":\"é\",\"value\":1}\r\n\r\n";
    List<JsonNode> events = new ArrayList<>();
    AgUiSseReader.read(
        new java.io.ByteArrayInputStream(frames.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
        events::add);
    assertEquals("é", events.get(0).path("name").asText());
    assertThrows(
        java.io.EOFException.class,
        () ->
            AgUiSseReader.read(
                new java.io.ByteArrayInputStream("data: {}".getBytes()), events::add));
  }
}
