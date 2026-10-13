# AG-UI 1.0 in component 1.4.0

The component version is `1.4.0`; the standard wire protocol version is `1.0`.
Use `io.dscope.camel:camel-ag-ui-component:1.4.0`. The parent artifact
`io.dscope.camel:camel-ag-ui:1.4.0` is a POM, not the component JAR.

## Standard agent route

```java
from("agui:agent?transportMode=httpSse&serverUrl=undertow:http://127.0.0.1:8080")
    .process(exchange -> {
        var run = exchange.getProperty("agui.runContext", AgUiRunContext.class);
        var input = exchange.getIn().getBody(JsonNode.class);
        run.emit(new AgUiTextMessageStart(input.path("runId").asText(),
            input.path("threadId").asText(), "answer", "assistant"));
        run.emit(new AgUiTextMessageContent(input.path("runId").asText(),
            input.path("threadId").asText(), "answer", "Hello"));
        run.emit(new AgUiTextMessageEnd(input.path("runId").asText(),
            input.path("threadId").asText(), "answer"));
    });
```

POST a `RunAgentInput` to `/agui/agent` with `Content-Type: application/json`
and `Accept: text/event-stream`. Events flush as the route produces them. Normal
route completion closes open local entities and emits success. Explicit
`finish(result, usage)`, `interrupt(interrupts)`, `cancel()` or `fail(error)`
controls the terminal outcome. A successful frontend tool handoff contains the
unanswered tool call IDs; it is not an interrupt.

## Calling an agent

```java
from("direct:run")
    .to("agui:remote?transportMode=httpSse&remoteUrl=http://localhost:8080"
        + "&agentPath=/agui/agent&eventsEndpoint=direct:event");
from("direct:event").process(exchange -> {
    JsonNode event = exchange.getIn().getBody(JsonNode.class);
    // This runs for each normalized event, in order, before the run completes.
});
```

Pass a typed `RunAgentInput`, JSON string or JSON node as the request body.
The original exchange receives an `AgUiRunResult`; `agui.runResult` also exposes
partial state if delivery fails. HTTP 200 alone does not mean success: a missing
terminal event fails the call as truncation. Interrupted/truncated calls are
never retried automatically. Authorization headers are forwarded when supplied.

## Validation and compatibility

`-Dagui.validation.enabled=true` is the default. Set it to `false` to disable
schema checks. Framing, sequencing, patch application, resume coverage and
truncation checks still run. The bundled schema permits open application JSON
but closes protocol objects. Reception strips unknown closed fields and unknown
union members/types with warnings; malformed known material fails with validation
enabled. Local emission is strict. Optional `AgUiComponent.setEventMiddleware`
runs after retired-shape translation and before reception/schema processing.

All 31 events have Java models. Tool result `getContent()/setContent(String)`
remain available. `getContentParts()/setContentParts(List<ContentPart>)` use the
same standard `content` field, preserving ordered multimodal parts. Structured
tool result objects serialize as JSON text. Timestamps serialize as integer epoch
milliseconds. Encrypted reasoning is carried opaquely, never decrypted.

`transportMode=jsonRpc` remains the default. Existing RPC, GET replay and WebSocket
interfaces remain legacy integrations. Legacy interrupt/resume notifications
are `CUSTOM` events; standard interrupts are `RUN_FINISHED.outcome` and end the
run. Standard SSE has no `Last-Event-ID` resumption. The sample retains its old
POST bridge at `/agui/legacy/agent`.

## Interrupt continuity and storage

Resume uses a new run ID, the same thread ID, and a `resume` entry for every pending
interrupt. Each entry has `interruptId`, `status: "resolved" | "cancelled"` and,
for resolved entries, the answer `payload`. Missing/duplicate coverage and expired
resolved answers are rejected before opening the stream. Cancellation abandons
the previous request; agent code must inspect each answer before performing the
requested action. Omitted shared state and empty conversation history are restored
from checkpoints. Usage remains scoped to the new run.

Bind `agUiStateStore` and `agUiSessionRegistry` to persistent implementations for
recovery across process restarts. The component otherwise uses in-memory stores.
Checkpoint keys `agui.thread.<threadId>` and outbound
`agui.client.<remoteUrl><agentPath>.<threadId>` are separate from wire logs.
`AgUiStoredEventReplay.replay` translates complete legacy stored envelopes without
rewriting records. Replay requires complete run history; live delivery never
depends on the replay retention limit. One component instance serializes runs
within each thread; multi-node deployments need their own distributed ownership.

## Samples and verification

The YAML sample serves standard `/agui/agent`, legacy RPC/replay, and
`/agui/capabilities`. Send "approval" to demonstrate interrupt/resume, or "demo 1.0"
to demonstrate reasoning, activity, document tool results and interleaved parallel
subagents. Capabilities describe this configured sample, independently of model
availability.

```bash
npm ci --prefix tests/agui-client --ignore-scripts
mvn -B -ntp -Dagui.interop=true test
mvn -B -ntp -pl samples/ag-ui-yaml-service -am -Pui-e2e test
mvn -B -ntp -P release-artifacts -pl camel-ag-ui-component -am clean verify
./scripts/release-preflight.sh
```

Official client tests pin `@ag-ui/client` and `@ag-ui/core` to `1.0.0` in the lockfile.
Redis recovery tests require Redis, defaulting to `redis://localhost:6379`, or
`-Dcamel.persistence.test.redis.uri=...`; CI supplies Redis. Schema attribution
and its license are bundled beside `schema-1.0.json`.

The release artifact profile builds sources and Javadocs without signing or
publication. Central release tooling selects both parent and component, excluding
samples. Publication of camel-persistence **1.4.0**, including core/JDBC/Redis
artifacts, is a prerequisite. Passing a build against local dependency artifacts
does not establish Central readiness. No deployment is part of this change.
HTTP/Protobuf is outside this release's scope.
