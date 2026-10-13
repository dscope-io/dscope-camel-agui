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
import io.dscope.camel.agui.model.protocol.Interrupt;
import io.dscope.camel.agui.service.*;
import io.dscope.camel.persistence.core.RehydrationPolicy;
import io.dscope.camel.persistence.jdbc.JdbcFlowStateStore;
import io.dscope.camel.persistence.redis.RedisFlowStateStore;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class AgUiContinuationTest {
  @Test
  void attributedInterruptSuspendsSubagentAndAncestors() throws Exception {
    var run =
        new AgUiRunContext(
            AgUiSchema.MAPPER.readTree(AgUiStreamingIntegrationTest.input("r")),
            true,
            event -> {},
            null,
            null);
    run.start();
    run.emit(e("SUBAGENT_STARTED", "subagentRunId", "parent", "name", "parent"));
    run.emit(
        e(
            "SUBAGENT_STARTED",
            "subagentRunId",
            "child",
            "name",
            "child",
            "parentSubagentRunId",
            "parent"));
    Interrupt interrupt = new Interrupt();
    interrupt.setId("approval");
    interrupt.setReason("approval");
    interrupt.setSubagentRunId("child");
    run.interrupt(List.of(interrupt));
    assertEquals(
        "suspended", run.result().subagents().path("child").path("outcome").path("type").asText());
    assertEquals(
        "suspended", run.result().subagents().path("parent").path("outcome").path("type").asText());
    assertEquals(
        "parent", run.result().subagents().path("child").path("parentSubagentRunId").asText());
  }

  @Test
  void jdbcRecoveryPreservesInterruptedThreadAndSeparateWireLog() throws Exception {
    var store =
        new JdbcFlowStateStore(
            "jdbc:derby:memory:continuation"
                + UUID.randomUUID().toString().replace("-", "")
                + ";create=true",
            "",
            "");
    recovery(
        new PersistentAgUiStateStore(store),
        new PersistentAgUiStateStore(store),
        new PersistentAgUiSessionRegistry(
            new JacksonAgUiEventCodec(), store, RehydrationPolicy.DEFAULT));
  }

  @Test
  void redisRecoveryPreservesInterruptedThread() throws Exception {
    String uri = System.getProperty("camel.persistence.test.redis.uri", "redis://localhost:6379");
    try (var pool = new redis.clients.jedis.JedisPool(uri);
        var client = pool.getResource()) {
      client.ping();
    } catch (Exception unavailable) {
      Assumptions.abort("Redis unavailable");
    }
    var store = new RedisFlowStateStore(uri, "agui:continuation:" + UUID.randomUUID());
    recovery(
        new PersistentAgUiStateStore(store),
        new PersistentAgUiStateStore(store),
        new PersistentAgUiSessionRegistry(
            new JacksonAgUiEventCodec(), store, RehydrationPolicy.DEFAULT));
  }

  private void recovery(AgUiStateStore first, AgUiStateStore restored, AgUiSessionRegistry sessions)
      throws Exception {
    JsonNode input = AgUiSchema.MAPPER.readTree(AgUiStreamingIntegrationTest.input("r1"));
    var run = new AgUiRunContext(input, true, event -> {}, sessions.getOrCreate("r1", "t"), first);
    run.start();
    run.emit(e("STATE_SNAPSHOT", "snapshot", Map.of("counter", 2)));
    run.emit(e("TEXT_MESSAGE_CHUNK", "messageId", "m", "delta", "retained"));
    Interrupt interrupt = new Interrupt();
    interrupt.setId("approval");
    interrupt.setReason("approval");
    run.interrupt(List.of(interrupt));
    JsonNode checkpoint = restored.get(AgUiRunContext.checkpointKey("t"));
    assertEquals("interrupt", checkpoint.path("outcome").path("type").asText());
    assertEquals("retained", checkpoint.path("messages").get(0).path("content").asText());
    var resumed =
        AgUiSchema.MAPPER.readTree(
            "{\"threadId\":\"t\",\"runId\":\"r2\",\"messages\":[],\"resume\":[{\"interruptId\":\"approval\",\"status\":\"resolved\",\"payload\":true}]}");
    AgUiResumeSupport.check(resumed, checkpoint, message -> {});
    ((com.fasterxml.jackson.databind.node.ObjectNode) resumed)
        .set("state", checkpoint.get("state"));
    ((com.fasterxml.jackson.databind.node.ObjectNode) resumed)
        .set("messages", checkpoint.get("messages"));
    var continuation =
        new AgUiRunContext(resumed, true, event -> {}, sessions.getOrCreate("r2", "t"), restored);
    continuation.start();
    continuation.finish();
    assertEquals(2, continuation.result().state().path("counter").asInt());
    assertEquals(1, continuation.result().messages().size());
    assertNull(continuation.result().usage());
    assertEquals(
        "success",
        restored.get(AgUiRunContext.checkpointKey("t")).path("outcome").path("type").asText());
    assertEquals("RUN_STARTED", sessions.eventsSince("r1", 0, 100).get(0).eventType());
  }

  @Test
  void expiryCoverageAndAbandonmentAreEnforced() throws Exception {
    var checkpoint =
        AgUiSchema.MAPPER.valueToTree(
            Map.of(
                "runId",
                "old",
                "outcome",
                Map.of(
                    "type",
                    "interrupt",
                    "interrupts",
                    List.of(
                        Map.of(
                            "id",
                            "i",
                            "reason",
                            "approval",
                            "expiresAt",
                            Instant.now().minusSeconds(5).toString())))));
    var input =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            AgUiSchema.MAPPER.readTree(AgUiStreamingIntegrationTest.input("new"));
    assertThrows(
        IllegalArgumentException.class,
        () -> AgUiResumeSupport.check(input, checkpoint, message -> {}));
    input.set(
        "resume",
        AgUiSchema.MAPPER.valueToTree(
            List.of(Map.of("interruptId", "i", "status", "resolved", "payload", true))));
    assertThrows(
        IllegalArgumentException.class,
        () -> AgUiResumeSupport.check(input, checkpoint, message -> {}));
    input.set(
        "resume",
        AgUiSchema.MAPPER.valueToTree(List.of(Map.of("interruptId", "i", "status", "cancelled"))));
    assertDoesNotThrow(() -> AgUiResumeSupport.check(input, checkpoint, message -> {}));
  }

  @Test
  void cancellationClosesEntitiesAndLegacyReplayDoesNotRewriteRecords() throws Exception {
    var run =
        new AgUiRunContext(
            AgUiSchema.MAPPER.readTree(AgUiStreamingIntegrationTest.input("r")),
            true,
            event -> {},
            null,
            null);
    run.start();
    run.emit(e("TEXT_MESSAGE_CHUNK", "messageId", "m", "delta", "partial"));
    run.cancel();
    assertEquals("cancelled", run.result().outcome().path("type").asText());
    List<AgUiSessionEventRecord> records =
        List.of(
            new AgUiSessionEventRecord(
                1,
                "RUN_STARTED",
                "{\"type\":\"RUN_STARTED\",\"runId\":\"r\",\"sessionId\":\"t\",\"timestamp\":\"2025-01-01T00:00:00Z\"}"),
            new AgUiSessionEventRecord(
                2,
                "INTERRUPT_REQUESTED",
                "{\"type\":\"INTERRUPT_REQUESTED\",\"reason\":\"approval\"}"),
            new AgUiSessionEventRecord(
                3,
                "RUN_FINISHED",
                "{\"type\":\"RUN_FINISHED\",\"runId\":\"r\",\"sessionId\":\"t\"}"));
    List<JsonNode> replayed = new ArrayList<>();
    var result = AgUiStoredEventReplay.replay(records, "t", true, replayed::add, message -> {});
    assertEquals("success", result.outcome().path("type").asText());
    assertEquals("CUSTOM", replayed.get(1).path("type").asText());
    assertTrue(records.get(0).json().contains("2025-01-01"));
  }
}
