package io.dscope.camel.agui.processor;

import io.dscope.camel.agui.config.AgUiExchangeProperties;
import io.dscope.camel.agui.model.AgUiRunStarted;
import io.dscope.camel.agui.model.AgUiStepStarted;
import io.dscope.camel.agui.service.AgUiSession;
import io.dscope.camel.agui.service.AgUiSessionRegistry;
import java.util.Map;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class AgUiRunStartProcessor implements Processor {

    private final AgUiSessionRegistry sessionRegistry;

    public AgUiRunStartProcessor(AgUiSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void process(Exchange exchange) {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = exchange.getProperty(AgUiExchangeProperties.PARAMS, Map.class);

        String runId = params.getOrDefault("runId", UUID.randomUUID().toString()).toString();
        String sessionId = params.getOrDefault("sessionId", UUID.randomUUID().toString()).toString();

        AgUiSession session = sessionRegistry.getOrCreate(runId, sessionId);
        exchange.setProperty(AgUiExchangeProperties.RUN_ID, runId);
        exchange.setProperty(AgUiExchangeProperties.SESSION_ID, sessionId);
        exchange.setProperty(AgUiExchangeProperties.SESSION, session);

        session.emit(new AgUiRunStarted(runId, sessionId));
        session.emit(new AgUiStepStarted(runId, sessionId, "default"));

        exchange.setProperty(AgUiExchangeProperties.METHOD_RESULT,
            Map.of("runId", runId, "sessionId", sessionId, "status", "started"));
    }
}
