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
public class TransportCapabilities {
  private Boolean streaming;

  public Boolean getStreaming() {
    return streaming;
  }

  public void setStreaming(Boolean value) {
    this.streaming = value;
  }

  private Boolean websocket;

  public Boolean getWebsocket() {
    return websocket;
  }

  public void setWebsocket(Boolean value) {
    this.websocket = value;
  }

  private Boolean httpBinary;

  public Boolean getHttpBinary() {
    return httpBinary;
  }

  public void setHttpBinary(Boolean value) {
    this.httpBinary = value;
  }

  private Boolean pushNotifications;

  public Boolean getPushNotifications() {
    return pushNotifications;
  }

  public void setPushNotifications(Boolean value) {
    this.pushNotifications = value;
  }

  private Boolean resumable;

  public Boolean getResumable() {
    return resumable;
  }

  public void setResumable(Boolean value) {
    this.resumable = value;
  }
}
