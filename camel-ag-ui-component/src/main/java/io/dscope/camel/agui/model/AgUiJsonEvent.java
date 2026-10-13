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
import com.fasterxml.jackson.databind.JsonNode;

/** Lossless event envelope for normalized and replayed protocol JSON. */
public final class AgUiJsonEvent extends AgUiEvent {
  private final JsonNode payload;

  public AgUiJsonEvent(JsonNode payload) {
    super(payload.path("type").asText(), null, null);
    this.payload = payload.deepCopy();
  }

  @JsonIgnore
  public JsonNode getPayload() {
    return payload.deepCopy();
  }
}
