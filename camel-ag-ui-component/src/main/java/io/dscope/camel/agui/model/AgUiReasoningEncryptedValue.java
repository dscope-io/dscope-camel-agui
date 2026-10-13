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

public class AgUiReasoningEncryptedValue extends AgUiEvent {
  public AgUiReasoningEncryptedValue() {
    super("REASONING_ENCRYPTED_VALUE", null, null);
  }

  public AgUiReasoningEncryptedValue(String runId, String sessionId) {
    super("REASONING_ENCRYPTED_VALUE", runId, sessionId);
  }

  private String subtype;

  public String getSubtype() {
    return subtype;
  }

  public void setSubtype(String value) {
    this.subtype = value;
  }

  private String entityId;

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String value) {
    this.entityId = value;
  }

  private String encryptedValue;

  public String getEncryptedValue() {
    return encryptedValue;
  }

  public void setEncryptedValue(String value) {
    this.encryptedValue = value;
  }
}
