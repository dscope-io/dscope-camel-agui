package io.dscope.camel.agui.model;

public class AgUiToolCallEnd extends AgUiEvent {

    private String toolCallId;

    public AgUiToolCallEnd(String runId, String sessionId, String toolName) {
        super(AgUiEventTypes.TOOL_CALL_END, runId, sessionId);
    }

    public AgUiToolCallEnd(String runId, String sessionId, String toolCallId, String toolCallName) {
        this(runId, sessionId, toolCallName);
        this.toolCallId = toolCallId;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }
}
