#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/release-preflight.sh
# The reactor selection includes the parent POM and the component, excluding samples.
exec mvn -B -ntp -Pcentral-release -pl camel-ag-ui-component -am clean deploy "$@"
