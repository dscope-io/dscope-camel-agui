package io.dscope.camel.agui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dscope.camel.agui.model.AgUiRunStarted;
import io.dscope.camel.agui.model.AgUiTextMessageContent;
import io.dscope.camel.persistence.core.RehydrationPolicy;
import io.dscope.camel.persistence.jdbc.JdbcFlowStateStore;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PersistentAgUiPersistenceJdbcTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void sessionEventsRehydrateAcrossRegistryInstances() {
        JdbcFlowStateStore store = newJdbcStore();
        JacksonAgUiEventCodec codec = new JacksonAgUiEventCodec();

        String runId = "run-" + UUID.randomUUID();
        PersistentAgUiSessionRegistry first = new PersistentAgUiSessionRegistry(codec, store, RehydrationPolicy.DEFAULT);
        AgUiSession session = first.getOrCreate(runId, "session-1");

        session.emit(new AgUiRunStarted(runId, session.getSessionId()));
        session.emit(new AgUiTextMessageContent(runId, session.getSessionId(), "hello-jdbc"));

        List<AgUiSessionEventRecord> emitted = first.eventsSince(runId, 0L, 20);
        assertEquals(2, emitted.size());

        PersistentAgUiSessionRegistry second = new PersistentAgUiSessionRegistry(codec, store, RehydrationPolicy.DEFAULT);
        List<AgUiSessionEventRecord> restored = second.eventsSince(runId, 0L, 20);

        assertEquals(2, restored.size());
        assertEquals(emitted.get(0).eventType(), restored.get(0).eventType());
        assertTrue(restored.get(1).json().contains("hello-jdbc"));
    }

    @Test
    void stateStoreRoundTripAcrossInstances() {
        JdbcFlowStateStore store = newJdbcStore();
        String runId = "run-" + UUID.randomUUID();

        PersistentAgUiStateStore first = new PersistentAgUiStateStore(store);
        JsonNode state = mapper.createObjectNode()
            .put("status", "running")
            .put("count", 3);
        first.put(runId, state);

        PersistentAgUiStateStore second = new PersistentAgUiStateStore(store);
        JsonNode restored = second.get(runId);

        assertNotNull(restored);
        assertEquals("running", restored.path("status").asText());
        assertEquals(3, restored.path("count").asInt());
    }

    private JdbcFlowStateStore newJdbcStore() {
        String dbName = "aguiPersistence" + UUID.randomUUID().toString().replace("-", "");
        return new JdbcFlowStateStore("jdbc:derby:memory:" + dbName + ";create=true", "", "");
    }
}
