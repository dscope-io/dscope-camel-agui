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

package io.dscope.camel.agui.processor;

import java.util.Map;
import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import io.dscope.camel.agui.config.AgUiExchangeProperties;
import io.dscope.camel.agui.model.AgUiTextMessageContent;
import io.dscope.camel.agui.model.AgUiTextMessageEnd;
import io.dscope.camel.agui.model.AgUiTextMessageStart;
import io.dscope.camel.agui.service.AgUiSession;
import io.dscope.camel.agui.service.AgUiSessionRegistry;

public class AgUiRunTextProcessor implements Processor {

    private final AgUiSessionRegistry sessionRegistry;

    public AgUiRunTextProcessor(AgUiSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void process(Exchange exchange) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = exchange.getProperty(AgUiExchangeProperties.PARAMS, Map.class);
        String runId = params.get("runId").toString();
        String text = params.getOrDefault("text", "").toString();
        String messageId = params.getOrDefault("messageId", UUID.randomUUID().toString()).toString();

        AgUiSession session = requireSession(runId);
        session.emit(new AgUiTextMessageStart(runId, session.getSessionId(), messageId, "assistant"));
        session.emit(new AgUiTextMessageContent(runId, session.getSessionId(), messageId, text));
        session.emit(new AgUiTextMessageEnd(runId, session.getSessionId(), messageId));

        exchange.setProperty(AgUiExchangeProperties.METHOD_RESULT,
            Map.of("runId", runId, "messageId", messageId, "textLength", text.length()));
    }

    private AgUiSession requireSession(String runId) {
        AgUiSession session = sessionRegistry.get(runId);
        if (session == null) {
            throw new AgUiJsonRpcValidationException("Unknown runId: " + runId);
        }
        return session;
    }
}
