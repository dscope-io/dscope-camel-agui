package io.dscope.camel.agui.model;

public class AgUiRunFinished extends AgUiEvent {

    public AgUiRunFinished(String runId, String sessionId) {
        super(AgUiEventTypes.RUN_FINISHED, runId, sessionId);
    }
}
