package io.dscope.camel.agui.model;

public class AgUiTextMessageStart extends AgUiEvent {

    public AgUiTextMessageStart(String runId, String sessionId) {
        super(AgUiEventTypes.TEXT_MESSAGE_START, runId, sessionId);
    }
}
