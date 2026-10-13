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
import io.dscope.camel.agui.service.AgUiSessionEventRecord;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

/** Translates stored legacy wire envelopes without changing the persisted records. */
public final class AgUiStoredEventReplay {
  public static AgUiRunResult replay(
      List<AgUiSessionEventRecord> records,
      String threadId,
      boolean validation,
      Consumer<JsonNode> delivery,
      Consumer<String> warnings) {
    var pipeline = new AgUiEventPipeline(validation, warnings);
    for (var record : records) {
      try {
        ObjectNode event = (ObjectNode) AgUiSchema.MAPPER.readTree(record.json());
        if (event.path("timestamp").isTextual())
          event.put("timestamp", Instant.parse(event.get("timestamp").asText()).toEpochMilli());
        String type = event.path("type").asText();
        if (type.equals("RUN_STARTED") || type.equals("RUN_FINISHED")) {
          if (!event.has("threadId")) event.put("threadId", threadId);
        }
        if (type.equals("INTERRUPT_REQUESTED") || type.equals("RESUME_ACCEPTED")) {
          ObjectNode custom =
              AgUiSchema.MAPPER.createObjectNode().put("type", "CUSTOM").put("name", type);
          custom.set("value", event);
          event = custom;
        }
        if (type.equals("TOOL_CALL_RESULT")
            && event.has("content")
            && event.get("content").isObject())
          event.put("content", event.get("content").toString());
        for (JsonNode normalized : pipeline.accept(event)) delivery.accept(normalized);
      } catch (Exception error) {
        throw new IllegalArgumentException(
            "Unable to replay AG-UI record " + record.sequence(), error);
      }
    }
    pipeline.endOfStream();
    return pipeline.result();
  }

  private AgUiStoredEventReplay() {}
}
