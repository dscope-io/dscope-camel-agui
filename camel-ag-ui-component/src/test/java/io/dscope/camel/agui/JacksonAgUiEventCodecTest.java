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
import io.dscope.camel.agui.model.*;
import io.dscope.camel.agui.service.JacksonAgUiEventCodec;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JacksonAgUiEventCodecTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void allBuiltInEventsValidateAgainstBundledSchema() {
        var codec = new JacksonAgUiEventCodec(true);
        List<AgUiEvent> events = List.of(
            new AgUiRunStarted("r", "t"), new AgUiRunFinished("r", "t"), new AgUiRunError("r", "t", "failed"),
            new AgUiStepStarted("r", "t", "step"), new AgUiStepFinished("r", "t", "step"),
            new AgUiTextMessageStart("r", "t", "m", "assistant"), new AgUiTextMessageContent("r", "t", "m", "hello"),
            new AgUiTextMessageEnd("r", "t", "m"), new AgUiToolCallStart("r", "t", "call", "search"),
            new AgUiToolCallArgs("r", "t", "call", "{}"), new AgUiToolCallEnd("r", "t", "call", "search"),
            new AgUiToolCallResult("r", "t", "result", "call", "ok", "tool"),
            new AgUiStateSnapshot("r", "t", Map.of("count", 1)),
            new AgUiStateDelta("r", "t", List.of(Map.of("op", "replace", "path", "/count", "value", 2))),
            new AgUiInterruptRequested("r", "t", "approval"), new AgUiResumeAccepted("r", "t"));
        for (AgUiEvent event : events) {
            assertDoesNotThrow(() -> codec.toJson(event), event.getClass().getSimpleName());
        }
    }

    @Test
    void serializesMillisecondsAndOnlyProtocolIdentifiers() throws Exception {
        var codec = new JacksonAgUiEventCodec(true);
        var event = new AgUiTextMessageContent("r", "t", "m", "hello");
        event.setTimestamp(Instant.parse("2026-09-30T12:00:00.123456Z"));
        event.with("model", "example");
        var json = mapper.readTree(codec.toJson(event));
        assertTrue(json.get("timestamp").isIntegralNumber());
        assertEquals(event.getTimestamp().toEpochMilli(), json.get("timestamp").longValue());
        assertFalse(json.has("runId"));
        assertFalse(json.has("sessionId"));
        assertFalse(json.has("threadId"));
        assertEquals("example", json.path("metadata").path("model").asText());
        var start = new AgUiRunStarted("r", "t");
        start.setThreadId("other-thread");
        var lifecycle = mapper.readTree(codec.toJson(start));
        assertEquals("r", lifecycle.path("runId").asText());
        assertEquals("other-thread", lifecycle.path("threadId").asText());
        assertFalse(lifecycle.has("sessionId"));
    }

    @Test
    void rejectsMissingFieldsInvalidRolesPatchesAndUnknownFields() {
        var codec = new JacksonAgUiEventCodec(true);
        assertThrows(IllegalArgumentException.class, () -> codec.toJson(new AgUiTextMessageContent("r", "t", "missing-id")));
        assertThrows(IllegalArgumentException.class, () -> codec.toJson(new AgUiTextMessageStart("r", "t", "m", "invalid-role")));
        assertThrows(IllegalArgumentException.class, () -> codec.toJson(new AgUiStateDelta("r", "t", List.of(Map.of("op", "remove")))));
        assertThrows(IllegalArgumentException.class, () -> codec.toJson(new ExtraFieldEvent()));
        assertThrows(IllegalArgumentException.class, () -> codec.toJson(new AgUiEvent("UNKNOWN", "r", "t")));
        assertThrows(IllegalArgumentException.class, () -> codec.toJson(null));
    }

    @Test
    void validationCanBeDisabledWithoutChangingWireFormat() throws Exception {
        var event = new AgUiTextMessageContent("r", "t", "missing-id");
        var json = mapper.readTree(new JacksonAgUiEventCodec(false).toJson(event));
        assertEquals("missing-id", json.path("delta").asText());
        assertFalse(json.has("sessionId"));
        assertTrue(json.path("timestamp").isIntegralNumber());
    }

    @Test
    void systemPropertyDefaultsToTrueAndAllowsOptOut() {
        String key = JacksonAgUiEventCodec.VALIDATION_ENABLED_PROPERTY;
        String previous = System.getProperty(key);
        try {
            System.clearProperty(key);
            assertThrows(IllegalArgumentException.class, () -> new JacksonAgUiEventCodec().toJson(new ExtraFieldEvent()));
            System.setProperty(key, "false");
            assertDoesNotThrow(() -> new JacksonAgUiEventCodec().toJson(new ExtraFieldEvent()));
            System.setProperty(key, "true");
            assertThrows(IllegalArgumentException.class, () -> new JacksonAgUiEventCodec().toJson(new ExtraFieldEvent()));
            System.setProperty(key, "typo");
            assertThrows(IllegalArgumentException.class, JacksonAgUiEventCodec::new);
        } finally {
            if (previous == null) System.clearProperty(key); else System.setProperty(key, previous);
        }
    }

    public static class ExtraFieldEvent extends AgUiRunFinished {
        public ExtraFieldEvent() { super("r", "t"); }
        public String getUnexpected() { return "extra"; }
    }
}
