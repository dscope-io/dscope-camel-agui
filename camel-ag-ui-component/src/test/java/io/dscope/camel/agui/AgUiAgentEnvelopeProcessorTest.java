package io.dscope.camel.agui;

import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.dscope.camel.agui.config.AgUiExchangeProperties;
import io.dscope.camel.agui.processor.AgUiAgentEnvelopeProcessor;

class AgUiAgentEnvelopeProcessorTest {

    @Test
    void extractsTextFromStateMessagesUserParts() throws Exception {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setBody(Map.of(
            "threadId", "demo-thread",
            "runId", "demo-run",
            "state", Map.of(
                "messages", List.of(
                    Map.of(
                        "id", "m1",
                        "role", "assistant",
                        "parts", List.of(Map.of("type", "text", "text", "Acknowledged."))
                    ),
                    Map.of(
                        "id", "m2",
                        "role", "user",
                        "parts", List.of(Map.of("type", "text", "text", "What service slots are available tomorrow morning?"))
                    )
                )
            )
        ));

        new AgUiAgentEnvelopeProcessor().process(exchange);

        Assertions.assertEquals("run.text", exchange.getProperty(AgUiExchangeProperties.METHOD));
        @SuppressWarnings("unchecked")
        Map<String, Object> params = exchange.getProperty(AgUiExchangeProperties.PARAMS, Map.class);
        Assertions.assertNotNull(params);
        Assertions.assertEquals("What service slots are available tomorrow morning?", params.get("text"));
        Assertions.assertEquals("demo-thread", params.get("threadId"));
        Assertions.assertEquals("demo-thread", params.get("sessionId"));
        Assertions.assertEquals("demo-run", params.get("runId"));
    }
}