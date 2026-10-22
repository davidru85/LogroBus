#!/usr/bin/env bash
# Usage: scripts/checks/check-conventional-title.sh "<commit subject or PR title>"
#
# Validates a Conventional Commit subject (docs/engineering_workflow.md §2):
# type(scope)!: summary, at most 100 characters. Used by the commit-msg hook
# and by the PR-title check in CI.
set -euo pipefail
# shellcheck source=lib.sh
. "$(dirname "$0")/lib.sh"

title="${1:-}"
if [ -z "$title" ]; then
  lb_fail "Title: empty"
  exit 1
fi

if ! [[ "$title" =~ $LB_CONVENTIONAL_RE ]]; then
  lb_fail "Title is not a Conventional Commit: '$title'. Expected 'type(scope): summary', type one of feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert."
  exit 1
fi

if [ "${#title}" -gt "$LB_TITLE_MAX" ]; then
  lb_fail "Title longer than $LB_TITLE_MAX characters (${#title}): '$title'"
  exit 1
fi

lb_ok "Title: '$title'"
