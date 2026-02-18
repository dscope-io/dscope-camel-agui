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

import java.util.Map;

public class AgUiToolCallArgs extends AgUiEvent {

    private String toolCallId;
    private String delta;

    public AgUiToolCallArgs(String runId, String sessionId, Map<String, Object> args) {
        super(AgUiEventTypes.TOOL_CALL_ARGS, runId, sessionId);
        this.delta = String.valueOf(args);
    }

    public AgUiToolCallArgs(String runId, String sessionId, String toolCallId, String delta) {
        super(AgUiEventTypes.TOOL_CALL_ARGS, runId, sessionId);
        this.toolCallId = toolCallId;
        this.delta = delta;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public String getDelta() {
        return delta;
    }

    public void setDelta(String delta) {
        this.delta = delta;
    }
}
