package io.dscope.camel.agui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.dscope.camel.agui.model.AgUiTextMessageContent;
import org.junit.jupiter.api.Test;

class InMemoryAgUiSessionRegistryTest {

    @Test
    void rejectedEventDoesNotConsumeSequenceOrEnterHistory() {
        var registry = new InMemoryAgUiSessionRegistry(new JacksonAgUiEventCodec(true));
        var session = registry.getOrCreate("run-1", "session-1");
        assertThrows(IllegalArgumentException.class,
            () -> session.emit(new AgUiTextMessageContent("run-1", "session-1", "missing-id")));
        assertEquals(0, registry.eventsSince("run-1", 0, 10).size());
        assertEquals(1L, session.emit(new AgUiTextMessageContent("run-1", "session-1", "message", "text")));
    }

    @Test
    void retainsOnlyTheConfiguredEventWindow() {
        InMemoryAgUiSessionRegistry registry = new InMemoryAgUiSessionRegistry(new JacksonAgUiEventCodec(), 2);
        var session = registry.getOrCreate("run-1", "session-1");
        for (int index = 0; index < 3; index++) {
            session.emit(new AgUiTextMessageContent("run-1", "session-1", "message", "text"));
        }

        var events = registry.eventsSince("run-1", 0, 10);
        assertEquals(2, events.size());
        assertEquals(2L, events.get(0).sequence());
        assertEquals(3L, events.get(1).sequence());
    }

    @Test
    void rejectsNewSessionsBeyondTheConfiguredCapacity() {
        InMemoryAgUiSessionRegistry registry = new InMemoryAgUiSessionRegistry(new JacksonAgUiEventCodec(), 2, 1);
        registry.getOrCreate("run-1", "session-1");
        assertThrows(IllegalStateException.class, () -> registry.getOrCreate("run-2", "session-2"));
    }
}
