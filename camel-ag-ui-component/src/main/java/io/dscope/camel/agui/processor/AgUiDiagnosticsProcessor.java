package io.dscope.camel.agui.processor;

import io.dscope.camel.agui.model.AgUiMethodCatalog;
import io.dscope.camel.agui.service.AgUiSessionRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class AgUiDiagnosticsProcessor implements Processor {

    private final AgUiSessionRegistry sessionRegistry;
    private final AgUiMethodCatalog methodCatalog;

    public AgUiDiagnosticsProcessor(AgUiSessionRegistry sessionRegistry, AgUiMethodCatalog methodCatalog) {
        this.sessionRegistry = sessionRegistry;
        this.methodCatalog = methodCatalog;
    }

    @Override
    public void process(Exchange exchange) {
        exchange.getMessage().setBody(
            java.util.Map.of(
                "status", "UP",
                "activeSessions", sessionRegistry.activeSessionCount(),
                "supportedMethods", methodCatalog.supportedMethods()
            )
        );
    }
}
