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
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MoveOperation implements JsonPatchOperation {
  private String op = "move";

  public String getOp() {
    return op;
  }

  public void setOp(String value) {
    this.op = value;
  }

  private String from;

  public String getFrom() {
    return from;
  }

  public void setFrom(String value) {
    this.from = value;
  }

  private String path;

  public String getPath() {
    return path;
  }

  public void setPath(String value) {
    this.path = value;
  }

  private final Map<String, Object> extensions = new java.util.LinkedHashMap<>();

  @com.fasterxml.jackson.annotation.JsonAnySetter
  public void setExtension(String name, Object value) {
    extensions.put(name, value);
  }

  @com.fasterxml.jackson.annotation.JsonAnyGetter
  public Map<String, Object> getExtensions() {
    return extensions;
  }
}
