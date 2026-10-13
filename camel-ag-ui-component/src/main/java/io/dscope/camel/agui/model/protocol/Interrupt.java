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

package io.dscope.camel.agui.model.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Interrupt {
  private String subagentRunId;

  public String getSubagentRunId() {
    return subagentRunId;
  }

  public void setSubagentRunId(String value) {
    this.subagentRunId = value;
  }

  private String id;

  public String getId() {
    return id;
  }

  public void setId(String value) {
    this.id = value;
  }

  private String reason;

  public String getReason() {
    return reason;
  }

  public void setReason(String value) {
    this.reason = value;
  }

  private String message;

  public String getMessage() {
    return message;
  }

  public void setMessage(String value) {
    this.message = value;
  }

  private String toolCallId;

  public String getToolCallId() {
    return toolCallId;
  }

  public void setToolCallId(String value) {
    this.toolCallId = value;
  }

  private Map<String, Object> responseSchema;

  public Map<String, Object> getResponseSchema() {
    return responseSchema;
  }

  public void setResponseSchema(Map<String, Object> value) {
    this.responseSchema = value;
  }

  private String expiresAt;

  public String getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(String value) {
    this.expiresAt = value;
  }

  private Map<String, Object> metadata;

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> value) {
    this.metadata = value;
  }
}
