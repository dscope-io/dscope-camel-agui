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
