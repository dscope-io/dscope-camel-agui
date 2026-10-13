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
public class TokenUsage {
  private String provider;

  public String getProvider() {
    return provider;
  }

  public void setProvider(String value) {
    this.provider = value;
  }

  private String model;

  public String getModel() {
    return model;
  }

  public void setModel(String value) {
    this.model = value;
  }

  private Long inputTokens;

  public Long getInputTokens() {
    return inputTokens;
  }

  public void setInputTokens(Long value) {
    this.inputTokens = value;
  }

  private Long outputTokens;

  public Long getOutputTokens() {
    return outputTokens;
  }

  public void setOutputTokens(Long value) {
    this.outputTokens = value;
  }

  private Long totalTokens;

  public Long getTotalTokens() {
    return totalTokens;
  }

  public void setTotalTokens(Long value) {
    this.totalTokens = value;
  }

  private Long reasoningTokens;

  public Long getReasoningTokens() {
    return reasoningTokens;
  }

  public void setReasoningTokens(Long value) {
    this.reasoningTokens = value;
  }

  private Long cachedInputTokens;

  public Long getCachedInputTokens() {
    return cachedInputTokens;
  }

  public void setCachedInputTokens(Long value) {
    this.cachedInputTokens = value;
  }

  private Long cacheWriteInputTokens;

  public Long getCacheWriteInputTokens() {
    return cacheWriteInputTokens;
  }

  public void setCacheWriteInputTokens(Long value) {
    this.cacheWriteInputTokens = value;
  }
}
