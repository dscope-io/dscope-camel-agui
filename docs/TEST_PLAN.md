# TEST PLAN

## Automated Commands

```bash
mvn test
mvn -pl samples/ag-ui-yaml-service -am test

# Persistence unit tests (JDBC, embedded Derby)
mvn -pl camel-ag-ui-component -Dtest=PersistentAgUiPersistenceJdbcTest test

# Persistence unit tests (Redis)
mvn -pl camel-ag-ui-component -Dtest=PersistentAgUiPersistenceRedisTest -Dcamel.persistence.test.redis.uri=redis://localhost:6379 test
```

Redis test behavior:

- If Redis is reachable, Redis persistence tests execute.
- If Redis is not reachable, Redis persistence tests are skipped.

## Manual Verification

Configuration defaults come from `samples/ag-ui-yaml-service/src/main/resources/application.yaml`; runtime `-D...` system properties take precedence when both are set.

Example override: `mvn -DskipTests compile exec:java -Dagui.health.port=9080 -Dagui.rpc.port=9080`

Start sample service:

```bash
cd samples/ag-ui-yaml-service
mvn -DskipTests compile exec:java
```

### 1. Health and diagnostics

```bash
curl -s http://localhost:8080/health
curl -s http://localhost:8080/diagnostics
```

### 2. Start run

```bash
curl -s http://localhost:8080/agui/rpc \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":"1","method":"run.start","params":{"runId":"demo-run","sessionId":"demo-session"}}'
```

### 3. Emit text

```bash
curl -s http://localhost:8080/agui/rpc \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":"2","method":"run.text","params":{"runId":"demo-run","text":"hello from ag-ui"}}'
```

### 4. Update state

```bash
curl -s http://localhost:8080/agui/rpc \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":"3","method":"state.update","params":{"runId":"demo-run","state":{"cart":["widget-a"]}}}'
```

### 5. Finish run

```bash
curl -s http://localhost:8080/agui/rpc \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":"4","method":"run.finish","params":{"runId":"demo-run"}}'
```

### 6. Read stream

```bash
curl -s "http://localhost:8080/agui/stream/demo-run?afterSequence=0&limit=100"
```

Expected: ordered SSE event records for lifecycle, text, tool/state updates, and run completion.

### 7. Persistence Sanity (JDBC/Redis)

Recommended startup command for Redis mode:

```bash
chmod +x samples/ag-ui-yaml-service/scripts/run-with-redis-persistence.sh
./samples/ag-ui-yaml-service/scripts/run-with-redis-persistence.sh
```

Optional Redis URI override:

```bash
CAMEL_PERSISTENCE_REDIS_URI=redis://localhost:6379 \
./samples/ag-ui-yaml-service/scripts/run-with-redis-persistence.sh
```

Note: `run-with-redis-persistence.sh` includes a Redis connectivity preflight check and fails fast with a clear message if Redis is unreachable.

1. Enable persistence in runtime with `-Dcamel.persistence.enabled=true`.
2. Set backend:
   - JDBC: `-Dcamel.persistence.backend=jdbc -Dcamel.persistence.jdbc.url=jdbc:derby:memory:agui;create=true`
   - Redis: `-Dcamel.persistence.backend=redis -Dcamel.persistence.redis.uri=redis://localhost:6379`
3. Start run and emit text/state updates.
4. Restart runtime process with same backend settings.
5. Query stream for same `runId`.

Expected:

- persisted events are still available after restart
- stream returns previously emitted events in sequence order
- no protocol schema changes in responses

### 8. Optional WebSocket Scaffold

Start with WS enabled:

```bash
cd samples/ag-ui-yaml-service
mvn -DskipTests compile exec:java -Dagui.websocket.enabled=true -Dagui.websocket.path=/agui/ws
```

Expected: runtime starts successfully and exposes `ws://localhost:8080/agui/ws`.

### 9. Scripted HTTP Smoke Test

Run the end-to-end HTTP verification script (starts/stops the sample runtime automatically):

```bash
chmod +x samples/ag-ui-yaml-service/scripts/http-curl-smoke-test.sh
./samples/ag-ui-yaml-service/scripts/http-curl-smoke-test.sh
```

