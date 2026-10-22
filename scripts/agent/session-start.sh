#!/usr/bin/env bash
# Usage: scripts/agent/session-start.sh
#
# Prints a short context block for an AI agent at session start: the language
# policy, the current branch, and warnings about local setup. Claude Code adds
# this output to the model context; other agents can run it manually.
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
echo "- Quality gates: scripts/checks/run-all.sh. Workflows: docs/playbooks/."
exit 0
