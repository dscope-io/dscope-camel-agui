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
public class ToolMessage implements Message {
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

  private String role = "tool";

  public String getRole() {
    return role;
  }

  public void setRole(String value) {
    this.role = value;
  }

  private Object content;

  public Object getContent() {
    return content;
  }

  public void setContent(Object value) {
    this.content = value;
  }

  private String toolCallId;

  public String getToolCallId() {
    return toolCallId;
  }

  public void setToolCallId(String value) {
    this.toolCallId = value;
  }

  private String error;

  public String getError() {
    return error;
  }

  public void setError(String value) {
    this.error = value;
  }

  private String encryptedValue;

  public String getEncryptedValue() {
    return encryptedValue;
  }

  public void setEncryptedValue(String value) {
    this.encryptedValue = value;
  }

  private Map<String, Object> metadata;

  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> value) {
    this.metadata = value;
  }
}
