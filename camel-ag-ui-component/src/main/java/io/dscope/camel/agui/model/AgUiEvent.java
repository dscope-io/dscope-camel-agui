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

package io.dscope.camel.agui.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgUiEvent {

  private String type;
  private Instant timestamp;
  private String runId;
  private String sessionId;
  private String threadId;
  private Map<String, Object> metadata;
  private Object rawEvent;
  private String subagentRunId;

  public AgUiEvent() {
    this.timestamp = Instant.now();
  }

  public AgUiEvent(String type, String runId, String sessionId) {
    this.type = type;
    this.runId = runId;
    this.sessionId = sessionId;
    this.threadId = sessionId;
    this.timestamp = Instant.now();
  }

  public AgUiEvent with(String key, Object value) {
    if (metadata == null) {
      metadata = new LinkedHashMap<>();
    }
    metadata.put(key, value);
    return this;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @JsonSerialize(using = EpochMillisSerializer.class)
  @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = EpochMillisDeserializer.class)
  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  @JsonIgnore
  public String getRunId() {
    return runId;
  }

  public void setRunId(String runId) {
    this.runId = runId;
  }

  @JsonIgnore
  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  @JsonIgnore
  public String getThreadId() {
    return threadId;
  }

  public void setThreadId(String threadId) {
    this.threadId = threadId;
  }

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  public Object getRawEvent() {
    return rawEvent;
  }

  public void setRawEvent(Object value) {
    rawEvent = value;
  }

  public String getSubagentRunId() {
    return subagentRunId;
  }

  public void setSubagentRunId(String value) {
    subagentRunId = value;
  }

  public static class EpochMillisSerializer extends JsonSerializer<Instant> {
    @Override
    public void serialize(Instant value, JsonGenerator generator, SerializerProvider provider)
        throws IOException {
      generator.writeNumber(value.toEpochMilli());
    }
  }

  public static class EpochMillisDeserializer
      extends com.fasterxml.jackson.databind.JsonDeserializer<Instant> {
    @Override
    public Instant deserialize(
        com.fasterxml.jackson.core.JsonParser parser,
        com.fasterxml.jackson.databind.DeserializationContext context)
        throws IOException {
      if (!parser.currentToken().isNumeric())
        throw new IOException("AG-UI timestamp must be integer epoch milliseconds");
      return Instant.ofEpochMilli(parser.getLongValue());
    }
  }
}
