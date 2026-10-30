#!/usr/bin/env bash
# Usage: scripts/checks/check-commit-identity.sh --current
#        scripts/checks/check-commit-identity.sh --message <file>
#        scripts/checks/check-commit-identity.sh <revision-range>
#
# Owner rule (AGENTS.md §12, 2026-10-30): every commit is authored and committed
# as LB_COMMIT_IDENTITY, and no commit message has a Co-Authored-By trailer. The
# one exception is the committer of the merge commits GitHub creates when a pull
# request is merged on the web.
#   --current         the identity the next commit will get (pre-commit hook)
#   --message <file>  a commit message (commit-msg hook)
#   <range>           every commit in the range, such as origin/main..HEAD
#                     (pre-push hook, run-all.sh)
set -uo pipefail
# shellcheck source=scripts/checks/lib.sh
. "$(dirname "$0")/lib.sh"

github_committer="GitHub <noreply@github.com>"
coauthor_re='^[[:space:]]*co-authored-by:'
fix="git config user.name \"$LB_COMMIT_NAME\" && git config user.email $LB_COMMIT_EMAIL"

# The "Name <email>" part of a git ident, without its date.
ident() { git var "$1" | sed 's/>.*/>/'; }

case "${1:-}" in
  --current)
    status=0
    for var in GIT_AUTHOR_IDENT GIT_COMMITTER_IDENT; do
      who="$(ident "$var")"
      if [ "$who" != "$LB_COMMIT_IDENTITY" ]; then
        lb_fail "Commit identity: $var is '$who', must be '$LB_COMMIT_IDENTITY' ($fix)"
        status=1
      fi
    done
    [ "$status" -eq 0 ] && lb_ok "Commit identity: $LB_COMMIT_IDENTITY"
    exit "$status"
    ;;
  --message)
    if grep -Eiq "$coauthor_re" "${2:?--message needs a file}"; then
      lb_fail "Commit identity: remove the Co-Authored-By trailer; commits have no co-authors (AGENTS.md §12)"
      exit 1
    fi
    exit 0
    ;;
  "" | -*)
    sed -n '2,4p' "$0" | sed 's/^# //' >&2
    exit 2
    ;;
esac

range="$1"
status=0
count=0
for sha in $(git rev-list "$range"); do
  count=$((count + 1))
  short="$(git rev-parse --short "$sha")"
  author="$(git log -1 --format='%an <%ae>' "$sha")"
  committer="$(git log -1 --format='%cn <%ce>' "$sha")"
  parents="$(git log -1 --format='%P' "$sha" | wc -w | tr -d ' ')"
  if [ "$author" != "$LB_COMMIT_IDENTITY" ]; then
    lb_fail "Commit identity: $short is authored by '$author', must be '$LB_COMMIT_IDENTITY'"
    status=1
  fi
  if [ "$committer" != "$LB_COMMIT_IDENTITY" ] && ! { [ "$parents" -gt 1 ] && [ "$committer" = "$github_committer" ]; }; then
    lb_fail "Commit identity: $short is committed by '$committer', must be '$LB_COMMIT_IDENTITY'"
    status=1
  fi
  if git log -1 --format=%B "$sha" | grep -Eiq "$coauthor_re"; then
    lb_fail "Commit identity: $short has a Co-Authored-By trailer"
    status=1
  fi
done
[ "$status" -eq 0 ] && lb_ok "Commit identity: $count commits in $range, all by $LB_COMMIT_IDENTITY, no co-authors"
exit "$status"
