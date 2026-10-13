package io.dscope.camel.agui.processor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AgUiAccessProcessorTest {

    @AfterEach
    void clearToken() {
        System.clearProperty("agui.access.token");
    }

    @Test
    void allowsLoopbackWhenNoTokenIsConfigured() {
        assertDoesNotThrow(() -> new AgUiAccessProcessor().process(exchange("127.0.0.1:9000", null)));
    }

    @Test
    void requiresMatchingBearerTokenForNonLoopbackRequests() {
        System.setProperty("agui.access.token", "secret");
        assertThrows(AgUiAuthorizationException.class,
            () -> new AgUiAccessProcessor().process(exchange("192.0.2.1:9000", "Bearer wrong")));
        assertDoesNotThrow(() -> new AgUiAccessProcessor().process(exchange("192.0.2.1:9000", "Bearer secret")));
    }

    private DefaultExchange exchange(String remoteAddress, String authorization) {
        var exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader("CamelHttpRemoteAddress", remoteAddress);
        if (authorization != null) {
            exchange.getIn().setHeader("Authorization", authorization);
        }
        return exchange;
    }
}
