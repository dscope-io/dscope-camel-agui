# TEST PLAN

## Automated Commands

```bash
mvn test
mvn -pl samples/ag-ui-yaml-service -am test
```

## Manual Verification

Start sample service:

```bash
cd samples/ag-ui-yaml-service
mvn exec:java
```

### 1. Health and diagnostics

```bash
curl -s http://localhost:8080/health
curl -s http://localhost:8080/diagnostics
```

### 2. Start run

```bash
curl -s http://localhost:8081/agui/rpc \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":"1","method":"run.start","params":{"runId":"demo-run","sessionId":"demo-session"}}'
```

### 3. Emit text

```bash
curl -s http://localhost:8081/agui/rpc \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":"2","method":"run.text","params":{"runId":"demo-run","text":"hello from ag-ui"}}'
```

### 4. Update state

```bash
curl -s http://localhost:8081/agui/rpc \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":"3","method":"state.update","params":{"runId":"demo-run","state":{"cart":["widget-a"]}}}'
```

### 5. Finish run

```bash
curl -s http://localhost:8081/agui/rpc \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":"4","method":"run.finish","params":{"runId":"demo-run"}}'
```

### 6. Read stream

```bash
curl -s "http://localhost:8081/agui/stream/demo-run?afterSequence=0&limit=100"
```

Expected: ordered SSE event records for lifecycle, text, tool/state updates, and run completion.

### 7. Optional WebSocket Scaffold

Start with WS enabled:

```bash
cd samples/ag-ui-yaml-service
mvn exec:java -Dagui.websocket.enabled=true -Dagui.websocket.path=/agui/ws
```

Expected: runtime starts successfully and exposes `ws://localhost:8081/agui/ws`.
