package io.dscope.camel.agui.model;

public class AgUiStepFinished extends AgUiEvent {

    private String stepName;

    public AgUiStepFinished(String runId, String sessionId, String stepName) {
        super(AgUiEventTypes.STEP_FINISHED, runId, sessionId);
        this.stepName = stepName;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }
}
