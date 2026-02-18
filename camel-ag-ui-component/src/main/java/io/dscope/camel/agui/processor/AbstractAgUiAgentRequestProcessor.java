package io.dscope.camel.agui.processor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import io.dscope.camel.agui.AgUiComponentApplicationSupport;
import io.dscope.camel.agui.config.AgUiExchangeProperties;
import io.dscope.camel.agui.config.AgUiProtocolMethods;

public abstract class AbstractAgUiAgentRequestProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        boolean explicitMethod = Boolean.TRUE.equals(exchange.getProperty("agui.agent.explicit.method", Boolean.class));

        Processor methodDispatch = exchange.getContext().getRegistry()
            .lookupByNameAndType("agUiMethodDispatchProcessor", Processor.class);
        if (methodDispatch == null) {
            throw new IllegalStateException("agUiMethodDispatchProcessor bean is not available");
        }

        if (explicitMethod) {
            methodDispatch.process(exchange);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> params = exchange.getProperty(AgUiExchangeProperties.PARAMS, Map.class);
        Map<String, Object> mutableParams = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);

        String runId = stringValue(mutableParams.get("runId"));
        if (runId == null || runId.isBlank()) {
            runId = UUID.randomUUID().toString();
            mutableParams.put("runId", runId);
        }

        String sessionId = stringValue(mutableParams.get("sessionId"));
        String threadId = stringValue(mutableParams.get("threadId"));
        if ((sessionId == null || sessionId.isBlank()) && threadId != null && !threadId.isBlank()) {
            sessionId = threadId;
        }
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
            mutableParams.put("sessionId", sessionId);
        }
        if (threadId == null || threadId.isBlank()) {
            threadId = sessionId;
            mutableParams.put("threadId", threadId);
        }

        String text = stringValue(mutableParams.get("text"));
        if (text == null || text.isBlank()) {
            text = "Acknowledged.";
            mutableParams.put("text", text);
        }

        if (stringValue(mutableParams.get("messageId")) == null || stringValue(mutableParams.get("messageId")).isBlank()) {
            mutableParams.put("messageId", UUID.randomUUID().toString());
        }

        exchange.setProperty(AgUiExchangeProperties.PARAMS, mutableParams);
        dispatch(methodDispatch, exchange, AgUiProtocolMethods.RUN_START, mutableParams);

        beforeRunText(exchange, mutableParams, runId, sessionId, threadId);

        dispatch(methodDispatch, exchange, AgUiProtocolMethods.RUN_TEXT, mutableParams);
        dispatch(methodDispatch, exchange, AgUiProtocolMethods.RUN_FINISH, Map.of("runId", runId));

        exchange.setProperty(AgUiExchangeProperties.RUN_ID, runId);
        exchange.setProperty(AgUiExchangeProperties.SESSION_ID, sessionId);
        exchange.setProperty(AgUiExchangeProperties.PARAMS, mutableParams);
        exchange.setProperty(
            AgUiExchangeProperties.METHOD_RESULT,
            Map.of(
                "runId", runId,
                "sessionId", sessionId,
                "threadId", threadId,
                "messageId", stringValue(mutableParams.get("messageId")),
                "status", "finished",
                "textLength", stringValue(mutableParams.get("text")).length()));
    }

    protected void beforeRunText(Exchange exchange, Map<String, Object> params, String runId, String sessionId, String threadId)
        throws Exception {
        Processor widgetProcessor = exchange.getContext().getRegistry()
            .lookupByNameAndType(AgUiComponentApplicationSupport.BEAN_AGENT_PRE_RUN_TEXT_PROCESSOR, Processor.class);
        if (widgetProcessor != null) {
            widgetProcessor.process(exchange);
        }
    }

    protected void dispatch(Processor methodDispatch, Exchange exchange, String method, Map<String, Object> params) throws Exception {
        exchange.setProperty(AgUiExchangeProperties.METHOD, method);
        exchange.setProperty(AgUiExchangeProperties.PARAMS, params);
        methodDispatch.process(exchange);
    }

    protected String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}