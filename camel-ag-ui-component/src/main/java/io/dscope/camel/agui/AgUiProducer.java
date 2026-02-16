package io.dscope.camel.agui;

import io.dscope.camel.agui.config.AgUiProtocolDefaults;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultProducer;

public class AgUiProducer extends DefaultProducer {

    private final AgUiEndpoint endpoint;

    public AgUiProducer(AgUiEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Map<String, Object> params = exchange.getIn().getBody(Map.class);
        if (params == null) {
            params = Map.of();
        }

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("jsonrpc", AgUiProtocolDefaults.JSONRPC_VERSION);
        envelope.put("id", UUID.randomUUID().toString());
        envelope.put("method", endpoint.getConfiguration().getMethod());
        envelope.put("params", params);

        String target = buildHttpUri(endpoint.getConfiguration().getRemoteUrl(), endpoint.getConfiguration().getRpcPath());
        ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate();
        Object response = producerTemplate.requestBodyAndHeader(
            target,
            envelope,
            Exchange.CONTENT_TYPE,
            "application/json"
        );
        exchange.getMessage().setBody(response);
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "application/json");
        producerTemplate.stop();
    }

    private String buildHttpUri(String base, String path) {
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        if (normalizedBase.startsWith("http")) {
            return normalizedBase + normalizedPath;
        }
        if (normalizedBase.startsWith("undertow:")) {
            return normalizedBase.substring("undertow:".length()) + normalizedPath;
        }
        return "http://" + normalizedBase + normalizedPath;
    }
}
