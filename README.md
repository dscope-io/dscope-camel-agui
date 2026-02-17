# Camel AG-UI Component

Apache Camel component and sample runtime for AG-UI protocol workflows.

This repository provides:

- A reusable Camel component (`agui:`) for producer/consumer integration.
- A protocol service runtime with JSON-RPC 2.0 request handling, SSE event streaming, and optional WebSocket scaffolding.
- Core AG-UI event model covering lifecycle, text, tool, state, and interrupt/resume events.
- In-memory session/state services with pluggable SPI interfaces.
- Optional persistence-backed session/state services via shared `camel-persistence` backends.
- Extension hooks for soft integration with MCP/A2A ecosystems.

## Project Structure

```text
camel-ag-ui/
|- pom.xml
|- camel-ag-ui-component/
|  `- src/main/java/io/dscope/camel/agui/
|- samples/
|  `- ag-ui-yaml-service/
|     |- src/main/java/io/dscope/camel/agui/samples/
|     `- src/main/resources/routes/ag-ui-platform.yaml
`- docs/
   |- architecture.md
   |- development.md
   `- TEST_PLAN.md
```

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+

### Build and Test

```bash
mvn clean test
```

Run persistence-specific tests:

```bash
# JDBC persistence tests (embedded Derby)
mvn -pl camel-ag-ui-component -Dtest=PersistentAgUiPersistenceJdbcTest test

# Redis persistence tests (requires reachable Redis)
mvn -pl camel-ag-ui-component -Dtest=PersistentAgUiPersistenceRedisTest -Dcamel.persistence.test.redis.uri=redis://localhost:6379 test
```

### Run Sample Runtime

```bash
cd samples/ag-ui-yaml-service
mvn exec:java
```

### Persistence Quickstart

JDBC mode (embedded Derby):

```bash
cd samples/ag-ui-yaml-service
mvn exec:java -Dcamel.persistence.enabled=true -Dcamel.persistence.backend=jdbc -Dcamel.persistence.jdbc.url=jdbc:derby:memory:agui;create=true
```

Redis mode:

```bash
cd samples/ag-ui-yaml-service
mvn exec:java -Dcamel.persistence.enabled=true -Dcamel.persistence.backend=redis -Dcamel.persistence.redis.uri=redis://localhost:6379
```

Enable optional WebSocket scaffold route:

```bash
mvn exec:java -Dagui.websocket.enabled=true -Dagui.websocket.path=/agui/ws
```

## Runtime Endpoints

| Method | URL | Purpose |
|---|---|---|
| `GET` | `http://localhost:8080/health` | Health/liveness |
| `GET` | `http://localhost:8080/diagnostics` | Runtime diagnostics |
| `POST` | `http://localhost:8081/agui/rpc` | JSON-RPC protocol entrypoint |
| `GET` | `http://localhost:8081/agui/stream/{runId}` | SSE event stream |
| `WS` | `ws://localhost:8081/agui/ws` | Optional WebSocket scaffold route (`-Dagui.websocket.enabled=true`) |

## Persistence Configuration

Persistence is disabled by default. Enable it with system properties:

```bash
-Dcamel.persistence.enabled=true
-Dcamel.persistence.backend=redis|jdbc|ic4j
```

Common persistence properties:

- `camel.persistence.snapshot-every-events` (default `25`)
- `camel.persistence.max-replay-events` (default `500`)

Redis backend properties:

- `camel.persistence.redis.uri` (default `redis://localhost:6379`)
- `camel.persistence.redis.key-prefix` (default `camel:state`)

JDBC backend properties:

- `camel.persistence.jdbc.url` (example: `jdbc:derby:memory:agui;create=true`)
- `camel.persistence.jdbc.user`
- `camel.persistence.jdbc.password`

## Supported Methods

- `run.start`
- `run.text`
- `tool.call`
- `state.update`
- `run.interrupt`
- `run.resume`
- `run.finish`
- `health`

## Camel URI

```text
agui:agentId[?options]
```

Options:

- `agentId`
- `serverUrl`
- `remoteUrl`
- `rpcPath`
- `streamPath`
- `allowedOrigins`
- `sendToAll`
- `websocketEnabled`
- `wsPath`
- `protocolVersion`

## License

Apache License 2.0.
