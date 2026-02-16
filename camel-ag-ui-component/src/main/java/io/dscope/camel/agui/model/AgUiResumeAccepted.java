package io.dscope.camel.agui.model;

public class AgUiResumeAccepted extends AgUiEvent {

    public AgUiResumeAccepted(String runId, String sessionId) {
        super(AgUiEventTypes.RESUME_ACCEPTED, runId, sessionId);
    }
}
