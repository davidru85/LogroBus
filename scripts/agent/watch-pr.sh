#!/usr/bin/env bash
# Usage: scripts/agent/watch-pr.sh <number> [interval-seconds]
#
# Watches a pull request for the agent that opened it (AGENTS.md §10, docs/playbooks/finish-story.md
# step 9). It polls GitHub, by default every 180 s, and exits with one line saying why it stopped:
#   MERGED, CLOSED, CHANGES_REQUESTED, COMMENTS (a new comment or review since the watch started),
#   or CHECKS_FAILED, followed by each failed check and its link.
# The agent acts on that line and, while the pull request is still open, starts the watch again.
# After a push or a rerun, start it once the new run shows the checks as pending, or it reports the
# old failure again. A failed GitHub call, such as a network blip, is retried at the next poll
# instead of ending the watch.
set -uo pipefail

if [ $# -lt 1 ]; then
  echo "Usage: scripts/agent/watch-pr.sh <number> [interval-seconds]" >&2
  exit 2
fi
pr="$1"
interval="${2:-180}"

root="$(git rev-parse --show-toplevel)" || exit 1
cd "$root" || exit 1

count() { gh pr view "$pr" --json comments,reviews --jq '(.comments | length) + (.reviews | length)'; }
failed_checks() { gh pr checks "$pr" --json name,bucket,link --jq '.[] | select(.bucket == "fail") | "  \(.name): \(.link)"'; }

until baseline="$(count)"; do sleep "$interval"; done
while true; do
  if status="$(gh pr view "$pr" --json state,reviewDecision --jq '"\(.state) \(.reviewDecision // "NONE")"')"; then
    read -r state decision <<< "$status"
    case "$state" in
      MERGED) echo "MERGED: pull request #$pr"; exit 0 ;;
      CLOSED) echo "CLOSED: pull request #$pr, without a merge"; exit 0 ;;
    esac
    if [ "$decision" = "CHANGES_REQUESTED" ]; then
      echo "CHANGES_REQUESTED: pull request #$pr"
      exit 0
    fi
    if now="$(count)" && [ "$now" != "$baseline" ]; then
      echo "COMMENTS: pull request #$pr has new comments or reviews ($baseline -> $now)"
      exit 0
    fi
    if failed="$(failed_checks)" && [ -n "$failed" ]; then
      echo "CHECKS_FAILED: pull request #$pr"
      echo "$failed"
      exit 0
    fi
  fi
  sleep "$interval"
done
