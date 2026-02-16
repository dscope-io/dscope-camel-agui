package io.dscope.camel.agui;

import io.dscope.camel.agui.config.AgUiExchangeProperties;
import io.dscope.camel.agui.processor.AgUiJsonRpcEnvelopeProcessor;
import io.dscope.camel.agui.processor.AgUiJsonRpcValidationException;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AgUiJsonRpcEnvelopeProcessorTest {

    @Test
    void parsesValidEnvelope() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(Map.of(
            "jsonrpc", "2.0",
            "id", "1",
            "method", "run.start",
            "params", Map.of("runId", "r1")
        ));

        new AgUiJsonRpcEnvelopeProcessor().process(exchange);

        Assertions.assertEquals("run.start", exchange.getProperty(AgUiExchangeProperties.METHOD));
        Assertions.assertEquals("request", exchange.getProperty(AgUiExchangeProperties.ENVELOPE_TYPE));
    }

    @Test
    void rejectsInvalidMethod() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(Map.of(
            "jsonrpc", "2.0",
            "id", "1",
            "method", "unknown.method",
            "params", Map.of()
        ));

        Assertions.assertThrows(RuntimeException.class, () -> new AgUiJsonRpcEnvelopeProcessor().process(exchange));
    }

    @Test
    void rejectsInvalidVersion() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(Map.of(
            "jsonrpc", "1.0",
            "id", "1",
            "method", "run.start",
            "params", Map.of()
        ));

        Assertions.assertThrows(AgUiJsonRpcValidationException.class, () -> new AgUiJsonRpcEnvelopeProcessor().process(exchange));
    }
}
