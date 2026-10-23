#!/usr/bin/env bash
# Usage: scripts/ci/changed-areas.sh [base-ref]
#
# Prints one "<area>=true|false" line per CI area, in the format of
# $GITHUB_OUTPUT. An area is true when the diff against <base-ref> touches
# one of its paths (docs/ci_cd.md §4). Without a base ref, as on a push to
# main, every area is true. Run it locally to see which workflows a branch
# triggers, for example: scripts/ci/changed-areas.sh origin/main
set -euo pipefail

# fixtures/ holds the payloads that the contract tests decode. The architecture tests also read the
# XcodeGen spec, the Swift package manifests and .gitignore.
kotlin='^(shared|android|tools|build-logic|gradle|fixtures)/|\.gradle\.kts$|^gradlew|^gradle\.properties$|^version\.properties$|^\.github/(workflows/kotlin-checks\.yml|actions/gradle-setup/)|^scripts/ci/|^iosApp/project\.yml$|^iosApp/Packages/[^/]+/Package\.swift$|^\.gitignore$'
# Everything that can change the iOS app: Swift code and lint, the shared Kotlin it links, the Gradle build that
# produces the framework, the version, and the iOS scripts and workflow.
ios='^(iosApp|shared|gradle|build-logic|scripts/ios|scripts/ci)/|^\.swiftlint\.yml$|\.gradle\.kts$|^gradlew|^gradle\.properties$|^version\.properties$|^\.github/(workflows/ios-checks\.yml|actions/gradle-setup/)'

if [ $# -eq 0 ]; then
  echo "kotlin=true"
  echo "ios=true"
  exit 0
fi

# --no-renames lists both sides of a move, so moving a file out of a Kotlin path counts.
changed="$(git diff --no-renames --name-only "$1"...HEAD)"
# Prints "<area>=true" when a changed path matches the area's pattern, "<area>=false" otherwise.
report() {
  # A here-string, not a pipe: grep -q exits at the first match, and with pipefail the
  # writer's SIGPIPE on a large diff would turn that match into "<area>=false".
  if grep -Eq "$2" <<<"$changed"; then
    echo "$1=true"
  else
    echo "$1=false"
  fi
}

report kotlin "$kotlin"
report ios "$ios"
