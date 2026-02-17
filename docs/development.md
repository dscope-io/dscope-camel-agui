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
cd samples/ag-ui-yaml-service
mvn exec:java -Dagui.websocket.enabled=true -Dagui.websocket.path=/agui/ws
```

Feature flags:

- `agui.websocket.enabled` (`false` by default)
- `agui.websocket.path` (`/agui/ws` by default)
