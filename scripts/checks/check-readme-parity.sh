#!/usr/bin/env bash
# Usage: scripts/checks/check-readme-parity.sh [base-ref]
#
# Enforces the README rule (AGENTS.md §12): every change set must update both
# README.md (English) and README.es.md (Spanish), and the two files must keep
# the same section structure and link to each other.
# base-ref defaults to origin/main. Set LB_INCLUDE_WORKTREE=1 to also consider
# uncommitted changes.
set -euo pipefail
# shellcheck source=lib.sh
. "$(dirname "$0")/lib.sh"
cd "$LB_REPO_ROOT"

base="${1:-origin/main}"
status=0

changed="$(lb_changed_files "$base")"
if [ -z "$changed" ]; then
  lb_ok "README parity: no changes relative to $base"
  exit 0
fi

en_changed=0
es_changed=0
if printf '%s\n' "$changed" | grep -qx 'README.md'; then en_changed=1; fi
if printf '%s\n' "$changed" | grep -qx 'README.es.md'; then es_changed=1; fi

if [ "$en_changed" -ne 1 ] || [ "$es_changed" -ne 1 ]; then
  lb_fail "README parity: every change set must update both README.md and README.es.md (README.md changed: $en_changed, README.es.md changed: $es_changed)."
  status=1
fi

for file in README.md README.es.md; do
  if [ ! -f "$file" ]; then
    lb_fail "README parity: $file is missing."
    exit 1
  fi
done

h2_en="$(grep -c '^## ' README.md || true)"
h2_es="$(grep -c '^## ' README.es.md || true)"
h3_en="$(grep -c '^### ' README.md || true)"
h3_es="$(grep -c '^### ' README.es.md || true)"
if [ "$h2_en" != "$h2_es" ] || [ "$h3_en" != "$h3_es" ]; then
  lb_fail "README parity: section structure differs (README.md: $h2_en h2 / $h3_en h3, README.es.md: $h2_es h2 / $h3_es h3)."
  status=1
fi

if ! grep -q '(README.es.md)' README.md || ! grep -q '(README.md)' README.es.md; then
  lb_fail "README parity: each README must link to the other language version."
  status=1
fi

if [ "$status" -eq 0 ]; then
  lb_ok "README parity: both READMEs updated with matching structure"
fi
exit "$status"
