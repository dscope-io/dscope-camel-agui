package io.dscope.camel.agui.model;

public class AgUiInterruptRequested extends AgUiEvent {

    public AgUiInterruptRequested(String runId, String sessionId, String reason) {
        super(AgUiEventTypes.INTERRUPT_REQUESTED, runId, sessionId);
        with("reason", reason);
    }
}
