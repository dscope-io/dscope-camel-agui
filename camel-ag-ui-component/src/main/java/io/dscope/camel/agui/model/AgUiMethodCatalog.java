package io.dscope.camel.agui.model;

import io.dscope.camel.agui.config.AgUiProtocolMethods;
import java.util.Map;

public class AgUiMethodCatalog {

    public Map<String, String> supportedMethods() {
        return Map.of(
            AgUiProtocolMethods.RUN_START, "Start run and open stream session",
            AgUiProtocolMethods.RUN_TEXT, "Emit streaming text chunks",
            AgUiProtocolMethods.TOOL_CALL, "Emit tool call lifecycle",
            AgUiProtocolMethods.STATE_UPDATE, "Update and stream mutable state",
            AgUiProtocolMethods.INTERRUPT, "Request human-in-the-loop interrupt",
            AgUiProtocolMethods.RESUME, "Resume interrupted run",
            AgUiProtocolMethods.RUN_FINISH, "Complete run",
            AgUiProtocolMethods.HEALTH, "Runtime health check"
        );
    }
}
