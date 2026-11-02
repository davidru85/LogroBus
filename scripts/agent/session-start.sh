#!/usr/bin/env bash
# Usage: scripts/agent/session-start.sh
#
# Prints a short context block for an AI agent at session start: the language
# policy, the current branch, warnings about local setup, and the open pull
# requests whose watch the agent starts before any other task (AGENTS.md §10).
# Claude Code adds this output to the model context; other agents can run it
# manually.
set -uo pipefail

root="$(git rev-parse --show-toplevel 2>/dev/null)" || exit 0
cd "$root" || exit 0

branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo unknown)"
hooks="$(git config --get core.hooksPath || true)"

echo "LogroBus session context:"
echo "- Language policy: reply to the owner in Peninsular Spanish (es-ES); write every repository artefact in technical English."
echo "- Current branch: $branch."
if [ "$branch" = "main" ]; then
  echo "- You are on main. Create a story branch (docs/playbooks/start-story.md) before changing files; never push to main."
fi
if [ "$hooks" != ".githooks" ]; then
  echo "- Git hooks are not installed. Ask the owner to run scripts/bootstrap.sh."
fi
# The open pull requests of the signed-in account, which opens the agents' pull requests.
# The time limit keeps the hook inside its 10 s budget on a slow network.
if command -v gh >/dev/null 2>&1 &&
  prs="$(perl -e 'alarm shift; exec @ARGV' 6 gh pr list --author @me --state open \
    --json number,headRefName \
    --jq '.[] | "  - #\(.number) (\(.headRefName)): scripts/agent/watch-pr.sh \(.number) 180"' 2>/dev/null)"; then
  if [ -n "$prs" ]; then
    echo "- Open pull requests. Before any other task, start the watch of each in the background, unless this session already runs it, and act on its result (AGENTS.md §10):"
    echo "$prs"
  else
    echo "- No open pull requests to watch."
  fi
else
  echo "- The open pull requests could not be listed (gh missing, offline or signed out). Before any other task, list them with 'gh pr list --author @me' and watch each one (AGENTS.md §10)."
fi
echo "- Quality gates: scripts/checks/run-all.sh. Workflows: docs/playbooks/."
exit 0
