package io.dscope.camel.agui.model;

public class AgUiStateDelta extends AgUiEvent {

    public AgUiStateDelta(String runId, String sessionId, Object patch) {
        super(AgUiEventTypes.STATE_DELTA, runId, sessionId);
        with("patch", patch);
    }
}
