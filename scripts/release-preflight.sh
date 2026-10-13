#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Local repository artifacts are insufficient evidence of Central availability.
for artifact in camel-persistence camel-persistence-core camel-persistence-jdbc camel-persistence-redis; do
  url="https://repo.maven.apache.org/maven2/io/dscope/camel/$artifact/1.4.0/$artifact-1.4.0.pom"
  if ! curl --fail --silent --show-error --output /dev/null "$url"; then
    echo "Release blocked: publish io.dscope.camel:$artifact:1.4.0 to Maven Central first." >&2
    exit 1
  fi
done
echo "Required persistence dependency is available in Maven Central."
