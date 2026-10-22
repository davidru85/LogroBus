#!/usr/bin/env bash
# Usage: scripts/checks/check-secrets.sh [--staged|--all]
#
# Blocks signing material and obvious credentials from entering the repository
# (NFR-SEC-04). --staged (default) checks the git index; --all checks every
# tracked file. Pattern-based: a safety net, not a full secret scanner.
set -euo pipefail
# shellcheck source=lib.sh
. "$(dirname "$0")/lib.sh"
cd "$LB_REPO_ROOT"

mode="${1:---staged}"
self="scripts/checks/check-secrets.sh"

forbidden_names='(\.(jks|keystore|p12|p8|pem|key|mobileprovision|certSigningRequest)$|(^|/)local\.properties$|(^|/)\.env(\..*)?$|(^|/)secrets\.properties$)'
forbidden_content='(-----BEGIN [A-Z ]*PRIVATE KEY-----|AKIA[0-9A-Z]{16}|gh[pousr]_[A-Za-z0-9]{36}|github_pat_[A-Za-z0-9_]{20,}|xox[baprs]-[A-Za-z0-9-]{10,}|sk-ant-[A-Za-z0-9_-]{20,}|sk-[A-Za-z0-9]{40,})'

case "$mode" in
  --staged) files="$(git diff --cached --name-only --diff-filter=ACMR)" ;;
  --all) files="$(git ls-files)" ;;
  *) echo "Usage: $0 [--staged|--all]" >&2; exit 2 ;;
esac

status=0
while IFS= read -r f; do
  [ -n "$f" ] || continue
  if printf '%s\n' "$f" | grep -Eq "$forbidden_names"; then
    lb_fail "Secrets: forbidden file type committed: $f"
    status=1
    continue
  fi
  [ "$f" = "$self" ] && continue
  if [ "$mode" = "--staged" ]; then
    content="$(git show ":$f" 2>/dev/null || true)"
  else
    content="$(cat "$f" 2>/dev/null || true)"
  fi
  if printf '%s' "$content" | LC_ALL=C grep -Eq "$forbidden_content"; then
    lb_fail "Secrets: credential-like content found in $f"
    status=1
  fi
done <<EOF
$files
EOF

if [ "$status" -eq 0 ]; then
  lb_ok "Secrets: nothing suspicious ($mode)"
fi
exit "$status"
