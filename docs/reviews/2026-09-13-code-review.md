# Code review — 13 September 2026

Reviewed the working tree across the root/release POMs, core component (transport, processors, models, bridges, state and persistence), generated metadata, sample runtime/UI, scripts and tests. The existing root POM change from persistence 1.2.0 to 1.3.0 was preserved and included in validation. No implementation changes were made.

P1 means fix before production use; P2 means a concrete defect or hardening task to schedule next. Security exposure depends on deployment: the sample itself has no authentication boundary, while a downstream application could provide one externally. Performance observations below are based on code paths, not load benchmarks.

## Priority findings

### 1. P1 — Run reads and mutations have no ownership check

[AgUiRunTextProcessor.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/processor/AgUiRunTextProcessor.java:55), [AgUiSseProcessor.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/processor/AgUiSseProcessor.java:47), [sample routes](/Users/roman/Projects/DScope/CamelAGUIComponent/samples/ag-ui-yaml-service/src/main/resources/routes/ag-ui-platform.camel.yaml:30)

Every operation locates a run by caller-controlled `runId`; neither the registry nor processors associate it with an authenticated principal. The sample binds RPC, agent and replay endpoints to `0.0.0.0` without authentication. Someone who knows an existing ID can read its event history, append assistant messages, change state or finish it. The sample's default `demo-run` is predictable. Supplying a different session ID to `getOrCreate` does not reject access to an existing run.

**Proposed change:** provide a pluggable authorization boundary shared by RPC, POST+SSE, GET replay and WebSocket routes. Persist immutable owner/tenant identity and check it on every lookup and mutation. Default the sample to loopback and document how to expose it behind authentication. Test two principals attempting to access the same run; random IDs alone do not provide authorization.

### 2. P1 — Unbounded session/event retention enables memory exhaustion

[InMemoryAgUiSessionRegistry.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/service/InMemoryAgUiSessionRegistry.java:28), [PersistentAgUiSessionRegistry.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/service/PersistentAgUiSessionRegistry.java:126), [AgUiSseProcessor.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/processor/AgUiSseProcessor.java:45)

Neither registry evicts sessions or event lists. Completing a run does not release its history or state. Persistent mode still retains emitted JSON in memory, and a replay request for an unknown run can create a cached empty session. Repeated run creation or text/state writes grow heap indefinitely. The caller-controlled replay limit has no upper bound, and the entire selected history is copied into one response String.

**Proposed change:** introduce retention by age, event count and bytes; bound total sessions and per-principal request rates; avoid caching unknown runs; cap replay pages and response bytes; expose expired-cursor behavior. Keep a bounded memory window with durable paging. Apply request-body limits before JSON parsing. Verify eviction and boundary handling with small deterministic limits.

### 3. P1 — Producer fails to serialize the outgoing JSON-RPC envelope

[AgUiProducer.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/AgUiProducer.java:48)

The HTTP producer receives a `LinkedHashMap` plus an `application/json` header, but the envelope is never marshalled. A direct probe with the module's resolved dependencies failed before reaching a local HTTP handler: `InvalidPayloadException`, with no converter from `LinkedHashMap` to `InputStream`.

**Proposed change:** explicitly serialize the envelope to JSON bytes/String, set POST, and test the actual wire body against a local HTTP server. Reuse a lifecycle-managed ProducerTemplate instead of creating one per message; the present `stop()` is also skipped on exceptions, so the failing path leaves created templates without explicit cleanup.

### 4. P1 — Consumer transport is not started reliably and outlives its owner

[AgUiConsumer.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/AgUiConsumer.java:33), [AgUiConsumerRouteService.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/service/AgUiConsumerRouteService.java:32)

The consumer adds another route during its own `doStart()`, but does not manage that route's full lifecycle. In a direct CamelContext probe, initial startup reported the outer AG-UI route started while the HTTP route existed only as a definition; the port refused connections. Stopping/restarting the outer route subsequently created the HTTP listener. Stopping the outer route again left that listener responding with HTTP 500 and `RejectedExecutionException` from its stopped processor.

