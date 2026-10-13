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
public class StateCapabilities {
  private Boolean snapshots;

  public Boolean getSnapshots() {
    return snapshots;
  }

  public void setSnapshots(Boolean value) {
    this.snapshots = value;
  }

  private Boolean deltas;

  public Boolean getDeltas() {
    return deltas;
  }

  public void setDeltas(Boolean value) {
    this.deltas = value;
  }

  private Boolean memory;

  public Boolean getMemory() {
    return memory;
  }

  public void setMemory(Boolean value) {
    this.memory = value;
  }

  private Boolean persistentState;

  public Boolean getPersistentState() {
    return persistentState;
  }

  public void setPersistentState(Boolean value) {
    this.persistentState = value;
  }
}
