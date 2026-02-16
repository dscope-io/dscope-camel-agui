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

## Optional WebSocket Scaffold

Sample runtime can add a WebSocket route (default disabled):

```bash
cd samples/ag-ui-yaml-service
mvn exec:java -Dagui.websocket.enabled=true -Dagui.websocket.path=/agui/ws
```

Feature flags:

- `agui.websocket.enabled` (`false` by default)
- `agui.websocket.path` (`/agui/ws` by default)