**Proposed change:** manage the underlying transport consumer directly, or register transport routes at the appropriate context lifecycle phase and explicitly start/stop/remove them with the owner. Give each owner an unambiguous route ID and reject conflicting bindings. Test first startup, stop, restart and removal through real HTTP requests; the existing test only checks route definitions.

### 5. P1 — Persistence conflict handling cannot safely support multiple writers

[PersistentAgUiSessionRegistry.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/service/PersistentAgUiSessionRegistry.java:156)

`emit()` advances the local sequence and caches an event before the append succeeds. On conflict it retries the same event ID and sequence, changing only the expected version to `rehydrate().envelope().version()`. That version need not include the tail. With two registries sharing embedded JDBC, writer A appended while writer B held stale state; B's retry failed with `expected 0 actual 2`. Its local event had already been added. Backends accepting the retry can also encounter a reused event ID/sequence.

**Proposed change:** separate durable concurrency version from event sequence, account for rehydrated tail events, and reconcile/rebuild the append on a bounded conflict retry. Publish to the local cache only after durable success. Prefer an atomic backend sequence allocation or enforce one writer per run. Test two live registries, append failures and restart/replay agreement; the existing conflict mock never advances the actual version.

### 6. P1 — State and error events do not match AG-UI wire fields

[AgUiStateSnapshot.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/model/AgUiStateSnapshot.java:21), [AgUiStateDelta.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/model/AgUiStateDelta.java:21), [AgUiRunError.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/model/AgUiRunError.java:21)

