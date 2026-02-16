package io.dscope.camel.agui.config;

import java.util.Set;

public final class AgUiProtocolMethods {

    public static final String RUN_START = "run.start";
    public static final String RUN_TEXT = "run.text";
    public static final String TOOL_CALL = "tool.call";
    public static final String STATE_UPDATE = "state.update";
    public static final String INTERRUPT = "run.interrupt";
    public static final String RESUME = "run.resume";
    public static final String RUN_FINISH = "run.finish";
    public static final String HEALTH = "health";

    public static final Set<String> CORE_METHODS = Set.of(
        RUN_START,
        RUN_TEXT,
        TOOL_CALL,
        STATE_UPDATE,
        INTERRUPT,
        RESUME,
        RUN_FINISH,
        HEALTH
    );

    private AgUiProtocolMethods() {
    }
}
