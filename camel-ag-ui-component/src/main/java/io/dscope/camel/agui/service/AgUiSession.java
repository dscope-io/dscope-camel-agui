package io.dscope.camel.agui.service;

import io.dscope.camel.agui.model.AgUiEvent;

public interface AgUiSession {

    String getRunId();

    String getSessionId();

    long emit(AgUiEvent event);

    void complete();

    void fail(Throwable error);
}
