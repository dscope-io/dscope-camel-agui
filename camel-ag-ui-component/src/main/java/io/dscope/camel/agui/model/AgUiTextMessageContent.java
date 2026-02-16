package io.dscope.camel.agui.model;

public class AgUiTextMessageContent extends AgUiEvent {

    public AgUiTextMessageContent(String runId, String sessionId, String text) {
        super(AgUiEventTypes.TEXT_MESSAGE_CONTENT, runId, sessionId);
        with("text", text);
    }
}
