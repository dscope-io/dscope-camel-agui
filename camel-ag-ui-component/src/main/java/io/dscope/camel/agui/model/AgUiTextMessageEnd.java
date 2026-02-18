package io.dscope.camel.agui.model;

public class AgUiTextMessageEnd extends AgUiEvent {

    private String messageId;

    public AgUiTextMessageEnd(String runId, String sessionId) {
        super(AgUiEventTypes.TEXT_MESSAGE_END, runId, sessionId);
    }

    public AgUiTextMessageEnd(String runId, String sessionId, String messageId) {
        this(runId, sessionId);
        this.messageId = messageId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
