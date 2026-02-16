package io.dscope.camel.agui.model;

public final class AgUiEventTypes {

    public static final String RUN_STARTED = "run.started";
    public static final String RUN_FINISHED = "run.finished";
    public static final String RUN_ERROR = "run.error";
    public static final String STEP_STARTED = "step.started";
    public static final String STEP_FINISHED = "step.finished";
    public static final String TEXT_MESSAGE_START = "text.message.start";
    public static final String TEXT_MESSAGE_CONTENT = "text.message.content";
    public static final String TEXT_MESSAGE_END = "text.message.end";
    public static final String TOOL_CALL_START = "tool.call.start";
    public static final String TOOL_CALL_ARGS = "tool.call.args";
    public static final String TOOL_CALL_END = "tool.call.end";
    public static final String TOOL_CALL_RESULT = "tool.call.result";
    public static final String STATE_SNAPSHOT = "state.snapshot";
    public static final String STATE_DELTA = "state.delta";
    public static final String INTERRUPT_REQUESTED = "interrupt.requested";
    public static final String RESUME_ACCEPTED = "resume.accepted";

    private AgUiEventTypes() {
    }
}