Optional overrides:

```bash
AGUI_HEALTH_PORT=8080 \
AGUI_RPC_PORT=8080 \
AGUI_RUN_ID=demo-run \
AGUI_SESSION_ID=demo-session \
AGUI_TEXT_PAYLOAD='hello curl' \
./samples/ag-ui-yaml-service/scripts/http-curl-smoke-test.sh
```

Pass criteria:

- health endpoint returns HTTP 200
- `run.start`, `run.text`, and `run.finish` return HTTP 200
- stream endpoint returns HTTP 200 with `run.started`, `text.message.content`, and `run.finished`
- unknown RPC method returns HTTP 400 with JSON-RPC error code `-32601`

### 10. Scripted Redis HTTP Smoke Test

Run end-to-end HTTP verification with Redis persistence enabled:

```bash
chmod +x samples/ag-ui-yaml-service/scripts/http-curl-smoke-test-redis.sh
./samples/ag-ui-yaml-service/scripts/http-curl-smoke-test-redis.sh
```

Optional Redis URI override:

```bash
CAMEL_PERSISTENCE_REDIS_URI=redis://localhost:6379 \
./samples/ag-ui-yaml-service/scripts/http-curl-smoke-test-redis.sh
```

Note: `http-curl-smoke-test-redis.sh` includes the same Redis connectivity preflight check and exits before startup when Redis is unreachable.

Pass criteria are the same as the scripted HTTP smoke test, with persistence backend set to Redis.

### 11. Redis Troubleshooting

If Redis preflight fails, verify connectivity first:

```bash
# If redis-cli is installed
redis-cli -u redis://localhost:6379 ping

# TCP probe fallback
nc -z localhost 6379 && echo "redis port open" || echo "redis port closed"
```

If using a non-default Redis endpoint, set it explicitly before rerunning scripts:

```bash
CAMEL_PERSISTENCE_REDIS_URI=redis://<host>:6379 ./samples/ag-ui-yaml-service/scripts/run-with-redis-persistence.sh
CAMEL_PERSISTENCE_REDIS_URI=redis://<host>:6379 ./samples/ag-ui-yaml-service/scripts/http-curl-smoke-test-redis.sh
```

### 12. Browser Visualization

Open the built-in sample viewer in your browser:

```bash
open http://localhost:8080/agui/ui

```

From that page you can:

- call `run.start`, `run.text`, `run.finish`
- connect SSE for a chosen `runId`
- inspect RPC responses and streamed events in real time

### 13. Single-Endpoint POST+SSE Transport

Verify Dojo-style transport compatibility (`POST /agui/agent` returns SSE in one call):

```bash
curl -N -X POST http://localhost:8080/agui/agent \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":"1","method":"run.start","params":{"runId":"post-sse-run","sessionId":"post-sse-session"}}'
```

Expected:

- HTTP 200
- response `Content-Type` is `text/event-stream`
- response includes at least `event: run.started` and `event: step.started`

Negative case:

```bash
curl -s -X POST http://localhost:8080/agui/agent \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":"2","method":"unknown.method","params":{}}'
```

Expected:

- HTTP 400
- JSON-RPC error payload with code `-32601`

### 14. Scripted POST+SSE Smoke Test

Run end-to-end validation for single-endpoint POST+SSE transport (`/agui/agent`):

```bash
chmod +x samples/ag-ui-yaml-service/scripts/http-post-sse-smoke-test.sh
./samples/ag-ui-yaml-service/scripts/http-post-sse-smoke-test.sh
```

Optional overrides:

```bash
AGUI_PORT=8080 \
AGUI_RUN_ID=post-sse-run \
AGUI_SESSION_ID=post-sse-session \
./samples/ag-ui-yaml-service/scripts/http-post-sse-smoke-test.sh
```

Pass criteria:

- health endpoint returns HTTP 200
- `POST /agui/agent` returns HTTP 200 with SSE content containing `event: run.started` and `event: step.started`
- `POST /agui/agent` with `unknown.method` returns HTTP 400 with JSON-RPC error code `-32601`
