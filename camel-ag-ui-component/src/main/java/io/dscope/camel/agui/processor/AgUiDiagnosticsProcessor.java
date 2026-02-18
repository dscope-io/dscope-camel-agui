package io.dscope.camel.agui.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dscope.camel.agui.model.AgUiMethodCatalog;
import io.dscope.camel.agui.service.AgUiSessionRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class AgUiDiagnosticsProcessor implements Processor {

    private final AgUiSessionRegistry sessionRegistry;
    private final AgUiMethodCatalog methodCatalog;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public AgUiDiagnosticsProcessor(AgUiSessionRegistry sessionRegistry, AgUiMethodCatalog methodCatalog) {
        this.sessionRegistry = sessionRegistry;
        this.methodCatalog = methodCatalog;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        exchange.getMessage().setHeader("Content-Type", "application/json");
        exchange.getMessage().setBody(
            mapper.writeValueAsString(
                java.util.Map.of(
                    "status", "UP",
                    "activeSessions", sessionRegistry.activeSessionCount(),
                    "supportedMethods", methodCatalog.supportedMethods()
                )
            )
        );
    }
}
