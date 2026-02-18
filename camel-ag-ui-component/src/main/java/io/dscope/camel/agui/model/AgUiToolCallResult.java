package io.dscope.camel.agui.model;

public class AgUiToolCallResult extends AgUiEvent {

    private String messageId;
    private String toolCallId;
    private String content;
    private String role;

    public AgUiToolCallResult(String runId, String sessionId, Object result) {
        super(AgUiEventTypes.TOOL_CALL_RESULT, runId, sessionId);
        this.content = result == null ? "" : result.toString();
    }

    public AgUiToolCallResult(String runId, String sessionId, String messageId, String toolCallId, String content, String role) {
        super(AgUiEventTypes.TOOL_CALL_RESULT, runId, sessionId);
        this.messageId = messageId;
        this.toolCallId = toolCallId;
        this.content = content;
        this.role = role;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
