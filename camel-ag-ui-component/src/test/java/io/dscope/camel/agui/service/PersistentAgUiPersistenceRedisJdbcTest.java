/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dscope.camel.agui.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dscope.camel.agui.model.AgUiRunStarted;
import io.dscope.camel.agui.model.AgUiTextMessageContent;
import io.dscope.camel.persistence.core.FlowStateStore;
import io.dscope.camel.persistence.core.FlowStateStoreFactory;
import io.dscope.camel.persistence.core.PersistenceConfiguration;
import io.dscope.camel.persistence.core.RehydrationPolicy;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

class PersistentAgUiPersistenceRedisJdbcTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void rehydratesFromJdbcWhenRedisLayerMisses() {
        String uri = redisUri();
        Assumptions.assumeTrue(isRedisReachable(uri), "Redis not reachable at " + uri);

        String runId = "run-" + UUID.randomUUID();
        String jdbcUrl = newJdbcUrl();

        FlowStateStore firstStore = newRedisJdbcStore(uri, randomPrefix(), jdbcUrl);
        JacksonAgUiEventCodec codec = new JacksonAgUiEventCodec();
        PersistentAgUiSessionRegistry first = new PersistentAgUiSessionRegistry(codec, firstStore, RehydrationPolicy.DEFAULT);
        AgUiSession session = first.getOrCreate(runId, "session-1");

        session.emit(new AgUiRunStarted(runId, session.getSessionId()));
        session.emit(new AgUiTextMessageContent(runId, session.getSessionId(), "hello-redis-jdbc"));

        FlowStateStore secondStore = newRedisJdbcStore(uri, randomPrefix(), jdbcUrl);
        PersistentAgUiSessionRegistry second = new PersistentAgUiSessionRegistry(codec, secondStore, RehydrationPolicy.DEFAULT);
        List<AgUiSessionEventRecord> restored = second.eventsSince(runId, 0L, 20);

        assertEquals(2, restored.size());
        assertEquals("RUN_STARTED", restored.get(0).eventType());
        assertTrue(restored.get(1).json().contains("hello-redis-jdbc"));
    }

    @Test
    void stateSnapshotRehydratesViaRedisJdbcBackend() {
        String uri = redisUri();
        Assumptions.assumeTrue(isRedisReachable(uri), "Redis not reachable at " + uri);

        String runId = "run-" + UUID.randomUUID();
        String jdbcUrl = newJdbcUrl();

        FlowStateStore firstStore = newRedisJdbcStore(uri, randomPrefix(), jdbcUrl);
        PersistentAgUiStateStore first = new PersistentAgUiStateStore(firstStore);
        JsonNode state = mapper.createObjectNode()
            .put("status", "running")
            .put("count", 11);
        first.put(runId, state);

        FlowStateStore secondStore = newRedisJdbcStore(uri, randomPrefix(), jdbcUrl);
        PersistentAgUiStateStore second = new PersistentAgUiStateStore(secondStore);
        JsonNode restored = second.get(runId);

        assertNotNull(restored);
        assertEquals("running", restored.path("status").asText());
        assertEquals(11, restored.path("count").asInt());
    }

    private FlowStateStore newRedisJdbcStore(String redisUri, String redisPrefix, String jdbcUrl) {
        Properties props = new Properties();
        props.setProperty("camel.persistence.enabled", "true");
        props.setProperty("camel.persistence.backend", "redis_jdbc");
        props.setProperty("camel.persistence.redis.uri", redisUri);
        props.setProperty("camel.persistence.redis.key-prefix", redisPrefix);
        props.setProperty("camel.persistence.jdbc.url", jdbcUrl);
        props.setProperty("camel.persistence.jdbc.user", "");
        props.setProperty("camel.persistence.jdbc.password", "");
        return FlowStateStoreFactory.create(PersistenceConfiguration.fromProperties(props));
    }

    private String redisUri() {
        return System.getProperty("camel.persistence.test.redis.uri", "redis://localhost:6379");
    }

    private String newJdbcUrl() {
        String dbName = "aguiRedisJdbc" + UUID.randomUUID().toString().replace("-", "");
        return "jdbc:derby:memory:" + dbName + ";create=true";
    }

    private String randomPrefix() {
        return "camel:agui:test:redis_jdbc:" + UUID.randomUUID().toString().replace("-", "");
    }

    private boolean isRedisReachable(String uri) {
        try (JedisPool pool = new JedisPool(uri); Jedis jedis = pool.getResource()) {
            return "PONG".equalsIgnoreCase(jedis.ping());
        } catch (Exception ignored) {
            return false;
        }
    }
}
