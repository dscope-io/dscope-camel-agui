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
import java.util.List;

public class AgUiActivityDelta extends AgUiEvent {
  public AgUiActivityDelta() {
    super("ACTIVITY_DELTA", null, null);
  }

  public AgUiActivityDelta(String runId, String sessionId) {
    super("ACTIVITY_DELTA", runId, sessionId);
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

  private List<JsonPatchOperation> patch;

  public List<JsonPatchOperation> getPatch() {
    return patch;
  }

  public void setPatch(List<JsonPatchOperation> value) {
    this.patch = value;
  }
}
