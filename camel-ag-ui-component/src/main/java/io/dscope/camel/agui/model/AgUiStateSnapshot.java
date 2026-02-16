package io.dscope.camel.agui.model;

public class AgUiStateSnapshot extends AgUiEvent {

    public AgUiStateSnapshot(String runId, String sessionId, Object state) {
        super(AgUiEventTypes.STATE_SNAPSHOT, runId, sessionId);
        with("state", state);
    }
}
