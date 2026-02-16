package io.dscope.camel.agui.model;

public class AgUiRunStarted extends AgUiEvent {

    public AgUiRunStarted(String runId, String sessionId) {
        super(AgUiEventTypes.RUN_STARTED, runId, sessionId);
    }
}
