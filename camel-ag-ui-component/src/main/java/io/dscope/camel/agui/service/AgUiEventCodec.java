package io.dscope.camel.agui.service;

import io.dscope.camel.agui.model.AgUiEvent;

public interface AgUiEventCodec {

    String toJson(AgUiEvent event);
}
