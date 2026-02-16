package io.dscope.camel.agui.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class AgUiHealthProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        exchange.getMessage().setBody(java.util.Map.of("status", "UP", "component", "camel-ag-ui"));
    }
}
