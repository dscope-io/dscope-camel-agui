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
public class HumanInTheLoopCapabilities {
  private Boolean supported;

  public Boolean getSupported() {
    return supported;
  }

  public void setSupported(Boolean value) {
    this.supported = value;
  }

  private Boolean approvals;

  public Boolean getApprovals() {
    return approvals;
  }

  public void setApprovals(Boolean value) {
    this.approvals = value;
  }

  private Boolean interventions;

  public Boolean getInterventions() {
    return interventions;
  }

  public void setInterventions(Boolean value) {
    this.interventions = value;
  }

  private Boolean feedback;

  public Boolean getFeedback() {
    return feedback;
  }

  public void setFeedback(Boolean value) {
    this.feedback = value;
  }

  private Boolean interrupts;

  public Boolean getInterrupts() {
    return interrupts;
  }

  public void setInterrupts(Boolean value) {
    this.interrupts = value;
  }

  private Boolean approveWithEdits;

  public Boolean getApproveWithEdits() {
    return approveWithEdits;
  }

  public void setApproveWithEdits(Boolean value) {
    this.approveWithEdits = value;
  }
}
