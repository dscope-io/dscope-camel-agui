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

public class AgUiTextMessageStart extends AgUiEvent {

    private String messageId;
    private String role;

    public AgUiTextMessageStart(String runId, String sessionId) {
        super(AgUiEventTypes.TEXT_MESSAGE_START, runId, sessionId);
        this.role = "assistant";
    }

    public AgUiTextMessageStart(String runId, String sessionId, String messageId, String role) {
        this(runId, sessionId);
        this.messageId = messageId;
        this.role = role;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
