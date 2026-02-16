package io.dscope.camel.agui.model;

public class AgUiToolCallEnd extends AgUiEvent {

    public AgUiToolCallEnd(String runId, String sessionId, String toolName) {
        super(AgUiEventTypes.TOOL_CALL_END, runId, sessionId);
        with("toolName", toolName);
    }
}
