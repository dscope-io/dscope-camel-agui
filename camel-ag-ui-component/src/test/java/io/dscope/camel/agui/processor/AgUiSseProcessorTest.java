package io.dscope.camel.agui.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.dscope.camel.agui.model.AgUiTextMessageContent;
import io.dscope.camel.agui.service.InMemoryAgUiSessionRegistry;
import io.dscope.camel.agui.service.JacksonAgUiEventCodec;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

class AgUiSseProcessorTest {

    @Test
    void usesLastEventIdAndCapsTheRequestedPageSize() throws Exception {
        InMemoryAgUiSessionRegistry registry = new InMemoryAgUiSessionRegistry(new JacksonAgUiEventCodec());
        var session = registry.getOrCreate("run-1", "session-1");
        for (int index = 0; index < 505; index++) {
            session.emit(new AgUiTextMessageContent("run-1", "session-1", "message", "text"));
        }

        var exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getIn().setHeader("runId", "run-1");
        exchange.getIn().setHeader("Last-Event-ID", 10L);
        exchange.getIn().setHeader("limit", 10_000);

        new AgUiSseProcessor(registry).process(exchange);

        String body = exchange.getMessage().getBody(String.class);
        assertTrue(body.startsWith("id: 11"));
        assertEquals(495, body.lines().filter(line -> line.startsWith("id:")).count());
        assertEquals(505L, exchange.getMessage().getHeader("X-AGUI-Next-Sequence", Long.class));
    }
}
