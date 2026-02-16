package io.dscope.camel.agui.processor;

import io.dscope.camel.agui.config.AgUiExchangeProperties;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class AgUiHealthMethodProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        exchange.setProperty(AgUiExchangeProperties.METHOD_RESULT, Map.of("status", "UP"));
    }
}
