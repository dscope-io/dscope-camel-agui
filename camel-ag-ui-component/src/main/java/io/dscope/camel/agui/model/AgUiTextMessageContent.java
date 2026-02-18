package io.dscope.camel.agui.model;

public class AgUiTextMessageContent extends AgUiEvent {

    private String messageId;
    private String delta;

    public AgUiTextMessageContent(String runId, String sessionId, String text) {
        super(AgUiEventTypes.TEXT_MESSAGE_CONTENT, runId, sessionId);
        this.delta = text;
    }

    public AgUiTextMessageContent(String runId, String sessionId, String messageId, String delta) {
        this(runId, sessionId, delta);
        this.messageId = messageId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getDelta() {
        return delta;
    }

    public void setDelta(String delta) {
        this.delta = delta;
    }
}
