package io.dscope.camel.agui.model;

import java.util.Map;

public class AgUiToolCallArgs extends AgUiEvent {

    private String toolCallId;
    private String delta;

    public AgUiToolCallArgs(String runId, String sessionId, Map<String, Object> args) {
        super(AgUiEventTypes.TOOL_CALL_ARGS, runId, sessionId);
        this.delta = String.valueOf(args);
    }

    public AgUiToolCallArgs(String runId, String sessionId, String toolCallId, String delta) {
        super(AgUiEventTypes.TOOL_CALL_ARGS, runId, sessionId);
        this.toolCallId = toolCallId;
        this.delta = delta;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public String getDelta() {
        return delta;
    }

    public void setDelta(String delta) {
        this.delta = delta;
    }
}
