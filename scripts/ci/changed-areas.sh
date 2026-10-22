#!/usr/bin/env bash
# Usage: scripts/ci/changed-areas.sh [base-ref]
#
# Prints one "<area>=true|false" line per CI area, in the format of
# $GITHUB_OUTPUT. An area is true when the diff against <base-ref> touches
# one of its paths (docs/ci_cd.md §4). Without a base ref, as on a push to
# main, every area is true. Run it locally to see which workflows a branch
# triggers, for example: scripts/ci/changed-areas.sh origin/main
set -euo pipefail

kotlin='^(shared|android|tools|build-logic|gradle)/|\.gradle\.kts$|^gradlew|^gradle\.properties$|^version\.properties$|^\.github/(workflows/kotlin-checks\.yml|actions/gradle-setup/)|^scripts/ci/'

if [ $# -eq 0 ]; then
  echo "kotlin=true"
  exit 0
fi

changed="$(git diff --name-only "$1"...HEAD)"
if printf '%s\n' "$changed" | grep -Eq "$kotlin"; then
  echo "kotlin=true"
else
  echo "kotlin=false"
fi
