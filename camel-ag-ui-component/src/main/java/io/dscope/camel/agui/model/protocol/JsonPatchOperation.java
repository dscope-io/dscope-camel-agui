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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "op",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = AddOperation.class, name = "add"),
  @JsonSubTypes.Type(value = RemoveOperation.class, name = "remove"),
  @JsonSubTypes.Type(value = ReplaceOperation.class, name = "replace"),
  @JsonSubTypes.Type(value = MoveOperation.class, name = "move"),
  @JsonSubTypes.Type(value = CopyOperation.class, name = "copy"),
  @JsonSubTypes.Type(value = TestOperation.class, name = "test")
})
public interface JsonPatchOperation {}
