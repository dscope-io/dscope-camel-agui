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
    property = "role",
    visible = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = DeveloperMessage.class, name = "developer"),
  @JsonSubTypes.Type(value = SystemMessage.class, name = "system"),
  @JsonSubTypes.Type(value = AssistantMessage.class, name = "assistant"),
  @JsonSubTypes.Type(value = UserMessage.class, name = "user"),
  @JsonSubTypes.Type(value = ToolMessage.class, name = "tool"),
  @JsonSubTypes.Type(value = ActivityMessage.class, name = "activity"),
  @JsonSubTypes.Type(value = ReasoningMessage.class, name = "reasoning")
})
public interface Message {}
