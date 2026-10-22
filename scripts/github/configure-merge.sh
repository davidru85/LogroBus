#!/usr/bin/env bash
# Usage: scripts/github/configure-merge.sh [owner/repo]
#
# Configures how pull requests are merged (docs/engineering_workflow.md §5):
# merge commits only (no squash, no rebase), the merge commit titled with the
# PR title and described with the PR body, and branches deleted after merge.
# Works on private repositories on GitHub Free. Run it by hand (the owner).
set -euo pipefail

repo="${1:-$(gh repo view --json nameWithOwner -q .nameWithOwner)}"

gh api --method PATCH "repos/$repo" \
  -F allow_merge_commit=true \
  -F allow_squash_merge=false \
  -F allow_rebase_merge=false \
  -F delete_branch_on_merge=true \
  -f merge_commit_title=PR_TITLE \
  -f merge_commit_message=PR_BODY \
  --jq '{allow_merge_commit, allow_squash_merge, allow_rebase_merge, delete_branch_on_merge, merge_commit_title, merge_commit_message}'

echo "Merge settings applied to $repo."
