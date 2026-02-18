# AG-UI YAML Service + AGUI Dojo Guide

This document explains:

- what AGUI Dojo is used for,
- how to run this Camel sample,
- how to install and run AGUI Dojo against this sample.

## What AGUI Dojo Is (Purpose)

AGUI Dojo is a visual test client for AG-UI agents and integrations.

Use it to:

- validate your endpoint behavior from a real chat UI,
- verify event streaming and rendering (`RUN_STARTED`, `TEXT_MESSAGE_CONTENT`, `RUN_FINISHED`, etc.),
- debug integration compatibility quickly while developing your backend.

For this sample, Dojo is a fast way to confirm that `POST /agui/agent` works end-to-end.

## 1. Start the Camel Sample

From repository root:

```bash
cd samples/ag-ui-yaml-service
mvn -DskipTests compile exec:java
```

Default endpoints:

- Health: `http://localhost:8080/health`
- Dojo-compatible agent endpoint: `http://localhost:8080/agui/agent`

Quick health check:

```bash
curl -s http://localhost:8080/health
```

## 2. Prerequisites for AGUI Dojo

Install:

- Node.js 18+
- pnpm (recommended: v10+)

Optional checks:

```bash
node -v
pnpm -v
```

## 3. Get AGUI Dojo

AGUI Dojo lives in the AG-UI monorepo:

```bash
git clone https://github.com/ag-ui-protocol/ag-ui.git
cd ag-ui
```

Install dependencies:

```bash
pnpm install
```

If proto generation is needed in your environment:

```bash
pnpm --filter @ag-ui/proto generate
```

Build Dojo dependencies:

```bash
pnpm -r --filter demo-viewer... build
```

## 4. Run Dojo Against This Camel Sample

Start Dojo and point `server-starter` integration to this sample endpoint:

```bash
SERVER_STARTER_URL=http://localhost:8080/agui/agent pnpm --filter demo-viewer start
```

If you need explicit port selection:

```bash
PORT=3000 SERVER_STARTER_URL=http://localhost:8080/agui/agent pnpm --filter demo-viewer start
```

Open:

- `http://localhost:3000/server-starter/feature/agentic_chat`

## 5. Expected Result in Dojo

When you send a chat message, Dojo should render an assistant response from this sample.

That confirms:

- request acceptance at `POST /agui/agent`,
- SSE stream parsing by Dojo,
- AG-UI event compatibility for chat rendering.

### Prompt Guide (What To Type In Dojo)

Use these prompts to quickly validate behavior:

- `hello`
  - Expected: standard assistant text flow (`TEXT_MESSAGE_START`/`CONTENT`/`END`) and run lifecycle events.
- `weather in berlin`
  - Expected: weather widget tool lifecycle (`TOOL_CALL_START`, `TOOL_CALL_ARGS`, `TOOL_CALL_RESULT`, `TOOL_CALL_END`) followed by weather summary text.
- `show me 49ers score`
  - Expected: sports widget tool lifecycle and sports summary text.
- `sports update please`
  - Expected (current behavior): plain text response only.
  - Note: sports widget currently triggers on specific sports keywords like `49ers`, `cowboys`, or team-name phrases.

Tip: if you want deterministic validation of tool events, use the weather/sports trigger phrases above exactly.

## 6. Event Compatibility Checklist (Important)

For Dojo compatibility in this repo version, ensure SSE event payloads include:

- uppercase AG-UI event type values (for example `RUN_STARTED`, `TEXT_MESSAGE_CONTENT`),
- `threadId` on events,
- `stepName` as top-level field on `STEP_STARTED` and `STEP_FINISHED`,
- `messageId` and `delta` on `TEXT_MESSAGE_CONTENT`.

Minimal flow:

- `RUN_STARTED`
- `STEP_STARTED`
- `TEXT_MESSAGE_START`
- `TEXT_MESSAGE_CONTENT`
- `TEXT_MESSAGE_END`
- `STEP_FINISHED`
- `RUN_FINISHED`

## 7. Troubleshooting

If Dojo page loads but chat hangs:

1. Check Camel sample logs for request handling and SSE flush.
2. Verify endpoint directly:

```bash
curl -N -X POST http://localhost:8080/agui/agent \
  -H 'content-type: application/json' \
  -d '{"threadId":"t1","runId":"r1","messages":[{"role":"user","content":"hello"}]}'
```

3. Confirm event payload fields match the compatibility checklist above.


## 8. Implement Weather Widget Flow In This Sample

Use this when you want Dojo to render weather tool activity (and tool result payloads) from this Camel sample.

### Where to implement

Recommended extension point:

- `/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/processor/AgUiRunTextProcessor.java`

Current runtime wiring:

- `/Users/roman/Projects/DScope/CamelAGUIComponent/camel-ag-ui-component/src/main/java/io/dscope/camel/agui/AgUiComponentApplicationSupport.java`

### High-level approach

1. In `AgUiRunTextProcessor`, detect weather intent from incoming text (for example contains `weather`).
2. Emit tool lifecycle events in AG-UI format (`TOOL_CALL_START`, `TOOL_CALL_ARGS`, `TOOL_CALL_RESULT`, `TOOL_CALL_END`).
3. Emit assistant text events (`TEXT_MESSAGE_START`, `TEXT_MESSAGE_CONTENT`, `TEXT_MESSAGE_END`) summarizing weather output.
4. Keep existing run lifecycle events (`RUN_STARTED`, `STEP_STARTED`, `STEP_FINISHED`, `RUN_FINISHED`) unchanged.

### Dojo-required tool event payload shape

For this Dojo/client version, use these fields:

- `TOOL_CALL_START`: `toolCallId`, `toolCallName`
- `TOOL_CALL_ARGS`: `toolCallId`, `delta` (string; JSON string is fine)
- `TOOL_CALL_RESULT`: `messageId`, `toolCallId`, `content` (string), optional `role: "tool"`
- `TOOL_CALL_END`: `toolCallId`

Also include `runId`, `threadId`, and `sessionId` consistently.

### Example event sequence for a weather response

Use this order inside one run:

1. `TOOL_CALL_START`
2. `TOOL_CALL_ARGS`
3. `TOOL_CALL_RESULT`
4. `TOOL_CALL_END`
5. `TEXT_MESSAGE_START`
6. `TEXT_MESSAGE_CONTENT`
7. `TEXT_MESSAGE_END`

Example payload content:

- tool name: `get_weather`
- args (as JSON string): `{"city":"Berlin","unit":"C"}`
- result content (as JSON string): `{"city":"Berlin","tempC":18,"condition":"Cloudy"}`
- assistant text delta: `Weather in Berlin: 18C and Cloudy.`

### Practical implementation notes

- The existing `AgUiToolCall*` model classes currently store most fields under `metadata`.
- If Dojo does not render tool results, add sample-specific event model classes that put required fields at top-level JSON (not nested in `metadata`).
- Keep your successful compatibility fixes:
  - uppercase `type` values,
  - top-level `stepName` on step events,
  - top-level `messageId` + `delta` for text content events.

### Test after implementation

1. Restart sample runtime:

```bash
cd samples/ag-ui-yaml-service
mvn -DskipTests compile exec:java
```

2. Start Dojo pointed at this sample:

```bash
SERVER_STARTER_URL=http://localhost:8080/agui/agent pnpm --filter demo-viewer start
```

3. Open:

- `http://localhost:3000/server-starter/feature/agentic_chat`

4. Send: `what is the weather in Berlin?`

Expected:

- assistant response appears,
- weather tool lifecycle is emitted in stream,
- tool result content is available to UI rendering.
