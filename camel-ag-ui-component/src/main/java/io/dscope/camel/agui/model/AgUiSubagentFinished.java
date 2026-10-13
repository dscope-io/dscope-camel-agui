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

public class AgUiSubagentFinished extends AgUiEvent {
  public AgUiSubagentFinished() {
    super("SUBAGENT_FINISHED", null, null);
  }

  public AgUiSubagentFinished(String runId, String sessionId) {
    super("SUBAGENT_FINISHED", runId, sessionId);
  }

  private Object result;

  public Object getResult() {
    return result;
  }

  public void setResult(Object value) {
    this.result = value;
  }

  private SubagentFinishedOutcome outcome;

  public SubagentFinishedOutcome getOutcome() {
    return outcome;
  }

  public void setOutcome(SubagentFinishedOutcome value) {
    this.outcome = value;
  }
}
