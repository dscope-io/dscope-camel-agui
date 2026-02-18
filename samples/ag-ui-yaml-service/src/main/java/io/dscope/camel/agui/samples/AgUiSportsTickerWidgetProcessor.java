package io.dscope.camel.agui.samples;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.camel.Exchange;

import io.dscope.camel.agui.model.AgUiToolCallArgs;
import io.dscope.camel.agui.model.AgUiToolCallEnd;
import io.dscope.camel.agui.model.AgUiToolCallResult;
import io.dscope.camel.agui.model.AgUiToolCallStart;
import io.dscope.camel.agui.processor.AbstractAgUiAgentWidgetProcessor;
import io.dscope.camel.agui.service.AgUiSession;
import io.dscope.camel.agui.service.AgUiSessionRegistry;

final class AgUiSportsTickerWidgetProcessor extends AbstractAgUiAgentWidgetProcessor {

    private static final String SPORTS_TOOL_NAME = "get_sports_ticker";
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Override
    protected void process(Exchange exchange, Map<String, Object> params) {
        String text = stringValue(params.get("text"));
        if (!isSportsIntent(text)) {
            return;
        }

        String runId = stringValue(params.get("runId"));
        if (runId == null || runId.isBlank()) {
            return;
        }

        AgUiSessionRegistry sessionRegistry = exchange.getContext().getRegistry()
            .lookupByNameAndType("agUiSessionRegistry", AgUiSessionRegistry.class);
        if (sessionRegistry == null) {
            return;
        }

        AgUiSession session = sessionRegistry.get(runId);
        if (session == null) {
            return;
        }

        emitSportsToolLifecycle(session, runId);
        params.put("text", "San Francisco 49ers (49s) are winning 40:3 against the Dallas Cowboys.");
        params.put("messageId", UUID.randomUUID().toString());
    }

    private boolean isSportsIntent(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.toLowerCase();
        return normalized.contains("49ers")
            || normalized.contains("49s")
            || normalized.contains("cowboys")
            || normalized.contains("dallas")
            || normalized.contains("san francisco")
            || normalized.contains("score");
    }

    private void emitSportsToolLifecycle(AgUiSession session, String runId) {
        String sessionId = session.getSessionId();
        String toolCallId = UUID.randomUUID().toString();
        String toolMessageId = UUID.randomUUID().toString();

        String argsDelta = toJson(Map.of(
            "homeTeam", "San Francisco 49ers",
            "awayTeam", "Dallas Cowboys",
            "league", "NFL"));

        String resultContent = toJson(new HashMap<>(Map.of(
            "homeTeam", "San Francisco 49ers",
            "awayTeam", "Dallas Cowboys",
            "score", "40:3",
            "status", "final",
            "winner", "San Francisco 49ers")));

        session.emit(new AgUiToolCallStart(runId, sessionId, toolCallId, SPORTS_TOOL_NAME));
        session.emit(new AgUiToolCallArgs(runId, sessionId, toolCallId, argsDelta));
        session.emit(new AgUiToolCallResult(runId, sessionId, toolMessageId, toolCallId, resultContent, "tool"));
        session.emit(new AgUiToolCallEnd(runId, sessionId, toolCallId, SPORTS_TOOL_NAME));
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize sample sports payload", e);
        }
    }
}