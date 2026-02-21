# Camel AG-UI Component

Apache Camel component and sample runtime for AG-UI protocol workflows.

This repository provides:

- A reusable Camel component (`agui:`) for producer/consumer integration.
- A protocol service runtime with JSON-RPC 2.0 request handling, SSE event streaming, and optional WebSocket scaffolding.
- Transport compatibility modes for both split endpoints (`/agui/rpc` + `/agui/stream/{runId}`) and single-endpoint POST+SSE (`/agui/agent`).
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
|     `- src/main/resources/routes/ag-ui-platform.camel.yaml
`- docs/
   |- architecture.md
   |- development.md
   `- TEST_PLAN.md
```

## Quick Start

AGUI Dojo integration guide for this sample:

- `samples/ag-ui-yaml-service/README.md`

## Using in Other Projects

Dependency coordinates for consumers:

- Group: `io.dscope.camel`
- Artifact: `camel-ag-ui-component`
- Version: match your release (for example `1.0.1`)

Maven:

```xml
<dependency>
   <groupId>io.dscope.camel</groupId>
   <artifactId>camel-ag-ui-component</artifactId>
   <version>1.0.1</version>
</dependency>
```

Gradle (Groovy):

```groovy
implementation 'io.dscope.camel:camel-ag-ui-component:1.0.1'
```

Gradle (Kotlin):

```kotlin
implementation("io.dscope.camel:camel-ag-ui-component:1.0.1")
```

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

Configuration defaults are loaded from `samples/ag-ui-yaml-service/src/main/resources/application.yaml`; any JVM `-D...` system properties override those values at runtime.

Example override: `mvn -DskipTests compile exec:java -Dagui.health.port=9080 -Dagui.rpc.port=9080`

```bash
cd samples/ag-ui-yaml-service
mvn -DskipTests compile exec:java
```

Recommended from repository root (avoids accidental root-module `exec:java` execution):

```bash
mvn -f samples/ag-ui-yaml-service/pom.xml -DskipTests -Dexec.mainClass=io.dscope.camel.agui.samples.Main compile exec:java
```

### Persistence Quickstart

JDBC mode (embedded Derby):

```bash
cd samples/ag-ui-yaml-service
mvn -DskipTests compile exec:java -Dcamel.persistence.enabled=true -Dcamel.persistence.backend=jdbc -Dcamel.persistence.jdbc.url=jdbc:derby:memory:agui;create=true
```

Redis mode:

```bash
cd samples/ag-ui-yaml-service
mvn -DskipTests compile exec:java -Dcamel.persistence.enabled=true -Dcamel.persistence.backend=redis -Dcamel.persistence.redis.uri=redis://localhost:6379
```

Enable optional WebSocket scaffold route:

```bash
mvn -DskipTests compile exec:java -Dagui.websocket.enabled=true -Dagui.websocket.path=/agui/ws
```

## Runtime Endpoints

| Method | URL | Purpose |
|---|---|---|
| `GET` | `http://localhost:8080/health` | Health/liveness |
| `GET` | `http://localhost:8080/diagnostics` | Runtime diagnostics |
| `GET` | `http://localhost:8080/agui/ui` | Browser UI for manual RPC + SSE visualization |
| `POST` | `http://localhost:8080/agui/rpc` | JSON-RPC protocol entrypoint |
| `POST` | `http://localhost:8080/agui/agent` | Single-endpoint POST+SSE transport (Dojo-style clients) |
| `POST` | `http://localhost:8080/agui/backend_tool_rendering` | Alias of `/agui/agent` for backend-tool-rendering clients |
| `GET` | `http://localhost:8080/agui/stream/{runId}` | SSE event stream |
| `WS` | `ws://localhost:8080/agui/ws` | Optional WebSocket scaffold route (`-Dagui.websocket.enabled=true`) |

## Transport Modes

### Split transport (existing)

- Send JSON-RPC calls to `POST /agui/rpc`
- Read events from `GET /agui/stream/{runId}`

### Single-endpoint POST+SSE (Dojo-compatible)

- Send JSON-RPC calls to `POST /agui/agent`
- Receive `text/event-stream` response in the same HTTP call

Example:

```bash
curl -N -X POST http://localhost:8080/agui/agent \
   -H 'Content-Type: application/json' \
   -d '{"jsonrpc":"2.0","id":"1","method":"run.start","params":{"runId":"dojo-run","sessionId":"dojo-session"}}'
```

Expected response stream includes events such as `run.started` and `step.started`.

Current sample emits uppercase AG-UI event names (for example `RUN_STARTED`, `STEP_STARTED`).

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
