package io.dscope.camel.agui.processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dscope.camel.agui.config.AgUiExchangeProperties;
import io.dscope.camel.agui.config.AgUiProtocolDefaults;
import io.dscope.camel.agui.config.AgUiProtocolMethods;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

public class AgUiJsonRpcEnvelopeProcessor implements Processor {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };

    @Override
    public void process(Exchange exchange) throws Exception {
        Map<String, Object> envelope = readEnvelope(exchange.getIn());
        exchange.setProperty(AgUiExchangeProperties.RAW_PAYLOAD, envelope);

        String jsonRpc = asString(envelope.get("jsonrpc"));
        if (!AgUiProtocolDefaults.JSONRPC_VERSION.equals(jsonRpc)) {
            throw new AgUiJsonRpcValidationException("Unsupported jsonrpc version");
        }

        String method = asString(envelope.get("method"));
        if (method == null || method.isBlank()) {
            throw new AgUiJsonRpcValidationException("method is required");
        }
        if (!AgUiProtocolMethods.CORE_METHODS.contains(method)) {
            throw new AgUiMethodNotFoundException("Method not found: " + method);
        }

        Object id = envelope.get("id");
        exchange.setProperty(AgUiExchangeProperties.ENVELOPE_TYPE, id == null ? "notification" : "request");
        exchange.setProperty(AgUiExchangeProperties.REQUEST_ID, id);
        exchange.setProperty(AgUiExchangeProperties.METHOD, method);

        Object params = envelope.get("params");
        if (params == null) {
            params = Map.of();
        }
        if (!(params instanceof Map<?, ?>)) {
            throw new AgUiJsonRpcValidationException("params must be an object");
        }
        exchange.setProperty(AgUiExchangeProperties.PARAMS, params);
        exchange.getMessage().setBody(params);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readEnvelope(Message message) throws Exception {
        Object body = message.getBody();
        if (body instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        String json = message.getBody(String.class);
        if (json == null || json.isBlank()) {
            throw new AgUiJsonRpcValidationException("Request body must be a JSON-RPC object");
        }
        return MAPPER.readValue(json, MAP_TYPE);
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
