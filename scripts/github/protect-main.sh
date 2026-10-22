#!/usr/bin/env bash
# Usage: scripts/github/protect-main.sh [owner/repo]
#
# Applies the branch protection for main described in
# docs/engineering_workflow.md §1.1 (LB-415). Run it by hand (the owner),
# once the repository is public or on a plan that supports protection.
# GitHub Free rejects it for private repositories (HTTP 403).
#
# - pull request required, 0 approvals (single maintainer merges own PRs)
# - required status checks (strict): the policy-checks jobs
# - enforced for admins, conversations resolved
# - linear history NOT required: PRs are merged with merge commits (no squash)
# - no force pushes, no deletions
set -euo pipefail

repo="${1:-$(gh repo view --json nameWithOwner -q .nameWithOwner)}"

if [ "$(gh repo view "$repo" --json isPrivate -q .isPrivate)" = "true" ]; then
  echo "Note: $repo is private; GitHub Free does not allow branch protection there (expect HTTP 403)." >&2
fi

gh api --method PUT "repos/$repo/branches/main/protection" --input - <<'JSON'
{
  "required_status_checks": {
    "strict": true,
    "contexts": [
      "PR title",
      "README parity",
      "Frozen spec",
      "Doc links",
      "Secrets scan",
      "Shell scripts"
    ]
  },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "required_approving_review_count": 0,
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": false
  },
  "restrictions": null,
  "required_linear_history": false,
  "required_conversation_resolution": true,
  "allow_force_pushes": false,
  "allow_deletions": false
}
JSON

echo "Branch protection applied to $repo:main."
