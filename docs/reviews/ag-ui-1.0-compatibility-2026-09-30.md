# AG-UI 1.0 compatibility review

Reviewed September 30, 2026 against the current working tree (component version 1.4.0), including its existing uncommitted changes.

## Verdict

The component implements a subset of AG-UI features, but it is not currently a conformant AG-UI 1.0 producer. Existing core events fail the published 1.0 schema. New 1.0 features also need implementation. Interoperability with a particular tolerant client remains possible, but was not established by this review.

The announcement's backwards compatibility statement does not establish conformance for this component: malformed known fields remain errors. Missing optional features alone do not make a producer nonconformant; a producer need not emit every event family.

## Implementation follow-up

The subsequent correction adds default-on, offline outgoing event schema validation through `JacksonAgUiEventCodec`, with `-Dagui.validation.enabled=false` to opt out. Integer millisecond timestamps and event-specific identifiers correct the confirmed wire failures. Legacy interrupt/resume notifications use `CUSTOM` envelopes, and tool argument streams close before results. Tests cover all built-in event models and rejection/opt-out behavior. The findings below describe the original reviewed state; missing 1.0 features and transport limitations remain outside this correction.

## Findings

| Area | Current implementation | Assessment |
| --- | --- | --- |
| Event JSON | `AgUiEvent` carries `Instant timestamp`, `runId`, `sessionId`, and `threadId` for all events; `JacksonAgUiEventCodec` uses default Jackson time serialization. | Confirmed blocker: fractional seconds instead of an integer timestamp. `sessionId` is not a protocol field; `runId`/`threadId` are also invalid on text events. Keep internal identifiers separate from wire fields. |
| Core features | Run/step lifecycle, text start/content/end, tool start/args/end/result, state snapshot/delta. | 14 of the 31 standard event types have built-in constants/models. Presence does not establish schema or sequencing conformance. |
| Metadata | Base event supports an arbitrary metadata map. | Basic event emission is present. No client-side metadata merge behavior is implemented or verified. `rawEvent` and subagent attribution are absent from the base model. |
| Interrupt/resume | `AgUiInterruptProcessor` emits `INTERRUPT_REQUESTED`; `AgUiResumeProcessor` emits `RESUME_ACCEPTED` in the same run. | These are not 1.0 event types. Standard interrupts terminate a run with `RUN_FINISHED.outcome.type = interrupt` and interrupt entries; answers arrive through `RunAgentInput.resume` on a new run. |
| Completion/cancellation/usage | `AgUiRunFinished` has no fields beyond the base event. | No built-in outcome, cancellation, pending frontend tool call list, result, or token usage representation. Omitting a success outcome is allowed; the missing fields limit feature support. |
| Multimodal tool results | `AgUiToolCallResult.content` is a `String`; its object constructor calls `toString()`. | Cannot emit the standard ordered `ContentPart[]` representation for images/audio/video/documents. |
| Additional event families | No built-in reasoning, subagent, activity, messages snapshot, raw/custom, or chunk event models. | Feature gaps. Custom application event subclasses could add shapes, but this repository provides no implementation or verification for them. |
| HTTP/SSE server | Sample `/agui/agent` accepts plain JSON and returns SSE through envelope, request, bridge, and SSE processors. | Partial default binding. Request processing completes before the SSE body is built; SSE replays at most 200 events by default, capped at 500. Longer runs can lose their beginning or terminal event. No live incremental response is implemented in this path. |
| Outbound Camel producer | `AgUiProducer` sends a JSON-RPC envelope to the configured RPC path and returns the raw response. | A repository-specific interface, not the standard `RunAgentInput` POST plus AG-UI event consumption. |
| Version/capability declarations | No built-in `protocolVersion` output or `AgentCapabilities` model. | Capability/version support is absent; these declarations are optional, so absence alone is not a conformance blocker. |

## Verification

1. Ran `mvn -pl camel-ag-ui-component test -q`: **28 tests passed, zero failures/errors/skips**. These tests do not validate against the 1.0 schema; the codec test only checks for an event name and text substring.
2. Downloaded the published schema from `https://docs.ag-ui.com/spec/1.0/schema.json` and checked it with Python `jsonschema`'s Draft 2020-12 validator in an isolated temporary environment.
3. Ran a temporary Java probe against the freshly compiled component. It emitted `RUN_STARTED`, `TEXT_MESSAGE_CONTENT`, and `RUN_FINISHED` through `InMemoryAgUiSessionRegistry` and `JacksonAgUiEventCodec`.
4. **All three emitted events failed validation** against their matching event definitions. Converting the timestamp to integer milliseconds, removing `sessionId`, and removing `runId`/`threadId` from the text event made those three payloads validate. These corrections were applied only to probe data, not repository implementation.

Example actual output:

```json
{"type":"TEXT_MESSAGE_CONTENT","timestamp":1790782707.634435000,"runId":"r1","sessionId":"t1","threadId":"t1","messageId":"m1","delta":"hello"}
```

Remaining scope: this was a source review, existing component test run, and three-event wire validation. It was not a full behavioral conformance suite or end-to-end test with a 1.0 client.

## Upgrade priorities

1. Correct event serialization and validate every built-in event against the pinned 1.0 schema.
2. Implement the standard interrupt/new-run resume contract, cancellation, multimodal tool result content, and optional usage reporting.
3. Ensure POST/SSE delivers the complete run through its terminal event, including runs exceeding the current replay limits; add standard outbound HTTP/SSE support if the Camel producer is intended to connect to arbitrary AG-UI servers.
4. Add remaining event families as needed and test sequencing, input handling, and any advertised capabilities with a 1.0 client.

## Sources

- [AG-UI 1.0 announcement](https://www.copilotkit.ai/blog/ag-ui-1.0)
- [Published 1.0 JSON Schema](https://docs.ag-ui.com/spec/1.0/schema.json)
- [1.0 key changes](https://docs.ag-ui.com/spec/1.0/changelog)
- [HTTP/SSE binding](https://docs.ag-ui.com/spec/1.0/basic/transports/http-sse)

Primary implementation evidence: `model/AgUiEvent.java`, `model/AgUiEventTypes.java`, `model/AgUiRunFinished.java`, `model/AgUiToolCallResult.java`, `service/JacksonAgUiEventCodec.java`, `service/InMemoryAgUiSessionRegistry.java`, `processor/AgUiInterruptProcessor.java`, `processor/AgUiResumeProcessor.java`, `processor/AgUiSseProcessor.java`, `processor/AbstractAgUiAgentRequestProcessor.java`, `AgUiProducer.java`, and the sample `ag-ui-platform.camel.yaml` routes.
