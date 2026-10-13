# Development Guide

## Build

```bash
mvn clean test
```

This repository is Maven-native (no Gradle wrapper/build files are maintained here).

## Maven and Gradle Commands

Maven (in this repository):

```bash
mvn clean test
mvn -pl camel-ag-ui-component test
mvn -f samples/ag-ui-yaml-service/pom.xml -DskipTests -Dexec.mainClass=io.dscope.camel.agui.samples.Main compile exec:java
```

Gradle equivalents (for consumers using this artifact in a Gradle project):

```bash
./gradlew clean test
./gradlew :your-module:test
./gradlew run
```

Use Maven commands in this repo; use Gradle commands in downstream Gradle-based projects.

## Dependency Coordinates (Consumers)

For downstream projects using this component:

- Group: `io.dscope.camel`
- Artifact: `camel-ag-ui-component`
- Version: use your released version (for example `1.4.0`)

Central release tooling selects the component and its parent POM. See [AG-UI 1.0 migration](agui-1.0-migration.md) for protocol configuration and dependency prerequisites.

Maven:

```xml
<dependency>
	<groupId>io.dscope.camel</groupId>
	<artifactId>camel-ag-ui-component</artifactId>
	<version>1.4.0</version>
</dependency>
```

Gradle (Groovy):

```groovy
implementation 'io.dscope.camel:camel-ag-ui-component:1.4.0'
```

Gradle (Kotlin):

```kotlin
implementation("io.dscope.camel:camel-ag-ui-component:1.4.0")
```

The parent POM is `io.dscope.camel:camel-ag-ui:1.4.0`; applications depend on the component JAR.

## Module-by-Module

```bash
mvn -pl camel-ag-ui-component test
mvn -pl samples/ag-ui-yaml-service -am test
```

## Run Sample Runtime

The sample binds to loopback by default. Set `-Dagui.access.token=<strong-random-token>` before exposing it outside the host; clients must send the matching `Authorization: Bearer` header.

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
-Dcamel.persistence.backend=redis|jdbc|redis_jdbc|ic4j
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

Redis + JDBC dehydration backend example:

```bash
-Dcamel.persistence.enabled=true
-Dcamel.persistence.backend=redis_jdbc
-Dcamel.persistence.redis.uri=redis://localhost:6379
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

Run Redis-JDBC persistence tests:

```bash
mvn -pl camel-ag-ui-component -Dtest=PersistentAgUiPersistenceRedisJdbcTest -Dcamel.persistence.test.redis.uri=redis://localhost:6379 test
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
