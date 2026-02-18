package io.dscope.camel.agui.processor;

import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import io.dscope.camel.agui.config.AgUiExchangeProperties;

public class AgUiPostSseBridgeProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        String runId = resolveRunId(exchange);
        if (runId == null || runId.isBlank()) {
            throw new AgUiJsonRpcValidationException("runId is required for POST+SSE transport");
        }

        exchange.getMessage().setHeader("runId", runId);
        if (exchange.getMessage().getHeader("afterSequence") == null) {
            exchange.getMessage().setHeader("afterSequence", 0L);
        }
        if (exchange.getMessage().getHeader("limit") == null) {
            exchange.getMessage().setHeader("limit", 200);
        }
    }

    @SuppressWarnings("unchecked")
    private String resolveRunId(Exchange exchange) {
        Object paramsObj = exchange.getProperty(AgUiExchangeProperties.PARAMS);
        if (paramsObj instanceof Map<?, ?> params) {
            Object runId = params.get("runId");
            if (runId != null && !runId.toString().isBlank()) {
                return runId.toString();
            }
        }

        Object resultObj = exchange.getProperty(AgUiExchangeProperties.METHOD_RESULT);
        if (resultObj instanceof Map<?, ?> result) {
            Object runId = result.get("runId");
            if (runId != null && !runId.toString().isBlank()) {
                return runId.toString();
            }
        }

        String runId = exchange.getProperty(AgUiExchangeProperties.RUN_ID, String.class);
        if (runId != null && !runId.isBlank()) {
            return runId;
        }
        return null;
    }
}
