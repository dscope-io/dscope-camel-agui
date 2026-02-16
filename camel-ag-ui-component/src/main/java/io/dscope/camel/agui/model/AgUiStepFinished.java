package io.dscope.camel.agui.model;

public class AgUiStepFinished extends AgUiEvent {

    public AgUiStepFinished(String runId, String sessionId, String stepName) {
        super(AgUiEventTypes.STEP_FINISHED, runId, sessionId);
        with("stepName", stepName);
    }
}
