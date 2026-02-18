package io.dscope.camel.agui.samples;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.camel.Exchange;

import io.dscope.camel.agui.model.AgUiToolCallArgs;
import io.dscope.camel.agui.model.AgUiToolCallEnd;
import io.dscope.camel.agui.model.AgUiToolCallResult;
import io.dscope.camel.agui.model.AgUiToolCallStart;
import io.dscope.camel.agui.processor.AbstractAgUiAgentWidgetProcessor;
import io.dscope.camel.agui.service.AgUiSession;
import io.dscope.camel.agui.service.AgUiSessionRegistry;

final class AgUiWeatherWidgetProcessor extends AbstractAgUiAgentWidgetProcessor {

    private static final Pattern WEATHER_CITY_PATTERN = Pattern.compile("(?i)\\bweather\\b(?:\\s+(?:in|at|for))?\\s+([a-zA-Z][a-zA-Z\\s-]{1,40})");
    private static final String WEATHER_TOOL_NAME = "get_weather";
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Override
    protected void process(Exchange exchange, Map<String, Object> params) {
        String text = stringValue(params.get("text"));
        if (!isWeatherIntent(text)) {
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

        emitWeatherToolLifecycle(session, runId, text);
        String city = detectCity(text);
        params.put("text", "Weather in " + city + ": 18C and Cloudy.");
        params.put("messageId", UUID.randomUUID().toString());
    }

    private boolean isWeatherIntent(String text) {
        return text != null && text.toLowerCase().contains("weather");
    }

    private String detectCity(String text) {
        if (text == null || text.isBlank()) {
            return "Berlin";
        }
        Matcher matcher = WEATHER_CITY_PATTERN.matcher(text.trim());
        if (matcher.find()) {
            String city = matcher.group(1).replaceAll("[?.!,;:]+$", "").trim();
            if (!city.isBlank()) {
                return city;
            }
        }
        return "Berlin";
    }

    private void emitWeatherToolLifecycle(AgUiSession session, String runId, String text) {
        String sessionId = session.getSessionId();
        String city = detectCity(text);

        String toolCallId = UUID.randomUUID().toString();
        String toolMessageId = UUID.randomUUID().toString();
        String argsDelta = toJson(Map.of("city", city, "unit", "C"));
        String resultContent = toJson(new HashMap<>(Map.of("city", city, "tempC", 18, "condition", "Cloudy")));

        session.emit(new AgUiToolCallStart(runId, sessionId, toolCallId, WEATHER_TOOL_NAME));
        session.emit(new AgUiToolCallArgs(runId, sessionId, toolCallId, argsDelta));
        session.emit(new AgUiToolCallResult(runId, sessionId, toolMessageId, toolCallId, resultContent, "tool"));
        session.emit(new AgUiToolCallEnd(runId, sessionId, toolCallId, WEATHER_TOOL_NAME));
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize sample weather payload", e);
        }
    }
}