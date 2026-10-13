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
public class MultimodalInputCapabilities {
  private Boolean image;

  public Boolean getImage() {
    return image;
  }

  public void setImage(Boolean value) {
    this.image = value;
  }

  private Boolean audio;

  public Boolean getAudio() {
    return audio;
  }

  public void setAudio(Boolean value) {
    this.audio = value;
  }

  private Boolean video;

  public Boolean getVideo() {
    return video;
  }

  public void setVideo(Boolean value) {
    this.video = value;
  }

  private Boolean pdf;

  public Boolean getPdf() {
    return pdf;
  }

  public void setPdf(Boolean value) {
    this.pdf = value;
  }

  private Boolean file;

  public Boolean getFile() {
    return file;
  }

  public void setFile(Boolean value) {
    this.file = value;
  }
}
