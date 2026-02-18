/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dscope.camel.agui.samples;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.reflect.Method;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.camel.Exchange;

import io.dscope.camel.agui.processor.AbstractAgUiAgentWidgetProcessor;
import io.dscope.camel.agui.model.AgUiToolCallArgs;
import io.dscope.camel.agui.model.AgUiToolCallEnd;
import io.dscope.camel.agui.model.AgUiToolCallResult;
import io.dscope.camel.agui.model.AgUiToolCallStart;
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

        AgUiToolCallStart start = new AgUiToolCallStart(runId, sessionId, WEATHER_TOOL_NAME);
        applyIfPresent(start, "setToolCallId", String.class, toolCallId);
        session.emit(start);

        AgUiToolCallArgs args = new AgUiToolCallArgs(runId, sessionId, Map.of("city", city, "unit", "C"));
        applyIfPresent(args, "setToolCallId", String.class, toolCallId);
        applyIfPresent(args, "setDelta", String.class, argsDelta);
        session.emit(args);

        AgUiToolCallResult result = new AgUiToolCallResult(runId, sessionId, resultContent);
        applyIfPresent(result, "setMessageId", String.class, toolMessageId);
        applyIfPresent(result, "setToolCallId", String.class, toolCallId);
        applyIfPresent(result, "setContent", String.class, resultContent);
        applyIfPresent(result, "setRole", String.class, "tool");
        session.emit(result);

        AgUiToolCallEnd end = new AgUiToolCallEnd(runId, sessionId, WEATHER_TOOL_NAME);
        applyIfPresent(end, "setToolCallId", String.class, toolCallId);
        session.emit(end);
    }

    private static void applyIfPresent(Object target, String method, Class<?> argType, Object value) {
        try {
            Method setter = target.getClass().getMethod(method, argType);
            setter.invoke(target, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize sample weather payload", e);
        }
    }
}