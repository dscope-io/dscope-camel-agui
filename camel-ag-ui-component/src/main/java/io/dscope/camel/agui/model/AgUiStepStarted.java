package io.dscope.camel.agui.model;

public class AgUiStepStarted extends AgUiEvent {

    private String stepName;

    public AgUiStepStarted(String runId, String sessionId, String stepName) {
        super(AgUiEventTypes.STEP_STARTED, runId, sessionId);
        this.stepName = stepName;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }
}
