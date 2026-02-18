package io.dscope.camel.agui.processor;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import io.dscope.camel.agui.config.AgUiExchangeProperties;

public abstract class AbstractAgUiAgentWidgetProcessor implements Processor {

    @Override
    public final void process(Exchange exchange) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = exchange.getProperty(AgUiExchangeProperties.PARAMS, Map.class);
        if (params == null || params.isEmpty()) {
            return;
        }
        process(exchange, params);
    }

    protected abstract void process(Exchange exchange, Map<String, Object> params) throws Exception;

    protected String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}