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
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RunAgentInput {
  private String threadId;

  public String getThreadId() {
    return threadId;
  }

  public void setThreadId(String value) {
    this.threadId = value;
  }

  private String runId;

  public String getRunId() {
    return runId;
  }

  public void setRunId(String value) {
    this.runId = value;
  }

  private String protocolVersion;

  public String getProtocolVersion() {
    return protocolVersion;
  }

  public void setProtocolVersion(String value) {
    this.protocolVersion = value;
  }

  private String parentRunId;

  public String getParentRunId() {
    return parentRunId;
  }

  public void setParentRunId(String value) {
    this.parentRunId = value;
  }

  private Object state;

  public Object getState() {
    return state;
  }

  public void setState(Object value) {
    this.state = value;
  }

  private List<Message> messages;

  public List<Message> getMessages() {
    return messages;
  }

  public void setMessages(List<Message> value) {
    this.messages = value;
  }

  private List<Tool> tools;

  public List<Tool> getTools() {
    return tools;
  }

  public void setTools(List<Tool> value) {
    this.tools = value;
  }

  private List<Context> context;

  public List<Context> getContext() {
    return context;
  }

  public void setContext(List<Context> value) {
    this.context = value;
  }

  private Object forwardedProps;

  public Object getForwardedProps() {
    return forwardedProps;
  }

  public void setForwardedProps(Object value) {
    this.forwardedProps = value;
  }

  private List<ResumeEntry> resume;

  public List<ResumeEntry> getResume() {
    return resume;
  }

  public void setResume(List<ResumeEntry> value) {
    this.resume = value;
  }
}
