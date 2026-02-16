package io.dscope.camel.agui.model;

public class AgUiStepStarted extends AgUiEvent {

    public AgUiStepStarted(String runId, String sessionId, String stepName) {
        super(AgUiEventTypes.STEP_STARTED, runId, sessionId);
        with("stepName", stepName);
    }
}
