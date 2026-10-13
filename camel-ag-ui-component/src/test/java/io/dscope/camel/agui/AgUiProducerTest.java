package io.dscope.camel.agui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

class AgUiProducerTest {

    @Test
    void serializesTheJsonRpcEnvelopeBeforeSendingIt() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/agui/rpc", request -> {
            requestBody.set(new String(request.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
            request.sendResponseHeaders(200, response.length);
            request.getResponseBody().write(response);
            request.close();
        });
        server.start();

        DefaultCamelContext context = new DefaultCamelContext();
        context.start();
        try {
            AgUiConfiguration configuration = new AgUiConfiguration();
            configuration.setRemoteUrl("http://127.0.0.1:" + server.getAddress().getPort());
            AgUiComponent component = new AgUiComponent();
            component.setCamelContext(context);
            AgUiEndpoint endpoint = new AgUiEndpoint("agui:test", component, configuration, "test");
            var producer = endpoint.createProducer();
            producer.start();
            try {
                var exchange = new DefaultExchange(context);
                exchange.getIn().setBody(Map.of("text", "hello"));
                producer.process(exchange);
                assertEquals("{}", exchange.getMessage().getBody(String.class));
            } finally {
                producer.stop();
            }
        } finally {
            context.stop();
            server.stop(0);
        }

        assertTrue(requestBody.get().contains("\"jsonrpc\":\"2.0\""));
        assertTrue(requestBody.get().contains("\"text\":\"hello\""));
    }
}
