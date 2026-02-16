package io.dscope.camel.agui.model;

public class AgUiRunError extends AgUiEvent {

    public AgUiRunError(String runId, String sessionId, String error) {
        super(AgUiEventTypes.RUN_ERROR, runId, sessionId);
        with("error", error);
    }
}
