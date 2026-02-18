package io.dscope.camel.agui.processor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.dscope.camel.agui.config.AgUiExchangeProperties;

public class AgUiAgentEnvelopeProcessor implements Processor {

    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() { };
    private static final Set<String> CORE_METHODS = Set.of(
        "run.start",
        "run.text",
        "tool.call",
        "state.update",
        "run.interrupt",
        "run.resume",
        "run.finish",
        "health"
    );

    @Override
    public void process(Exchange exchange) throws Exception {
        Map<String, Object> envelope = readEnvelope(exchange.getIn());
        exchange.setProperty(AgUiExchangeProperties.RAW_PAYLOAD, envelope);

        boolean explicitMethod = hasExplicitMethod(envelope);
        exchange.setProperty("agui.agent.explicit.method", explicitMethod);

        String method = detectMethod(envelope);
        Map<String, Object> params = deriveParams(envelope);
        Object requestId = envelope.get("id");

        exchange.setProperty(AgUiExchangeProperties.ENVELOPE_TYPE, requestId == null ? "notification" : "request");
        exchange.setProperty(AgUiExchangeProperties.REQUEST_ID, requestId);
        exchange.setProperty(AgUiExchangeProperties.METHOD, method);
        exchange.setProperty(AgUiExchangeProperties.PARAMS, params);

        if (explicitMethod && !CORE_METHODS.contains(method)) {
            throw new AgUiMethodNotFoundException("Method not found: " + method);
        }
        exchange.getMessage().setBody(params);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readEnvelope(Message message) throws Exception {
        Object body = message.getBody();
        if (body instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        String json = message.getBody(String.class);
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Request body must be a JSON object");
        }
        return MAPPER.readValue(json, MAP_TYPE);
    }

    @SuppressWarnings("unchecked")
    private String detectMethod(Map<String, Object> envelope) {
        String method = asString(envelope.get("method"));
        if (method == null || method.isBlank()) {
            method = asString(firstPresent(envelope, "type", "event", "action", "operation", "op", "name"));
        }
        method = normalizeMethod(method);
        if (method != null) {
            return method;
        }

        if (findText(envelope) != null) {
            return "run.text";
        }
        if (envelope.get("state") instanceof Map<?, ?>) {
            return "state.update";
        }
        if (envelope.get("tool") != null || envelope.get("toolCall") != null) {
            return "tool.call";
        }
        Object status = envelope.get("status");
        if (status != null) {
            String normalizedStatus = status.toString().toLowerCase();
            if (normalizedStatus.equals("finished") || normalizedStatus.equals("completed") || normalizedStatus.equals("done")) {
                return "run.finish";
            }
        }
        if (Boolean.TRUE.equals(envelope.get("finished"))) {
            return "run.finish";
        }

        if (envelope.get("runId") != null || envelope.get("sessionId") != null || envelope.get("run") instanceof Map<?, ?>) {
            return "run.start";
        }

        return "run.start";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deriveParams(Map<String, Object> envelope) {
        Map<String, Object> params;
        Object rawParams = envelope.get("params");
        if (rawParams instanceof Map<?, ?> map) {
            params = new LinkedHashMap<>((Map<String, Object>) map);
        } else {
            params = new LinkedHashMap<>(envelope);
            params.remove("jsonrpc");
            params.remove("id");
            params.remove("method");
            params.remove("type");
            params.remove("event");
            params.remove("action");
            params.remove("operation");
            params.remove("op");
            params.remove("name");
        }

        Object runObj = envelope.get("run");
        if (runObj instanceof Map<?, ?> run) {
            if (!params.containsKey("runId")) {
                Object runId = run.get("runId");
                if (runId == null) {
                    runId = run.get("id");
                }
                if (runId != null) {
                    params.put("runId", runId.toString());
                }
            }
            if (!params.containsKey("sessionId")) {
                Object sessionId = run.get("sessionId");
                if (sessionId == null) {
                    sessionId = run.get("session");
                }
                if (sessionId != null) {
                    params.put("sessionId", sessionId.toString());
                }
            }
        }

        if (!params.containsKey("runId") && envelope.get("runId") != null) {
            params.put("runId", envelope.get("runId").toString());
        }
        if (!params.containsKey("sessionId") && envelope.get("sessionId") != null) {
            params.put("sessionId", envelope.get("sessionId").toString());
        }

        Object threadId = firstPresent(envelope, "threadId", "thread", "conversationId", "contextId");
        if (threadId != null && !params.containsKey("threadId")) {
            params.put("threadId", threadId.toString());
        }
        if (!params.containsKey("sessionId") && params.get("threadId") != null) {
            params.put("sessionId", params.get("threadId").toString());
        }
        if (!params.containsKey("threadId") && params.get("sessionId") != null) {
            params.put("threadId", params.get("sessionId").toString());
        }

        if (!params.containsKey("text")) {
            String text = findText(envelope);
            if (text != null && !text.isBlank()) {
                params.put("text", text);
            }
        }

        return params;
    }

    private boolean hasExplicitMethod(Map<String, Object> envelope) {
        return firstPresent(envelope, "method", "type", "event", "action", "operation", "op", "name") != null;
    }

    @SuppressWarnings("unchecked")
    private String findText(Object node) {
        if (node == null) {
            return null;
        }
        if (node instanceof String text) {
            String trimmed = text.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }
        if (node instanceof Map<?, ?> map) {
            Object direct = firstPresent((Map<String, Object>) map,
                "text", "content", "input", "message", "prompt", "query", "value");
            String directText = findText(direct);
            if (directText != null) {
                return directText;
            }
            String messagesText = findText(map.get("messages"));
            if (messagesText != null) {
                return messagesText;
            }
            String partsText = findText(map.get("parts"));
            if (partsText != null) {
                return partsText;
            }
            return findText(map.get("data"));
        }
        if (node instanceof List<?> list) {
            for (int index = list.size() - 1; index >= 0; index--) {
                String text = findText(list.get(index));
                if (text != null) {
                    return text;
                }
            }
            return null;
        }
        return null;
    }

    private String normalizeMethod(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase().replace('_', '.').replace('-', '.').replace(' ', '.');

        return switch (normalized) {
            case "run.start", "run.started", "start" -> "run.start";
            case "run.text", "text", "message", "text.message" -> "run.text";
            case "tool.call", "tool", "toolcall" -> "tool.call";
            case "state.update", "state" -> "state.update";
            case "run.interrupt", "interrupt" -> "run.interrupt";
            case "run.resume", "resume" -> "run.resume";
            case "run.finish", "finish", "finished", "complete", "completed", "done" -> "run.finish";
            case "health" -> "health";
            default -> value;
        };
    }

    private Object firstPresent(Map<String, Object> envelope, String... keys) {
        for (String key : keys) {
            Object value = envelope.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}