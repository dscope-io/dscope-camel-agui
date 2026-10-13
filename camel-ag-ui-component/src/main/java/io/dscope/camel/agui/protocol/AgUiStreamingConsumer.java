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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.dscope.camel.agui.AgUiEndpoint;
import io.dscope.camel.agui.processor.AgUiAccessProcessor;
import io.dscope.camel.agui.service.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.camel.*;
import org.apache.camel.component.undertow.*;
import org.apache.camel.support.DefaultExchange;

/** Dedicated HTTP binding registered on Camel's shared Undertow host. */
public final class AgUiStreamingConsumer extends UndertowConsumer {
  private final AgUiEndpoint agui;
  private final AgUiStateStore checkpoints;
  private final AgUiSessionRegistry sessions;
  private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
  private final ConcurrentMap<String, ReentrantLock> locks;

  public AgUiStreamingConsumer(
      UndertowEndpoint transport,
      Processor processor,
      AgUiEndpoint agui,
      AgUiStateStore checkpoints,
      AgUiSessionRegistry sessions,
      ConcurrentMap<String, ReentrantLock> locks) {
    super(transport, processor);
    this.agui = agui;
    this.checkpoints = checkpoints;
    this.sessions = sessions;
    this.locks = locks;
  }

  @Override
  public void handleRequest(HttpServerExchange http) {
    if (!http.getRequestMethod().equalToString("POST")) {
      http.setStatusCode(405);
      http.getResponseHeaders().put(Headers.ALLOW, "POST");
      http.endExchange();
      return;
    }
    if (isSuspended()) {
      http.setStatusCode(503);
      http.endExchange();
      return;
    }
    http.dispatch(workers, () -> serve(http));
  }

  private void serve(HttpServerExchange http) {
    ReentrantLock lock = null;
    AgUiRunContext run = null;
    String lockedThread = null;
    try {
      http.startBlocking();
      Exchange exchange = new DefaultExchange(agui.getCamelContext());
      exchange
          .getIn()
          .setHeader(
              "CamelHttpRemoteAddress", http.getSourceAddress().getAddress().getHostAddress());
      exchange
          .getIn()
          .setHeader("Authorization", http.getRequestHeaders().getFirst(Headers.AUTHORIZATION));
      new AgUiAccessProcessor().process(exchange);
      String contentType = http.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
      if (contentType == null
          || !contentType.toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
        reject(http, 415, "Expected application/json");
        return;
      }
      String accept = http.getRequestHeaders().getFirst(Headers.ACCEPT);
      if (accept != null && !accept.contains("text/event-stream") && !accept.contains("*/*")) {
        reject(http, 406, "Only text/event-stream is supported");
        return;
      }
      boolean validate = AgUiSchema.validationEnabled();
      JsonNode decoded = AgUiSchema.MAPPER.readTree(http.getInputStream());
      if (decoded == null || !decoded.isObject())
        throw new IllegalArgumentException("Expected RunAgentInput object");
      ObjectNode input =
          (ObjectNode)
              AgUiSchema.receive(
                  "RunAgentInput",
                  decoded,
                  validate,
                  message -> org.slf4j.LoggerFactory.getLogger(getClass()).warn(message));
      if (!input.path("threadId").isTextual()
          || !input.path("runId").isTextual()
          || !input.path("messages").isArray())
        throw new IllegalArgumentException("Run input requires threadId, runId and messages");
      String thread = input.get("threadId").asText(), runId = input.get("runId").asText();
      synchronized (locks) {
        lock = locks.computeIfAbsent(thread, unused -> new ReentrantLock());
        if (!lock.tryLock()) {
          lock = null;
          reject(http, 409, "Thread already has an active run");
          return;
        }
        lockedThread = thread;
      }
      if (sessions.get(runId) != null) {
        reject(http, 409, "runId has already been used");
        return;
      }
      JsonNode checkpoint = checkpoints.get(AgUiRunContext.checkpointKey(thread));
      AgUiResumeSupport.check(
          input,
          checkpoint,
          message -> org.slf4j.LoggerFactory.getLogger(getClass()).warn(message));
      if (checkpoint != null && !input.has("state") && checkpoint.has("state"))
        input.set("state", checkpoint.get("state"));
      if (checkpoint != null
          && input.path("messages").isEmpty()
          && checkpoint.has("messages")) input.set("messages", checkpoint.get("messages"));
      var session = sessions.getOrCreate(runId, thread);
      http.setStatusCode(200);
      http.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/event-stream; charset=utf-8");
      http.getResponseHeaders().put(Headers.CACHE_CONTROL, "no-cache");
      run =
          new AgUiRunContext(
              input,
              validate,
              event -> {
                writeEvent(http, event);
                if (event.path("type").asText().equals("RUN_FINISHED")
                    || event.path("type").asText().equals("RUN_ERROR")) http.endExchange();
              },
              session,
              checkpoints,
              ((io.dscope.camel.agui.AgUiComponent) agui.getComponent()).getEventMiddleware());
      exchange.getIn().setBody(input);
      exchange.setProperty(AgUiRunContext.EXCHANGE_PROPERTY, run);
      run.start();
      getProcessor().process(exchange);
      if (exchange.getException() != null) run.fail(exchange.getException());
      else if (!run.isTerminal()) run.finish();
    } catch (io.dscope.camel.agui.processor.AgUiAuthorizationException denied) {
      if (run == null) reject(http, 401, "Access denied");
    } catch (Exception error) {
      if (run == null)
        reject(
            http,
            400,
            error instanceof IllegalArgumentException ? error.getMessage() : "Unable to start run");
      else
        try {
          run.fail(error);
        } catch (Exception disconnected) {
          org.slf4j.LoggerFactory.getLogger(getClass())
              .debug("AG-UI stream disconnected", disconnected);
        }
    } finally {
      if (lock != null)
        synchronized (locks) {
          lock.unlock();
          locks.remove(lockedThread, lock);
        }
      try {
        http.getOutputStream().close();
      } catch (Exception ignored) {
      }
      http.endExchange();
    }
  }

