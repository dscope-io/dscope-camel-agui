package io.dscope.camel.agui.processor;

import io.dscope.camel.agui.bridge.AgUiToolEventBridge;
import io.dscope.camel.agui.config.AgUiExchangeProperties;
import io.dscope.camel.agui.model.AgUiToolCallArgs;
import io.dscope.camel.agui.model.AgUiToolCallEnd;
import io.dscope.camel.agui.model.AgUiToolCallResult;
import io.dscope.camel.agui.model.AgUiToolCallStart;
import io.dscope.camel.agui.service.AgUiSession;
import io.dscope.camel.agui.service.AgUiSessionRegistry;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class AgUiToolCallProcessor implements Processor {

    private final AgUiSessionRegistry sessionRegistry;
    private final AgUiToolEventBridge toolEventBridge;

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

        toolEventBridge.onToolCallStart(runId, sessionId, toolName, args);
        session.emit(new AgUiToolCallStart(runId, sessionId, toolName));
        session.emit(new AgUiToolCallArgs(runId, sessionId, args));

        Map<String, Object> result = Map.of("ok", true, "toolName", toolName);
        session.emit(new AgUiToolCallResult(runId, sessionId, result));
        session.emit(new AgUiToolCallEnd(runId, sessionId, toolName));
        toolEventBridge.onToolCallResult(runId, sessionId, toolName, result);

        exchange.setProperty(AgUiExchangeProperties.METHOD_RESULT, Map.of("runId", runId, "toolName", toolName, "result", result));
    }

    private AgUiSession requireSession(String runId) {
        AgUiSession session = sessionRegistry.get(runId);
        if (session == null) {
            throw new AgUiJsonRpcValidationException("Unknown runId: " + runId);
        }
        return session;
    }
}
