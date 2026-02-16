package io.dscope.camel.agui.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dscope.camel.agui.config.AgUiExchangeProperties;
import io.dscope.camel.agui.config.AgUiProtocolDefaults;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class AgUiMethodDispatchProcessor implements Processor {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final Map<String, Processor> methodProcessors;

    public AgUiMethodDispatchProcessor(Map<String, Processor> methodProcessors) {
        this.methodProcessors = Map.copyOf(methodProcessors);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String method = exchange.getProperty(AgUiExchangeProperties.METHOD, String.class);
        Processor processor = methodProcessors.get(method);
        if (processor == null) {
            throw new AgUiMethodNotFoundException("Method not found: " + method);
        }

        processor.process(exchange);

        String envelopeType = exchange.getProperty(AgUiExchangeProperties.ENVELOPE_TYPE, String.class);
        if ("notification".equals(envelopeType)) {
            exchange.getMessage().setBody("");
            return;
        }

        Map<String, Object> resultEnvelope = new LinkedHashMap<>();
        resultEnvelope.put("jsonrpc", AgUiProtocolDefaults.JSONRPC_VERSION);
        resultEnvelope.put("id", exchange.getProperty(AgUiExchangeProperties.REQUEST_ID));
        resultEnvelope.put("result", exchange.getProperty(AgUiExchangeProperties.METHOD_RESULT, Map.class));
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "application/json");
        exchange.getMessage().setBody(mapper.writeValueAsString(resultEnvelope));
    }
}