The codec emits state as `metadata.state`, patches as `metadata.patch`, and run errors as `metadata.error`. Standard AG-UI consumers expect top-level `snapshot`, `delta`, and `message`, respectively. A codec probe confirmed the state shape. Text-only sample assertions do not detect this interoperability failure. See the [official AG-UI event definitions](https://docs.ag-ui.com/concepts/events).

**Proposed change:** model those properties explicitly and validate serialized events against the AG-UI client schema. Include state synchronization and run failure in client integration coverage.

### 7. P2 — Generated patches target an empty property instead of the document

[AgUiStateDeltaCalculator.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/service/AgUiStateDeltaCalculator.java:25)

Both root add and replace operations use `path: "/"`. That pointer targets an object member named the empty string; the document root is `""`. A standards-compliant patch engine therefore adds an unintended member or rejects replacement of a missing member. This is independent of the event-envelope defect above. See [RFC 6901, section 5](https://www.rfc-editor.org/info/rfc6901/).

**Proposed change:** use the empty root pointer as the immediate repair. Test by applying the generated patch to the previous document and comparing the result with the requested state, including arrays, scalars and empty objects.

### 8. P2 — Rehydration before a snapshot loses the session identity

[PersistentAgUiSessionRegistry.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/service/PersistentAgUiSessionRegistry.java:96)

Without a snapshot, hydration chooses the caller's fallback session ID or the run ID. Replay restores only event records and sequence; it never restores the session ID from the events. The JDBC probe created `run` with session `original-session`, then loaded it through a second registry and obtained session `run`. Subsequent emitted events attach the wrong thread identity.

**Proposed change:** persist immutable run/session/thread metadata at creation, or recover it from the first durable lifecycle event before constructing SessionState. Reject conflicting fallback identity. Test restart after one event, before the first scheduled snapshot.

### 9. P2 — SSE reconnect replays old events and can permanently omit later ones

[AgUiSseProcessor.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/processor/AgUiSseProcessor.java:41), [sample UI](/Users/roman/Projects/DScope/CamelAGUIComponent/samples/ag-ui-yaml-service/src/main/resources/static/agui-ui.html:183)

The processor returns one finite batch, defaults to 200 events, and ignores `Last-Event-ID`. The sample EventSource repeatedly reconnects with `afterSequence=0`. A probe containing 205 events and `Last-Event-ID: 200` returned IDs starting at 1 and exactly 200 records. The sample will repeat that first page and never advance to the later records. POST orchestration also finishes before response construction and starts replay from zero, so repeated requests using the same run resend prior history.

**Proposed change:** honor the reconnect cursor, enforce bounded paging, and advance the sample cursor. For genuine live streaming, subscribe and flush events as they arrive with bounded queues and disconnect handling. For POST, capture the starting sequence for the request and deliver its events through completion, rather than silently truncating at 200.

### 10. P2 — Error handling leaks internal exception text and can fail itself

[AgUiErrorProcessor.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/processor/AgUiErrorProcessor.java:36)

Every exception message is returned verbatim to the client, including internal persistence and conversion errors. `Map.of` rejects a null message: a direct probe with `new RuntimeException()` caused the error processor itself to throw NullPointerException. Parse errors and internal failures are also both labelled invalid parameters. Unknown-method validation happens before the request ID is stored, losing response correlation.

**Proposed change:** return controlled public messages, log internal details with a correlation ID, use a non-null fallback, and distinguish parse, invalid-request, invalid-params and internal errors. Extract a valid request ID before method validation. Add malformed/null input and null-message exception tests.

### 11. P2 — Persistence settings in application.yaml are ignored during service creation

[AgUiComponentApplicationSupport.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/AgUiComponentApplicationSupport.java:105), [application.yaml](/Users/roman/Projects/DScope/CamelAGUIComponent/samples/ag-ui-yaml-service/src/main/resources/application.yaml:11)

Default services are constructed before Main configuration/customization and derive persistence settings exclusively from `System.getProperties()`. Setting persistence enabled/backend in the shipped YAML does not influence that selection; without JVM overrides it selects in-memory storage. This can silently defeat intended durability.

**Proposed change:** resolve persistence through Camel's configured properties after configuration loading, or accept an explicit PersistenceConfiguration from the application. Document precedence and test enabling JDBC solely through application configuration.

### 12. P2 — WebSocket origin restrictions are placed on the outgoing endpoint

[AgUiConsumerRouteService.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/service/AgUiConsumerRouteService.java:52), [sample Main](/Users/roman/Projects/DScope/CamelAGUIComponent/samples/ag-ui-yaml-service/src/main/java/io/dscope/camel/agui/samples/Main.java:42)

The component puts configured `allowedOrigins` on `sinkUri`, while its inbound WebSocket consumer is created from `wsUri` without that restriction. Configuring a trusted origin therefore does not configure the receiving handshake endpoint. The sample also explicitly permits all origins when WebSockets are enabled. This increases cross-origin access risk, especially if users later add cookie-based authentication.

**Proposed change:** apply the allowlist to the inbound consumer URI, configure it in the sample, and test accepted/rejected Origin handshakes. Keep response broadcast settings separate. This finding is based on route construction; an actual cross-origin browser exploit was not run.

### 13. P2 — Root build reports success without building or testing modules

[pom.xml](/Users/roman/Projects/DScope/CamelAGUIComponent/pom.xml:8), [development instructions](/Users/roman/Projects/DScope/CamelAGUIComponent/docs/development.md:16)

The root POM has no modules. `mvn test` completed successfully in about 0.06 seconds with no tests, and documented `-pl` commands cannot select the absent reactor projects. This defect predates the current persistence-version edit. Standalone sample builds can also resolve an installed component artifact instead of the current source.

**Proposed change:** restore the component and samples modules to the development reactor and keep the publication-only POM/profile separate. Make CI run the complete reactor and enforce expected test execution. Validation here used a temporary external aggregator without changing the project's build files.

### 14. P2 — Smoke tests assert obsolete event names

[HTTP smoke script](/Users/roman/Projects/DScope/CamelAGUIComponent/samples/ag-ui-yaml-service/scripts/http-curl-smoke-test.sh:111), [POST smoke script](/Users/roman/Projects/DScope/CamelAGUIComponent/samples/ag-ui-yaml-service/scripts/http-post-sse-smoke-test.sh:56)

Scripts search for lowercase dotted event names such as `run.started`, while the codec emits `RUN_STARTED`. Successful responses will fail these checks. The POST script's `AGUI_PORT` changes the target URL without passing corresponding port properties to the launched service.

**Proposed change:** assert the current uppercase event types, pass launch ports consistently, and use unique run IDs and temporary output directories so tests cannot validate stale data or another running service. Run scripts in CI against the just-built component.

## Additional changes worth scheduling

- **State update cost and consistency:** [AgUiStateUpdateProcessor.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/processor/AgUiStateUpdateProcessor.java:54) reads/writes state separately from emitting events, with no per-run transaction. Concurrent updates or append failure can leave durable state and replay disagreeing. It also emits a full snapshot on every update and another full state inside the replacement patch. Use versioned updates and an atomic event/state boundary or an event log as the source of truth; send initial/periodic snapshots and incremental deltas. Benchmark state payload size and update throughput after correctness is repaired.
- **History scan cost:** [InMemoryAgUiSessionRegistry.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/service/InMemoryAgUiSessionRegistry.java:104) scans from the beginning under synchronization for each cursor. Late-cursor polling is O(total retained history), even for a tiny response. Use indexed sequence lookup over a bounded buffer. Persistent emit also performs synchronous I/O under the session lock; consider ordered batch appends after preserving durability semantics.
- **Bridge injection:** [AgUiComponentApplicationSupport.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/AgUiComponentApplicationSupport.java:121) captures a new no-op tool bridge in the processor before the customizer can replace the registry bean. Rebinding `agUiToolEventBridge` does not replace that captured instance, and the task bridge has no call sites. Accept explicit dependencies or resolve custom beans before constructing processors; test callback delivery.
- **Endpoint metadata:** [AgUiEndpoint.java](/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/AgUiEndpoint.java:39) does not expose its configuration as a nested URI parameter, and generated endpoint metadata omits the configuration's transport options. Runtime manual binding and catalog/tooling therefore disagree. Add the appropriate nested configuration annotation and verify the regenerated catalog/URI factory exposes the supported options.
- **Lifecycle semantics:** interrupt/resume emit events but do not transition a run state, and `complete()` does not prevent later writes. If interrupt is intended to gate work or human approval, enforce that state in processors and persistence. Otherwise document it clearly as notification-only behavior.
- **Sample and script hardening:** keep using `textContent` for untrusted UI log content; replace repeated unbounded log-string concatenation with a bounded display. Bundle browser dependencies reproducibly rather than fetching all application code from a CDN at runtime. Redact credential-bearing Redis URIs in preflight errors.

## Dependency security assessment

The reviewed root pins Camel 4.20.0. Apache lists later fixes, including [CVE-2026-42527](https://camel.apache.org/security/CVE-2026-42527.html), fixed in 4.21.0, 4.18.3 and 4.14.8. That advisory affects particular Java-deserialization components; it does **not** establish an exploitable path through this repository's JSON-over-HTTP routes. Do not classify every advisory for the Camel version as a reachable vulnerability.

Proposed follow-up: generate a resolved production SBOM for both executable modules, run dependency vulnerability scanning including transitive Undertow/HTTP/Jackson/persistence dependencies, and assess each match against enabled routes. Upgrade to a supported patched Camel line with dependency convergence and regression tests. This review checked upstream advisories but did not perform an exhaustive CVE scan or audit the external camel-persistence implementation.

## Validation and recommended sequence

- Core suite: 22 discovered, 16 passed, 6 Redis-dependent skipped.
- Combined current-source reactor: 30 discovered, 23 passed, 7 Redis-dependent skipped. This includes 7 passing sample integration tests.
- Browser Playwright profile was not run; it is excluded by default. Redis-backed paths were not exercised.
- Direct temporary probes reproduced producer conversion failure, consumer startup/lifecycle failure, JDBC multi-writer failure, pre-snapshot identity loss, reconnect replay/truncation, null-message error-handler failure, and state-event serialization shape.
- No load benchmark, external penetration test, or exhaustive dependency scan was performed.

Suggested order: (1) authorization and resource bounds; (2) producer/consumer lifecycle and persistence durability; (3) wire-schema, patch and replay fixes; (4) configuration/build/CI repairs; (5) profile and optimize state updates and event persistence. Add regression coverage with each fix rather than relying on the existing happy-path assertions.
