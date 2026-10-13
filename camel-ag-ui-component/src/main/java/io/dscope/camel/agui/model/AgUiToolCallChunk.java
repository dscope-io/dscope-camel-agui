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

import io.dscope.camel.agui.model.protocol.*;

public class AgUiToolCallChunk extends AgUiEvent {
  public AgUiToolCallChunk() {
    super("TOOL_CALL_CHUNK", null, null);
  }

  public AgUiToolCallChunk(String runId, String sessionId) {
    super("TOOL_CALL_CHUNK", runId, sessionId);
  }

  private String toolCallId;

  public String getToolCallId() {
    return toolCallId;
  }

  public void setToolCallId(String value) {
    this.toolCallId = value;
  }

  private String toolCallName;

  public String getToolCallName() {
    return toolCallName;
  }

  public void setToolCallName(String value) {
    this.toolCallName = value;
  }

  private String parentMessageId;

  public String getParentMessageId() {
    return parentMessageId;
  }

  public void setParentMessageId(String value) {
    this.parentMessageId = value;
  }

  private String delta;

  public String getDelta() {
    return delta;
  }

  public void setDelta(String value) {
    this.delta = value;
  }
}
