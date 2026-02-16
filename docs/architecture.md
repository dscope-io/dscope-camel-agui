# Architecture

## Overview

The AG-UI project has two layers:

1. `camel-ag-ui-component` reusable endpoint and protocol processors.
2. `samples/ag-ui-yaml-service` runnable runtime exposing HTTP + SSE endpoints.

## Request Pipeline

```mermaid
flowchart LR
  A[POST /agui/rpc] --> B[AgUiJsonRpcEnvelopeProcessor]
  B --> C[AgUiMethodDispatchProcessor]
  C --> D[Method-specific processor]
  D --> E[JSON-RPC success envelope]
  B -. exception .-> F[AgUiErrorProcessor]
  C -. exception .-> F
  D -. exception .-> F
```

## Streaming Pipeline

```mermaid
flowchart LR
  A[Method processor emits AgUiEvent] --> B[AgUiSessionRegistry]
  B --> C[In-memory event buffer + sequence]
  D[GET /agui/stream/{runId}] --> E[AgUiSseProcessor]
  E --> F[text/event-stream payload]
```

## Core Runtime Beans

- `agUiJsonRpcEnvelopeProcessor`
- `agUiMethodDispatchProcessor`
- `agUiErrorProcessor`
- `agUiSseProcessor`
- `agUiHealthProcessor`
- `agUiDiagnosticsProcessor`
- `agUiSessionRegistry`
- `agUiStateStore`
