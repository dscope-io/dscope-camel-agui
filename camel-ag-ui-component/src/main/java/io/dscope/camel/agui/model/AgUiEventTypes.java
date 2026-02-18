package io.dscope.camel.agui.model;

public final class AgUiEventTypes {

    public static final String RUN_STARTED = "RUN_STARTED";
    public static final String RUN_FINISHED = "RUN_FINISHED";
    public static final String RUN_ERROR = "RUN_ERROR";
    public static final String STEP_STARTED = "STEP_STARTED";
    public static final String STEP_FINISHED = "STEP_FINISHED";
    public static final String TEXT_MESSAGE_START = "TEXT_MESSAGE_START";
    public static final String TEXT_MESSAGE_CONTENT = "TEXT_MESSAGE_CONTENT";
    public static final String TEXT_MESSAGE_END = "TEXT_MESSAGE_END";
    public static final String TOOL_CALL_START = "TOOL_CALL_START";
    public static final String TOOL_CALL_ARGS = "TOOL_CALL_ARGS";
    public static final String TOOL_CALL_END = "TOOL_CALL_END";
    public static final String TOOL_CALL_RESULT = "TOOL_CALL_RESULT";
    public static final String STATE_SNAPSHOT = "STATE_SNAPSHOT";
    public static final String STATE_DELTA = "STATE_DELTA";
    public static final String INTERRUPT_REQUESTED = "INTERRUPT_REQUESTED";
    public static final String RESUME_ACCEPTED = "RESUME_ACCEPTED";

    private AgUiEventTypes() {
    }
}
