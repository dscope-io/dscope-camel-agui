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
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MultiAgentCapabilities {
  private Boolean supported;

  public Boolean getSupported() {
    return supported;
  }

  public void setSupported(Boolean value) {
    this.supported = value;
  }

  private Boolean delegation;

  public Boolean getDelegation() {
    return delegation;
  }

  public void setDelegation(Boolean value) {
    this.delegation = value;
  }

  private Boolean handoffs;

  public Boolean getHandoffs() {
    return handoffs;
  }

  public void setHandoffs(Boolean value) {
    this.handoffs = value;
  }

  private List<SubagentInfo> subagents;

  public List<SubagentInfo> getSubagents() {
    return subagents;
  }

  public void setSubagents(List<SubagentInfo> value) {
    this.subagents = value;
  }
}
