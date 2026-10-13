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

public class AgUiRunError extends AgUiEvent {
  public AgUiRunError() {
    super("RUN_ERROR", null, null);
  }

  private String message;

  public AgUiRunError(String runId, String sessionId, String error) {
    super(AgUiEventTypes.RUN_ERROR, runId, sessionId);
    this.message = error;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  private String code;

  public String getCode() {
    return code;
  }

  public void setCode(String value) {
    code = value;
  }

  private java.util.List<io.dscope.camel.agui.model.protocol.TokenUsage> usage;

  public java.util.List<io.dscope.camel.agui.model.protocol.TokenUsage> getUsage() {
    return usage;
  }

  public void setUsage(java.util.List<io.dscope.camel.agui.model.protocol.TokenUsage> value) {
    usage = value;
  }
}
