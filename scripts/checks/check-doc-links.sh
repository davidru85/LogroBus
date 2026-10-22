#!/usr/bin/env bash
# Usage: scripts/checks/check-doc-links.sh
#
# Verifies that every relative Markdown link in tracked and untracked
# (non-ignored) .md files points to an existing file or directory.
# External links (http, https, mailto) and pure anchors are skipped.
set -euo pipefail
# shellcheck source=lib.sh
. "$(dirname "$0")/lib.sh"
cd "$LB_REPO_ROOT"

status=0
checked=0

while IFS= read -r md; do
  [ -f "$md" ] || continue
  dir="$(dirname "$md")"
  # Extract the target of every inline link: ](target)
  links="$(grep -oE '\]\([^)]+\)' "$md" | sed -E 's/^\]\(//; s/\)$//' || true)"
  [ -n "$links" ] || continue
  while IFS= read -r target; do
    # Drop an optional link title: (path "title")
    target="${target%% *}"
    target="${target#<}"
    target="${target%>}"
    case "$target" in
      http://*|https://*|mailto:*|\#*|'') continue ;;
    esac
    path="${target%%#*}"
    [ -n "$path" ] || continue
    checked=$((checked + 1))
    if [ ! -e "$dir/$path" ]; then
      lb_fail "Broken link in $md: $target"
      status=1
    fi
  done <<EOF
$links
EOF
done <<EOF
$(git ls-files --cached --others --exclude-standard -- '*.md')
EOF

if [ "$status" -eq 0 ]; then
  lb_ok "Doc links: $checked relative links resolve"
fi
exit "$status"
