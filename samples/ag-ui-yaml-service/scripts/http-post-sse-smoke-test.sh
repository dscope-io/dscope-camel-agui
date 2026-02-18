#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
SAMPLE_DIR="$ROOT_DIR/samples/ag-ui-yaml-service"

PORT="${AGUI_PORT:-8080}"
RUN_ID="${AGUI_RUN_ID:-post-sse-run}"
SESSION_ID="${AGUI_SESSION_ID:-post-sse-session}"

SERVICE_LOG="${TMPDIR:-/tmp}/agui-post-sse-service.log"
POST_SSE_TXT="${TMPDIR:-/tmp}/agui_post_sse.txt"
BAD_JSON="${TMPDIR:-/tmp}/agui_post_sse_bad.json"

cleanup() {
  if [[ -n "${SERVER_PID:-}" ]] && kill -0 "$SERVER_PID" 2>/dev/null; then
    kill "$SERVER_PID" 2>/dev/null || true
    wait "$SERVER_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

echo "Starting sample runtime from: $SAMPLE_DIR"
(
  cd "$SAMPLE_DIR"
  mvn -DskipTests compile exec:java >"$SERVICE_LOG" 2>&1
) &
SERVER_PID=$!

echo "Waiting for health endpoint on :$PORT"
for _ in {1..120}; do
  health_status="$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${PORT}/health" || true)"
  if [[ "$health_status" == "200" ]]; then
    break
  fi
  sleep 0.25
done

if [[ "${health_status:-}" != "200" ]]; then
  echo "Service failed to become healthy. Check log: $SERVICE_LOG" >&2
  exit 1
fi

echo "Validating POST+SSE transport"
post_sse_status="$(curl --max-time 15 -s -o "$POST_SSE_TXT" -w "%{http_code}" \
  -X POST "http://localhost:${PORT}/agui/agent" \
  -H 'Content-Type: application/json' \
  -d "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"run.start\",\"params\":{\"runId\":\"$RUN_ID\",\"sessionId\":\"$SESSION_ID\"}}")"

if [[ "$post_sse_status" != "200" ]]; then
  echo "POST+SSE call failed. Expected HTTP 200, got $post_sse_status" >&2
  cat "$POST_SSE_TXT" >&2
  exit 1
fi

for token in "event: run.started" "event: step.started"; do
  if ! grep -q "$token" "$POST_SSE_TXT"; then
    echo "POST+SSE response missing token: $token" >&2
    cat "$POST_SSE_TXT" >&2
    exit 1
  fi
done

echo "Validating POST+SSE unknown.method error"
bad_status="$(curl --max-time 10 -s -o "$BAD_JSON" -w "%{http_code}" \
  -X POST "http://localhost:${PORT}/agui/agent" \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":"2","method":"unknown.method","params":{}}')"

if [[ "$bad_status" != "400" ]]; then
  echo "Negative test failed. Expected HTTP 400, got $bad_status" >&2
  cat "$BAD_JSON" >&2
  exit 1
fi

if ! grep -q '"code":-32601' "$BAD_JSON"; then
  echo "Negative test missing JSON-RPC method-not-found code" >&2
  cat "$BAD_JSON" >&2
  exit 1
fi

echo "PASS: POST+SSE smoke test completed"
echo "- /agui/agent returned SSE payload with run.started and step.started"
echo "- unknown.method returned HTTP 400 with code -32601"
