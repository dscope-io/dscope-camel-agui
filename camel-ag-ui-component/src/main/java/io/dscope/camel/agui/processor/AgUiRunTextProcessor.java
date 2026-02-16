package io.dscope.camel.agui.processor;

import io.dscope.camel.agui.config.AgUiExchangeProperties;
import io.dscope.camel.agui.model.AgUiTextMessageContent;
import io.dscope.camel.agui.model.AgUiTextMessageEnd;
import io.dscope.camel.agui.model.AgUiTextMessageStart;
import io.dscope.camel.agui.service.AgUiSession;
import io.dscope.camel.agui.service.AgUiSessionRegistry;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

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

        AgUiSession session = requireSession(runId);
        session.emit(new AgUiTextMessageStart(runId, session.getSessionId()));
        session.emit(new AgUiTextMessageContent(runId, session.getSessionId(), text));
        session.emit(new AgUiTextMessageEnd(runId, session.getSessionId()));

        exchange.setProperty(AgUiExchangeProperties.METHOD_RESULT, Map.of("runId", runId, "textLength", text.length()));
    }

    private AgUiSession requireSession(String runId) {
        AgUiSession session = sessionRegistry.get(runId);
        if (session == null) {
            throw new AgUiJsonRpcValidationException("Unknown runId: " + runId);
        }
        return session;
    }
}
