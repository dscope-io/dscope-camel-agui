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

import io.dscope.camel.agui.model.protocol.*;
import java.util.Map;

public class AgUiActivitySnapshot extends AgUiEvent {
  public AgUiActivitySnapshot() {
    super("ACTIVITY_SNAPSHOT", null, null);
  }

  public AgUiActivitySnapshot(String runId, String sessionId) {
    super("ACTIVITY_SNAPSHOT", runId, sessionId);
  }

  private String messageId;

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String value) {
    this.messageId = value;
  }

  private String activityType;

  public String getActivityType() {
    return activityType;
  }

  public void setActivityType(String value) {
    this.activityType = value;
  }

  private Map<String, Object> content;

  public Map<String, Object> getContent() {
    return content;
  }

  public void setContent(Map<String, Object> value) {
    this.content = value;
  }

  private Boolean replace;

  public Boolean getReplace() {
    return replace;
  }

  public void setReplace(Boolean value) {
    this.replace = value;
  }
}
