package io.dscope.camel.agui.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dscope.camel.agui.model.AgUiEvent;

public class JacksonAgUiEventCodec implements AgUiEventCodec {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public String toJson(AgUiEvent event) {
        try {
            return mapper.writeValueAsString(event);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encode AG-UI event", e);
        }
    }
}
