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
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Consumer;

/** Shared client/producer checks for continuation of a known interrupted thread. */
public final class AgUiResumeSupport {
  public static void check(JsonNode input, JsonNode checkpoint, Consumer<String> warning) {
    if (checkpoint == null
        || !checkpoint.path("outcome").path("type").asText().equals("interrupt")) {
      if (input.has("resume"))
        warning.accept("Resume entries do not reference a known interrupted run");
      return;
    }
    if (input.path("runId").asText().equals(checkpoint.path("runId").asText()))
      throw new IllegalArgumentException("Resume requires a new runId");
    Set<String> answered = new HashSet<>();
    Map<String, JsonNode> pending = new HashMap<>();
    checkpoint
        .path("outcome")
        .path("interrupts")
        .forEach(i -> pending.put(i.path("id").asText(), i));
    for (JsonNode entry : input.path("resume")) {
      String id = entry.path("interruptId").asText();
      if (!answered.add(id)) throw new IllegalArgumentException("Duplicate resume entry " + id);
      JsonNode interrupt = pending.get(id);
      if (interrupt == null) {
        warning.accept("Unrecognised interrupt answer " + id);
        continue;
      }
      if (entry.path("status").asText().equals("resolved") && interrupt.has("expiresAt")) {
        try {
          if (!Instant.parse(interrupt.get("expiresAt").asText()).isAfter(Instant.now()))
            throw new IllegalArgumentException("Expired interrupt " + id);
        } catch (DateTimeParseException ignored) {
          warning.accept("Unparseable interrupt expiry " + id);
        }
      }
    }
    if (!answered.containsAll(pending.keySet()))
      throw new IllegalArgumentException("Resume must answer or abandon every pending interrupt");
  }

  private AgUiResumeSupport() {}
}
