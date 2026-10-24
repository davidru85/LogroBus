#!/usr/bin/env bash
# Usage: scripts/agent/post-edit.sh [file]
#
# Agent-agnostic post-edit step: formats or lints one edited file with the
# tools installed locally, and does nothing when the tool is missing.
# - With an argument, it checks that file (any agent or human can call it).
# - Without one, it reads a Claude Code PostToolUse JSON payload from stdin.
# Exit code 2 reports a problem the agent must fix (Claude Code feeds stderr back).
set -uo pipefail

root="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"

file="${1:-}"
if [ -z "$file" ] && [ ! -t 0 ]; then
  file="$(jq -r '.tool_input.file_path // .tool_response.filePath // empty' 2>/dev/null || true)"
fi
[ -n "$file" ] || exit 0
[ -f "$file" ] || exit 0

case "$file" in
  *.kt|*.kts)
    # Only the ktlint pinned in the catalog: another version can format code that ktlintCheck then rejects.
    pinned="$(sed -n 's/^ktlint = "\(.*\)"$/\1/p' "$root/gradle/libs.versions.toml" 2>/dev/null)"
    if command -v ktlint >/dev/null 2>&1 && [ -n "$pinned" ] && ktlint --version 2>/dev/null | grep -q -F "$pinned"; then
      if ! out="$(ktlint --format "$file" 2>&1)"; then
        printf 'ktlint could not fix every issue in %s:\n%s\n' "$file" "$out" >&2
        exit 2
      fi
    fi
    ;;
  *.swift)
    if command -v swiftlint >/dev/null 2>&1; then
      (cd "$root" && swiftlint --fix --quiet "$file" >/dev/null 2>&1) || true
      if ! out="$(cd "$root" && swiftlint lint --quiet --strict "$file" 2>&1)"; then
        printf 'SwiftLint findings in %s:\n%s\n' "$file" "$out" >&2
        exit 2
      fi
    fi
    ;;
  *.sh|*/.githooks/*)
    if command -v shellcheck >/dev/null 2>&1; then
      if ! out="$(shellcheck -x "$file" 2>&1)"; then
        printf 'shellcheck findings in %s:\n%s\n' "$file" "$out" >&2
        exit 2
      fi
    fi
    ;;
esac
exit 0
