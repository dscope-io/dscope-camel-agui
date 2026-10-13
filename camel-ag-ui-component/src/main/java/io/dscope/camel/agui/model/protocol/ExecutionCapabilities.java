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

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionCapabilities {
  private Boolean codeExecution;

  public Boolean getCodeExecution() {
    return codeExecution;
  }

  public void setCodeExecution(Boolean value) {
    this.codeExecution = value;
  }

  private Boolean sandboxed;

  public Boolean getSandboxed() {
    return sandboxed;
  }

  public void setSandboxed(Boolean value) {
    this.sandboxed = value;
  }

  private Long maxIterations;

  public Long getMaxIterations() {
    return maxIterations;
  }

  public void setMaxIterations(Long value) {
    this.maxIterations = value;
  }

  private Long maxExecutionTime;

  public Long getMaxExecutionTime() {
    return maxExecutionTime;
  }

  public void setMaxExecutionTime(Long value) {
    this.maxExecutionTime = value;
  }
}
