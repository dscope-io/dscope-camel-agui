package io.dscope.camel.agui.model;

public class AgUiTextMessageStart extends AgUiEvent {

    private String messageId;
    private String role;

    public AgUiTextMessageStart(String runId, String sessionId) {
        super(AgUiEventTypes.TEXT_MESSAGE_START, runId, sessionId);
        this.role = "assistant";
    }

    public AgUiTextMessageStart(String runId, String sessionId, String messageId, String role) {
        this(runId, sessionId);
        this.messageId = messageId;
        this.role = role;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
