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
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentCapabilities {
  private IdentityCapabilities identity;

  public IdentityCapabilities getIdentity() {
    return identity;
  }

  public void setIdentity(IdentityCapabilities value) {
    this.identity = value;
  }

  private TransportCapabilities transport;

  public TransportCapabilities getTransport() {
    return transport;
  }

  public void setTransport(TransportCapabilities value) {
    this.transport = value;
  }

  private ToolsCapabilities tools;

  public ToolsCapabilities getTools() {
    return tools;
  }

  public void setTools(ToolsCapabilities value) {
    this.tools = value;
  }

  private OutputCapabilities output;

  public OutputCapabilities getOutput() {
    return output;
  }

  public void setOutput(OutputCapabilities value) {
    this.output = value;
  }

  private StateCapabilities state;

  public StateCapabilities getState() {
    return state;
  }

  public void setState(StateCapabilities value) {
    this.state = value;
  }

  private MultiAgentCapabilities multiAgent;

  public MultiAgentCapabilities getMultiAgent() {
    return multiAgent;
  }

  public void setMultiAgent(MultiAgentCapabilities value) {
    this.multiAgent = value;
  }

  private ReasoningCapabilities reasoning;

  public ReasoningCapabilities getReasoning() {
    return reasoning;
  }

  public void setReasoning(ReasoningCapabilities value) {
    this.reasoning = value;
  }

  private MultimodalCapabilities multimodal;

  public MultimodalCapabilities getMultimodal() {
    return multimodal;
  }

  public void setMultimodal(MultimodalCapabilities value) {
    this.multimodal = value;
  }

  private ExecutionCapabilities execution;

  public ExecutionCapabilities getExecution() {
    return execution;
  }

  public void setExecution(ExecutionCapabilities value) {
    this.execution = value;
  }

  private HumanInTheLoopCapabilities humanInTheLoop;

  public HumanInTheLoopCapabilities getHumanInTheLoop() {
    return humanInTheLoop;
  }

  public void setHumanInTheLoop(HumanInTheLoopCapabilities value) {
    this.humanInTheLoop = value;
  }

  private Map<String, Object> custom;

  public Map<String, Object> getCustom() {
    return custom;
  }

  public void setCustom(Map<String, Object> value) {
    this.custom = value;
  }
}
