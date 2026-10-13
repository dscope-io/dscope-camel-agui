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
public class VideoPart implements ContentPart {
  private String type = "video";

  public String getType() {
    return type;
  }

  public void setType(String value) {
    this.type = value;
  }

  private String id;

  public String getId() {
    return id;
  }

  public void setId(String value) {
    this.id = value;
  }

  private PartSource source;

  public PartSource getSource() {
    return source;
  }

  public void setSource(PartSource value) {
    this.source = value;
  }

  private Object metadata;

  public Object getMetadata() {
    return metadata;
  }

  public void setMetadata(Object value) {
    this.metadata = value;
  }
}
