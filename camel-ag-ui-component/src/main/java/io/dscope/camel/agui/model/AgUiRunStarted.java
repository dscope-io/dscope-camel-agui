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

public class AgUiRunStarted extends AgUiEvent {
  public AgUiRunStarted() {
    super("RUN_STARTED", null, null);
  }

  public AgUiRunStarted(String runId, String sessionId) {
    super(AgUiEventTypes.RUN_STARTED, runId, sessionId);
  }

  @Override
  @JsonIgnore(false)
  public String getRunId() {
    return super.getRunId();
  }

  @Override
  @JsonIgnore(false)
  public String getThreadId() {
    return super.getThreadId();
  }

  private String protocolVersion;

  public String getProtocolVersion() {
    return protocolVersion;
  }

  public void setProtocolVersion(String value) {
    protocolVersion = value;
  }

  private String parentRunId;

  public String getParentRunId() {
    return parentRunId;
  }

  public void setParentRunId(String value) {
    parentRunId = value;
  }

  private io.dscope.camel.agui.model.protocol.RunAgentInput input;

  public io.dscope.camel.agui.model.protocol.RunAgentInput getInput() {
    return input;
  }

  public void setInput(io.dscope.camel.agui.model.protocol.RunAgentInput value) {
    input = value;
  }
}
