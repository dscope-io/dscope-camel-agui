#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
SAMPLE_DIR="$ROOT_DIR/samples/ag-ui-yaml-service"

HEALTH_PORT="${AGUI_HEALTH_PORT:-8080}"
RPC_PORT="${AGUI_RPC_PORT:-8080}"
RUN_ID="${AGUI_RUN_ID:-curl-run}"
SESSION_ID="${AGUI_SESSION_ID:-curl-session}"
TEXT_PAYLOAD="${AGUI_TEXT_PAYLOAD:-hello curl}"
AGUI_MVN_EXTRA_ARGS="${AGUI_MVN_EXTRA_ARGS:-}"

SERVICE_LOG="${TMPDIR:-/tmp}/agui-sample-service.log"
START_JSON="${TMPDIR:-/tmp}/agui_start.json"
TEXT_JSON="${TMPDIR:-/tmp}/agui_text.json"
FINISH_JSON="${TMPDIR:-/tmp}/agui_finish.json"
SSE_TXT="${TMPDIR:-/tmp}/agui_sse.txt"
BAD_JSON="${TMPDIR:-/tmp}/agui_bad.json"

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
  if [[ -n "$AGUI_MVN_EXTRA_ARGS" ]]; then
    read -r -a mvn_extra_args <<< "$AGUI_MVN_EXTRA_ARGS"
    mvn -DskipTests compile exec:java "${mvn_extra_args[@]}" >"$SERVICE_LOG" 2>&1
  else
    mvn -DskipTests compile exec:java >"$SERVICE_LOG" 2>&1
  fi
) &
SERVER_PID=$!

echo "Waiting for health endpoint on :$HEALTH_PORT"
for _ in {1..120}; do
  health_status="$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${HEALTH_PORT}/health" || true)"
  if [[ "$health_status" == "200" ]]; then
    break
  fi
  sleep 0.25
done

if [[ "${health_status:-}" != "200" ]]; then
  echo "Service failed to become healthy. Check log: $SERVICE_LOG" >&2
  exit 1
fi

rpc_url="http://localhost:${RPC_PORT}/agui/rpc"

post_rpc() {
  local payload="$1"
  local output_file="$2"
  local expected_status="$3"
  local expected_token="$4"

  local http_status
  http_status="$(curl --max-time 10 -s -o "$output_file" -w "%{http_code}" \
    -H 'Content-Type: application/json' \
    -d "$payload" \
    "$rpc_url")"

  if [[ "$http_status" != "$expected_status" ]]; then
    echo "RPC call failed. Expected HTTP $expected_status, got $http_status" >&2
    cat "$output_file" >&2
    exit 1
  fi

  if ! grep -q "$expected_token" "$output_file"; then
    echo "RPC response missing token: $expected_token" >&2
    cat "$output_file" >&2
    exit 1
  fi
}

echo "Running JSON-RPC lifecycle"
post_rpc \
  "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"method\":\"run.start\",\"params\":{\"runId\":\"$RUN_ID\",\"sessionId\":\"$SESSION_ID\"}}" \
  "$START_JSON" \
  "200" \
  '"status":"started"'

post_rpc \
  "{\"jsonrpc\":\"2.0\",\"id\":\"2\",\"method\":\"run.text\",\"params\":{\"runId\":\"$RUN_ID\",\"text\":\"$TEXT_PAYLOAD\"}}" \
  "$TEXT_JSON" \
  "200" \
  '"textLength"'

post_rpc \
  "{\"jsonrpc\":\"2.0\",\"id\":\"3\",\"method\":\"run.finish\",\"params\":{\"runId\":\"$RUN_ID\"}}" \
  "$FINISH_JSON" \
  "200" \
  '"status":"finished"'

echo "Validating SSE stream"
sse_status="$(curl --max-time 10 -s -o "$SSE_TXT" -w "%{http_code}" \
  "http://localhost:${RPC_PORT}/agui/stream/${RUN_ID}?afterSequence=0&limit=100")"

if [[ "$sse_status" != "200" ]]; then
  echo "SSE call failed. Expected HTTP 200, got $sse_status" >&2
  cat "$SSE_TXT" >&2
  exit 1
fi

for token in "event: run.started" "event: text.message.content" "event: run.finished"; do
  if ! grep -q "$token" "$SSE_TXT"; then
    echo "SSE stream missing token: $token" >&2
    cat "$SSE_TXT" >&2
    exit 1
  fi
done

echo "Validating unknown.method error"
bad_status="$(curl --max-time 10 -s -o "$BAD_JSON" -w "%{http_code}" \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":"4","method":"unknown.method","params":{}}' \
  "$rpc_url")"

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

echo "PASS: HTTP curl smoke test completed"
echo "- Health endpoint responded with HTTP 200"
echo "- JSON-RPC lifecycle calls succeeded"
echo "- SSE stream includes started/content/finished events"
echo "- Unknown method returns HTTP 400 with code -32601"
