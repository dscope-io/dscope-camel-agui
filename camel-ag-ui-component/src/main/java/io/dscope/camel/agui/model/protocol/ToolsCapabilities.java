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
public class ToolsCapabilities {
  private Boolean supported;

  public Boolean getSupported() {
    return supported;
  }

  public void setSupported(Boolean value) {
    this.supported = value;
  }

  private List<Tool> items;

  public List<Tool> getItems() {
    return items;
  }

  public void setItems(List<Tool> value) {
    this.items = value;
  }

  private Boolean parallelCalls;

  public Boolean getParallelCalls() {
    return parallelCalls;
  }

  public void setParallelCalls(Boolean value) {
    this.parallelCalls = value;
  }

  private Boolean clientProvided;

  public Boolean getClientProvided() {
    return clientProvided;
  }

  public void setClientProvided(Boolean value) {
    this.clientProvided = value;
  }
}
