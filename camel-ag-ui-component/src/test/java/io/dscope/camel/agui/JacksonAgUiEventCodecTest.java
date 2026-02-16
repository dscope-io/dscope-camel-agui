package io.dscope.camel.agui;

import io.dscope.camel.agui.model.AgUiTextMessageContent;
import io.dscope.camel.agui.service.JacksonAgUiEventCodec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JacksonAgUiEventCodecTest {

    @Test
    void encodesEventJson() {
        JacksonAgUiEventCodec codec = new JacksonAgUiEventCodec();
        String json = codec.toJson(new AgUiTextMessageContent("run-1", "session-1", "hello"));
        Assertions.assertTrue(json.contains("text.message.content"));
        Assertions.assertTrue(json.contains("hello"));
    }
}
