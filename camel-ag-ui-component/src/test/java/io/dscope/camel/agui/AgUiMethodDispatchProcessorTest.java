package io.dscope.camel.agui;

import io.dscope.camel.agui.config.AgUiExchangeProperties;
import io.dscope.camel.agui.processor.AgUiMethodDispatchProcessor;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgUiMethodDispatchProcessorTest {

    @Test
    void wrapsResultEnvelope() throws Exception {
        Processor p = ex -> ex.setProperty(AgUiExchangeProperties.METHOD_RESULT, Map.of("status", "ok"));
        AgUiMethodDispatchProcessor processor = new AgUiMethodDispatchProcessor(Map.of("health", p));

        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setProperty(AgUiExchangeProperties.METHOD, "health");
        exchange.setProperty(AgUiExchangeProperties.ENVELOPE_TYPE, "request");
        exchange.setProperty(AgUiExchangeProperties.REQUEST_ID, "abc");

        processor.process(exchange);

        String body = exchange.getMessage().getBody(String.class);
        Assertions.assertTrue(body.contains("\"jsonrpc\":\"2.0\""));
        Assertions.assertTrue(body.contains("\"id\":\"abc\""));
    }
}
