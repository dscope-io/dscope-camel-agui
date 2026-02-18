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

import io.dscope.camel.agui.bridge.AgUiToolEventBridge;
import io.dscope.camel.agui.config.AgUiExchangeProperties;
import io.dscope.camel.agui.model.AgUiToolCallArgs;
import io.dscope.camel.agui.model.AgUiToolCallEnd;
import io.dscope.camel.agui.model.AgUiToolCallResult;
import io.dscope.camel.agui.model.AgUiToolCallStart;
import io.dscope.camel.agui.service.AgUiSession;
import io.dscope.camel.agui.service.AgUiSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class AgUiToolCallProcessor implements Processor {

    private final AgUiSessionRegistry sessionRegistry;
    private final AgUiToolEventBridge toolEventBridge;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public AgUiToolCallProcessor(AgUiSessionRegistry sessionRegistry, AgUiToolEventBridge toolEventBridge) {
        this.sessionRegistry = sessionRegistry;
        this.toolEventBridge = toolEventBridge;
    }

    @Override
    public void process(Exchange exchange) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = exchange.getProperty(AgUiExchangeProperties.PARAMS, Map.class);
        String runId = params.get("runId").toString();
        String toolName = params.getOrDefault("toolName", "unknown").toString();
        @SuppressWarnings("unchecked")
        Map<String, Object> args = (Map<String, Object>) params.getOrDefault("args", Map.of());

        AgUiSession session = requireSession(runId);
        String sessionId = session.getSessionId();
        String toolCallId = params.getOrDefault("toolCallId", UUID.randomUUID().toString()).toString();
        String messageId = params.getOrDefault("messageId", UUID.randomUUID().toString()).toString();

        toolEventBridge.onToolCallStart(runId, sessionId, toolName, args);
        session.emit(new AgUiToolCallStart(runId, sessionId, toolCallId, toolName));

        String argsDelta;
        try {
            argsDelta = mapper.writeValueAsString(args);
        } catch (Exception e) {
            argsDelta = "{}";
        }
        session.emit(new AgUiToolCallArgs(runId, sessionId, toolCallId, argsDelta));

        Map<String, Object> result = Map.of("ok", true, "toolName", toolName);
        String resultContent;
        try {
            resultContent = mapper.writeValueAsString(result);
        } catch (Exception e) {
            resultContent = "{}";
        }
        session.emit(new AgUiToolCallResult(runId, sessionId, messageId, toolCallId, resultContent, "tool"));
        session.emit(new AgUiToolCallEnd(runId, sessionId, toolCallId, toolName));
        toolEventBridge.onToolCallResult(runId, sessionId, toolName, result);

        exchange.setProperty(AgUiExchangeProperties.METHOD_RESULT,
            Map.of("runId", runId, "toolName", toolName, "toolCallId", toolCallId, "messageId", messageId, "result", result));
    }

    private AgUiSession requireSession(String runId) {
        AgUiSession session = sessionRegistry.get(runId);
        if (session == null) {
            throw new AgUiJsonRpcValidationException("Unknown runId: " + runId);
        }
        return session;
    }
}
