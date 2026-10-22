#!/usr/bin/env bash
# Usage: scripts/bootstrap.sh
#
# One-time (idempotent) local setup for humans and AI agents:
# installs the repository git hooks and reports missing tools. It never
# installs software or changes anything outside this repository's git config.
set -uo pipefail

root="$(git rev-parse --show-toplevel)"
cd "$root" || exit 1

git config core.hooksPath .githooks
chmod +x .githooks/* scripts/*.sh scripts/checks/*.sh scripts/agent/*.sh 2>/dev/null || true
echo "Git hooks installed (core.hooksPath=.githooks)."

check() {
  local tool="$1" why="$2"
  if command -v "$tool" >/dev/null 2>&1; then
    printf '  [ok]      %-10s %s\n' "$tool" "$why"
  else
    printf '  [missing] %-10s %s\n' "$tool" "$why"
  fi
}

echo "Tooling:"
check git "version control"
check jq "JSON processing in scripts and hooks (required)"
check gh "GitHub CLI for pull requests"
check java "JDK 21 for Gradle (Gradle toolchains can provision it)"
check xcodebuild "Xcode 26+ for the iOS app (macOS only)"
check swiftlint "Swift lint (brew install swiftlint)"
check ktlint "Kotlin formatting in the post-edit hook (optional; Gradle runs it too)"
check shellcheck "shell script lint (brew install shellcheck)"
check maestro "end-to-end flows (optional)"
