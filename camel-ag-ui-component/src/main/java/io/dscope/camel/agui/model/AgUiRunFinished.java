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

public class AgUiRunFinished extends AgUiEvent {
  public AgUiRunFinished() {
    super("RUN_FINISHED", null, null);
  }

  public AgUiRunFinished(String runId, String sessionId) {
    super(AgUiEventTypes.RUN_FINISHED, runId, sessionId);
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

  private io.dscope.camel.agui.model.protocol.RunFinishedOutcome outcome;

  public io.dscope.camel.agui.model.protocol.RunFinishedOutcome getOutcome() {
    return outcome;
  }

  public void setOutcome(io.dscope.camel.agui.model.protocol.RunFinishedOutcome value) {
    outcome = value;
  }

  private Object result;

  public Object getResult() {
    return result;
  }

  public void setResult(Object value) {
    result = value;
  }

  private java.util.List<io.dscope.camel.agui.model.protocol.TokenUsage> usage;

  public java.util.List<io.dscope.camel.agui.model.protocol.TokenUsage> getUsage() {
    return usage;
  }

  public void setUsage(java.util.List<io.dscope.camel.agui.model.protocol.TokenUsage> value) {
    usage = value;
  }
}
