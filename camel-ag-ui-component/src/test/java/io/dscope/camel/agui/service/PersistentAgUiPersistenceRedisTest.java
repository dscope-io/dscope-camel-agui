package io.dscope.camel.agui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dscope.camel.agui.model.AgUiRunStarted;
import io.dscope.camel.agui.model.AgUiTextMessageContent;
import io.dscope.camel.persistence.core.RehydrationPolicy;
import io.dscope.camel.persistence.redis.RedisFlowStateStore;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

class PersistentAgUiPersistenceRedisTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void sessionEventsRehydrateAcrossRegistryInstances() {
        String uri = redisUri();
        Assumptions.assumeTrue(isRedisReachable(uri), "Redis not reachable at " + uri);

        RedisFlowStateStore store = newRedisStore(uri);
        JacksonAgUiEventCodec codec = new JacksonAgUiEventCodec();
        String runId = "run-" + UUID.randomUUID();

        PersistentAgUiSessionRegistry first = new PersistentAgUiSessionRegistry(codec, store, RehydrationPolicy.DEFAULT);
        AgUiSession session = first.getOrCreate(runId, "session-1");

        session.emit(new AgUiRunStarted(runId, session.getSessionId()));
        session.emit(new AgUiTextMessageContent(runId, session.getSessionId(), "hello-redis"));

        List<AgUiSessionEventRecord> emitted = first.eventsSince(runId, 0L, 20);
        assertEquals(2, emitted.size());

        PersistentAgUiSessionRegistry second = new PersistentAgUiSessionRegistry(codec, store, RehydrationPolicy.DEFAULT);
        List<AgUiSessionEventRecord> restored = second.eventsSince(runId, 0L, 20);

        assertEquals(2, restored.size());
        assertEquals(emitted.get(0).eventType(), restored.get(0).eventType());
        assertTrue(restored.get(1).json().contains("hello-redis"));
    }

    @Test
    void stateStoreRoundTripAcrossInstances() {
        String uri = redisUri();
        Assumptions.assumeTrue(isRedisReachable(uri), "Redis not reachable at " + uri);

        RedisFlowStateStore store = newRedisStore(uri);
        String runId = "run-" + UUID.randomUUID();

        PersistentAgUiStateStore first = new PersistentAgUiStateStore(store);
        JsonNode state = mapper.createObjectNode()
            .put("status", "running")
            .put("count", 7);
        first.put(runId, state);

        PersistentAgUiStateStore second = new PersistentAgUiStateStore(store);
        JsonNode restored = second.get(runId);

        assertNotNull(restored);
        assertEquals("running", restored.path("status").asText());
        assertEquals(7, restored.path("count").asInt());
    }

    private RedisFlowStateStore newRedisStore(String uri) {
        String prefix = "camel:agui:test:" + UUID.randomUUID().toString().replace("-", "");
        return new RedisFlowStateStore(uri, prefix);
    }

    private String redisUri() {
        return System.getProperty("camel.persistence.test.redis.uri", "redis://localhost:6379");
    }

    private boolean isRedisReachable(String uri) {
        try (JedisPool pool = new JedisPool(uri); Jedis jedis = pool.getResource()) {
            return "PONG".equalsIgnoreCase(jedis.ping());
        } catch (Exception ignored) {
            return false;
        }
    }
}
