package io.dscope.camel.agui.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class AgUiHealthProcessor implements Processor {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void process(Exchange exchange) throws Exception {
        exchange.getMessage().setHeader("Content-Type", "application/json");
        exchange.getMessage().setBody(
            mapper.writeValueAsString(java.util.Map.of("status", "UP", "component", "camel-ag-ui"))
        );
    }
}
