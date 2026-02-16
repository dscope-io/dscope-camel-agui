package io.dscope.camel.agui;

import io.dscope.camel.agui.config.AgUiExchangeProperties;
import io.dscope.camel.agui.processor.AgUiErrorProcessor;
import io.dscope.camel.agui.processor.AgUiMethodNotFoundException;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgUiErrorProcessorTest {

    @Test
    void serializesErrorEnvelope() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setProperty(AgUiExchangeProperties.REQUEST_ID, "x1");
        exchange.setProperty(Exchange.EXCEPTION_CAUGHT, new AgUiMethodNotFoundException("missing"));

        new AgUiErrorProcessor().process(exchange);

        String body = exchange.getMessage().getBody(String.class);
        Assertions.assertTrue(body.contains("\"code\":-32601"));
        Assertions.assertTrue(body.contains("missing"));
    }
}
