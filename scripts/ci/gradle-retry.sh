#!/usr/bin/env bash
# Runs Gradle with the given arguments, and runs it again, up to three runs in all, when it failed only because a
# repository did not serve a dependency: a 429 or 5xx from Maven Central, or a "not found" from the Gradle Plugin
# Portal for an artifact it has. Gradle itself retries a 429 a few times, and a 404 never (docs/ci_cd.md §5).
# A failing test, a compile error or any other failure fails at once, with Gradle's exit code.
#
#   scripts/ci/gradle-retry.sh :shared:feature:lines:ciUnitTest
#
# GRADLE_COMMAND (default ./gradlew), GRADLE_ATTEMPTS (default 3) and GRADLE_RETRY_DELAY (seconds, default 60) are for
# tests.
set -uo pipefail

gradle="${GRADLE_COMMAND:-./gradlew}"
attempts="${GRADLE_ATTEMPTS:-3}"
delay="${GRADLE_RETRY_DELAY:-60}"

# What Gradle prints when a repository did not serve a dependency.
not_served='was not found in any of the following sources|Could not resolve all (artifacts|files|dependencies)|Could not (GET|HEAD) .*Received status code (429|5[0-9][0-9])|Could not find [a-z0-9_.-]+:[A-Za-z0-9_.-]+:'
# What Gradle prints when the code or a test failed: never worth a second run.
own_failure='There were failing tests|Compilation error|[0-9]+ tests completed, [0-9]+ failed|^e: '

output="$(mktemp)"
trap 'rm -f "$output"' EXIT

attempt=1
while true; do
  "$gradle" "$@" 2>&1 | tee "$output"
  status=${PIPESTATUS[0]}
  if [ "$status" -eq 0 ]; then
    exit 0
  fi
  if grep -qE "$own_failure" "$output" || ! grep -qE "$not_served" "$output"; then
    exit "$status"
  fi
  if [ "$attempt" -ge "$attempts" ]; then
    echo "::error::Gradle could not get a dependency in $attempts runs; giving up."
    exit "$status"
  fi
  attempt=$((attempt + 1))
  echo "::warning::A repository did not serve a dependency; running Gradle again in ${delay} s (run $attempt of $attempts)."
  sleep "$delay"
done
