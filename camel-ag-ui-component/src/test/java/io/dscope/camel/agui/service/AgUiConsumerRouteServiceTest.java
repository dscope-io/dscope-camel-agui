package io.dscope.camel.agui.service;

import io.dscope.camel.agui.AgUiComponent;
import io.dscope.camel.agui.AgUiConfiguration;
import io.dscope.camel.agui.AgUiEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgUiConsumerRouteServiceTest {

    @Test
    void registersHttpAndWebSocketDefinitionsWhenEnabled() throws Exception {
        DefaultCamelContext context = new DefaultCamelContext();
        AgUiComponent component = new AgUiComponent();
        component.setCamelContext(context);

        AgUiConfiguration configuration = new AgUiConfiguration();
        configuration.setAgentId("test-agent");
        configuration.setServerUrl("undertow:http://0.0.0.0:19091");
        configuration.setRpcPath("/rpc-test");
        configuration.setWebsocketEnabled(true);
        configuration.setWsPath("/ws-test");

        AgUiEndpoint endpoint = new AgUiEndpoint("agui:test-agent", component, configuration, "test-agent");
        AgUiConsumerRouteService routeService = new AgUiConsumerRouteService();

        routeService.registerRoute(endpoint, exchange -> {
        });

        ModelCamelContext modelContext = context;
        boolean hasRpc = modelContext.getRouteDefinitions().stream()
            .anyMatch(r -> r.getId().startsWith("agui-consumer-"));
        boolean hasWs = modelContext.getRouteDefinitions().stream()
            .anyMatch(r -> r.getId().startsWith("agui-ws-consumer-"));

        Assertions.assertTrue(hasRpc);
        Assertions.assertTrue(hasWs);
    }
}
