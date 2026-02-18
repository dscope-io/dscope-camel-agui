#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SAMPLE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

REDIS_URI="${CAMEL_PERSISTENCE_REDIS_URI:-redis://localhost:6379}"

check_redis_reachable() {
  if command -v redis-cli >/dev/null 2>&1; then
    if redis-cli -u "$REDIS_URI" ping >/dev/null 2>&1; then
      return 0
    fi
  fi

  if [[ "$REDIS_URI" =~ ^redis://([^:/]+):([0-9]+)$ ]]; then
    local host="${BASH_REMATCH[1]}"
    local port="${BASH_REMATCH[2]}"
    if command -v nc >/dev/null 2>&1 && nc -z "$host" "$port" >/dev/null 2>&1; then
      return 0
    fi
  fi

  echo "Redis preflight check failed: unable to reach $REDIS_URI" >&2
  echo "Set CAMEL_PERSISTENCE_REDIS_URI to a reachable instance and retry." >&2
  return 1
}

check_redis_reachable

cd "$SAMPLE_DIR"

exec mvn -DskipTests compile exec:java \
  -Dcamel.persistence.enabled=true \
  -Dcamel.persistence.backend=redis \
  -Dcamel.persistence.redis.uri="$REDIS_URI"
