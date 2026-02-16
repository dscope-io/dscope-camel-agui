package io.dscope.camel.agui.model;

public class AgUiToolCallResult extends AgUiEvent {

    public AgUiToolCallResult(String runId, String sessionId, Object result) {
        super(AgUiEventTypes.TOOL_CALL_RESULT, runId, sessionId);
        with("result", result);
    }
}