  private static void writeEvent(HttpServerExchange http, JsonNode event) {
    CountDownLatch completed = new CountDownLatch(1);
    java.util.concurrent.atomic.AtomicReference<IOException> failure =
        new java.util.concurrent.atomic.AtomicReference<>();
    http.getResponseSender()
        .send(
            java.nio.ByteBuffer.wrap(("data: " + event + "\n\n").getBytes(StandardCharsets.UTF_8)),
            new io.undertow.io.IoCallback() {
              public void onComplete(HttpServerExchange exchange, io.undertow.io.Sender sender) {
                completed.countDown();
              }

              public void onException(
                  HttpServerExchange exchange, io.undertow.io.Sender sender, IOException error) {
                failure.set(error);
                completed.countDown();
              }
            });
    try {
      completed.await();
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("AG-UI delivery cancelled", error);
    }
    if (failure.get() != null) throw new UncheckedIOException(failure.get());
    // With blocking request parsing Undertow's sender completes on the virtual worker.
    // Flush at every completed write, never wait for the route or replay buffer.
    try {
      http.getOutputStream().flush();
    } catch (IOException error) {
      throw new UncheckedIOException(error);
    }
  }

  private static void reject(HttpServerExchange http, int code, String message) {
    try {
      http.setStatusCode(code);
      http.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
      http.getOutputStream()
          .write(
              AgUiSchema.MAPPER
                  .createObjectNode()
                  .put("error", message)
                  .toString()
                  .getBytes(StandardCharsets.UTF_8));
    } catch (IOException ignored) {
    }
  }

  @Override
  protected void doStop() throws Exception {
    super.doStop();
    workers.shutdownNow();
    workers.awaitTermination(5, TimeUnit.SECONDS);
  }
}
