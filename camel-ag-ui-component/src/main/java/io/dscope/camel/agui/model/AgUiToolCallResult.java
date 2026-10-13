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

public class AgUiToolCallResult extends AgUiEvent {
  public AgUiToolCallResult() {
    super("TOOL_CALL_RESULT", null, null);
  }

  private String messageId;
  private String toolCallId;
  private String content;
  private String role;
  private java.util.List<io.dscope.camel.agui.model.protocol.ContentPart> contentParts;

  public AgUiToolCallResult(String runId, String sessionId, Object result) {
    super(AgUiEventTypes.TOOL_CALL_RESULT, runId, sessionId);
    try {
      this.content =
          result == null
              ? ""
              : result instanceof String text
                  ? text
                  : new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(result);
    } catch (Exception error) {
      throw new IllegalArgumentException("Unable to encode tool result", error);
    }
  }

  public AgUiToolCallResult(
      String runId,
      String sessionId,
      String messageId,
      String toolCallId,
      String content,
      String role) {
    super(AgUiEventTypes.TOOL_CALL_RESULT, runId, sessionId);
    this.messageId = messageId;
    this.toolCallId = toolCallId;
    this.content = content;
    this.contentParts = null;
    this.role = role;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public String getToolCallId() {
    return toolCallId;
  }

  public void setToolCallId(String toolCallId) {
    this.toolCallId = toolCallId;
  }

  @com.fasterxml.jackson.annotation.JsonIgnore
  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
    this.contentParts = null;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  @com.fasterxml.jackson.annotation.JsonIgnore
  public java.util.List<io.dscope.camel.agui.model.protocol.ContentPart> getContentParts() {
    return contentParts;
  }

  public void setContentParts(
      java.util.List<io.dscope.camel.agui.model.protocol.ContentPart> value) {
    contentParts = value;
    content = null;
  }

  @com.fasterxml.jackson.annotation.JsonProperty("content")
  public Object getWireContent() {
    return contentParts == null ? content : contentParts;
  }

  @com.fasterxml.jackson.annotation.JsonProperty("content")
  public void setWireContent(Object value) {
    if (value instanceof String text) {
      setContent(text);
      return;
    }
    if (value instanceof java.util.List<?> parts) {
      setContentParts(
          io.dscope.camel.agui.protocol.AgUiSchema.MAPPER.convertValue(
              parts,
              new com.fasterxml.jackson.core.type.TypeReference<
                  java.util.List<io.dscope.camel.agui.model.protocol.ContentPart>>() {}));
      return;
    }
    throw new IllegalArgumentException("Tool content must be text or ordered content parts");
  }
}
