# Development Guide

## Build

```bash
mvn clean test
```

## Module-by-Module

```bash
mvn -pl camel-ag-ui-component test
mvn -pl samples/ag-ui-yaml-service -am test
```

## Run Sample Runtime

From repository root (recommended):

```bash
mvn -f samples/ag-ui-yaml-service/pom.xml -DskipTests -Dexec.mainClass=io.dscope.camel.agui.samples.Main compile exec:java
```

Primary AG-UI POST+SSE endpoint:

- `POST /agui/agent`

Alias endpoint (same processor chain):

- `POST /agui/backend_tool_rendering`

## Extension Hooks

Soft integrations can be added through:

- `AgUiToolEventBridge`
- `AgUiTaskEventBridge`

Default runtime wiring uses no-op implementations.

## State Backends

Use `AgUiStateStore` to plug in external stores (Redis/JDBC/etc.).
The default runtime uses `InMemoryAgUiStateStore`.

## Persistence Runtime Configuration

Enable persistent mode:

```bash
-Dcamel.persistence.enabled=true
-Dcamel.persistence.backend=redis|jdbc|ic4j
```

Redis backend example:

```bash
-Dcamel.persistence.enabled=true
-Dcamel.persistence.backend=redis
-Dcamel.persistence.redis.uri=redis://localhost:6379
```

JDBC backend example (embedded Derby):

```bash
-Dcamel.persistence.enabled=true
-Dcamel.persistence.backend=jdbc
-Dcamel.persistence.jdbc.url=jdbc:derby:memory:agui;create=true
```

## Persistence Tests

Run JDBC persistence tests:

```bash
mvn -pl camel-ag-ui-component -Dtest=PersistentAgUiPersistenceJdbcTest test
```

Run Redis persistence tests:

```bash
mvn -pl camel-ag-ui-component -Dtest=PersistentAgUiPersistenceRedisTest -Dcamel.persistence.test.redis.uri=redis://localhost:6379 test
```

Redis tests auto-skip if Redis is unreachable.

## Optional WebSocket Scaffold

Sample runtime can add a WebSocket route (default disabled):

```bash
mvn -f samples/ag-ui-yaml-service/pom.xml -DskipTests -Dexec.mainClass=io.dscope.camel.agui.samples.Main compile exec:java -Dagui.websocket.enabled=true -Dagui.websocket.path=/agui/ws
```

Feature flags:

- `agui.websocket.enabled` (`false` by default)
- `agui.websocket.path` (`/agui/ws` by default)
