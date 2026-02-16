package io.dscope.camel.agui.service;

import io.dscope.camel.agui.model.AgUiEvent;
import java.util.List;

public interface AgUiSessionRegistry {

    AgUiSession getOrCreate(String runId, String sessionId);

    AgUiSession get(String runId);

    List<AgUiSessionEventRecord> eventsSince(String runId, long afterSequence, int limit);

    int activeSessionCount();
}
