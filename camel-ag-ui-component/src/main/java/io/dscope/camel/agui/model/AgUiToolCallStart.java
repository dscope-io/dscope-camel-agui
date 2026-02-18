package io.dscope.camel.agui.model;

public class AgUiToolCallStart extends AgUiEvent {

    private String toolCallId;
    private String toolCallName;

    public AgUiToolCallStart(String runId, String sessionId, String toolName) {
        super(AgUiEventTypes.TOOL_CALL_START, runId, sessionId);
        this.toolCallName = toolName;
    }

    public AgUiToolCallStart(String runId, String sessionId, String toolCallId, String toolCallName) {
        this(runId, sessionId, toolCallName);
        this.toolCallId = toolCallId;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public String getToolCallName() {
        return toolCallName;
    }

    public void setToolCallName(String toolCallName) {
        this.toolCallName = toolCallName;
    }
}
