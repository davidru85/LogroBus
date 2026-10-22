#!/usr/bin/env bash
# Usage: scripts/agent/stop-check.sh
#
# End-of-turn check for AI agents. It blocks only on an objective violation:
# one README changed in the working tree without the other (AGENTS.md §12).
# Reads a Claude Code Stop payload from stdin when present; on a repeated stop
# (stop_hook_active=true) it never blocks, which avoids loops.
# Exit code 2 = the agent must continue and fix the reported problem.
set -uo pipefail

if [ ! -t 0 ]; then
  active="$(jq -r '.stop_hook_active // false' 2>/dev/null || echo false)"
  if [ "$active" = "true" ]; then
    exit 0
  fi
fi

root="$(git rev-parse --show-toplevel 2>/dev/null)" || exit 0
cd "$root" || exit 0

changed="$( { git diff --name-only HEAD 2>/dev/null; git ls-files --others --exclude-standard; } | sort -u)"
en=0
es=0
if printf '%s\n' "$changed" | grep -qx 'README.md'; then en=1; fi
if printf '%s\n' "$changed" | grep -qx 'README.es.md'; then es=1; fi

if [ "$en" -ne "$es" ]; then
  if [ "$en" -eq 1 ]; then missing="README.es.md"; else missing="README.md"; fi
  printf 'README rule (AGENTS.md §12): only one README was modified. Update %s with equivalent content before finishing.\n' "$missing" >&2
  exit 2
fi
exit 0
