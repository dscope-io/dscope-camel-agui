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

public class AgUiToolCallStart extends AgUiEvent {

    private String toolCallId;
    private String toolCallName;

    public AgUiToolCallStart(String runId, String sessionId, String toolName) {
        super(AgUiEventTypes.TOOL_CALL_START, runId, sessionId);
        this.toolCallName = toolName;
    }

    public AgUiToolCallStart(String runId, String sessionId, String toolCallId, String toolCallName) {
        this(runId, sessionId, toolCallName);
        this.toolCallId = toolCallId;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public String getToolCallName() {
        return toolCallName;
    }

    public void setToolCallName(String toolCallName) {
        this.toolCallName = toolCallName;
    }
}
