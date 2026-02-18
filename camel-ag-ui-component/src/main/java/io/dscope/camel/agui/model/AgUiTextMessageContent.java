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

public class AgUiTextMessageContent extends AgUiEvent {

    private String messageId;
    private String delta;

    public AgUiTextMessageContent(String runId, String sessionId, String text) {
        super(AgUiEventTypes.TEXT_MESSAGE_CONTENT, runId, sessionId);
        this.delta = text;
    }

    public AgUiTextMessageContent(String runId, String sessionId, String messageId, String delta) {
        this(runId, sessionId, delta);
        this.messageId = messageId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getDelta() {
        return delta;
    }

    public void setDelta(String delta) {
        this.delta = delta;
    }
}
