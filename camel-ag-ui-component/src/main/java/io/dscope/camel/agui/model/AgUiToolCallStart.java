package io.dscope.camel.agui.model;

public class AgUiToolCallStart extends AgUiEvent {

    public AgUiToolCallStart(String runId, String sessionId, String toolName) {
        super(AgUiEventTypes.TOOL_CALL_START, runId, sessionId);
        with("toolName", toolName);
    }
}
