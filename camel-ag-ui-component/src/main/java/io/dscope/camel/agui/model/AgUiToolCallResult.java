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

public class AgUiToolCallResult extends AgUiEvent {

    private String messageId;
    private String toolCallId;
    private String content;
    private String role;

    public AgUiToolCallResult(String runId, String sessionId, Object result) {
        super(AgUiEventTypes.TOOL_CALL_RESULT, runId, sessionId);
        this.content = result == null ? "" : result.toString();
    }

    public AgUiToolCallResult(String runId, String sessionId, String messageId, String toolCallId, String content, String role) {
        super(AgUiEventTypes.TOOL_CALL_RESULT, runId, sessionId);
        this.messageId = messageId;
        this.toolCallId = toolCallId;
        this.content = content;
        this.role = role;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
