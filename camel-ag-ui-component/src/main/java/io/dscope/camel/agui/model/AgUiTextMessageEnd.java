package io.dscope.camel.agui.model;

public class AgUiTextMessageEnd extends AgUiEvent {

    public AgUiTextMessageEnd(String runId, String sessionId) {
        super(AgUiEventTypes.TEXT_MESSAGE_END, runId, sessionId);
    }
}
