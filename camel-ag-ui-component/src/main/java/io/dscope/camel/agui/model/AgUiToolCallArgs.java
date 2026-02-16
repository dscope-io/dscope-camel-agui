package io.dscope.camel.agui.model;

import java.util.Map;

public class AgUiToolCallArgs extends AgUiEvent {

    public AgUiToolCallArgs(String runId, String sessionId, Map<String, Object> args) {
        super(AgUiEventTypes.TOOL_CALL_ARGS, runId, sessionId);
        with("args", args);
    }
}
