#!/usr/bin/env bash
# Usage: scripts/checks/check-spec-frozen.sh [base-ref]
#
# The functional spec is a frozen baseline (AGENTS.md §2). A change set that
# modifies it must also add or modify an ADR in docs/adr/ explaining why.
set -euo pipefail
# shellcheck source=lib.sh
. "$(dirname "$0")/lib.sh"
cd "$LB_REPO_ROOT"

base="${1:-origin/main}"
changed="$(lb_changed_files "$base")"

if ! printf '%s\n' "$changed" | grep -qx "$LB_SPEC_FILE"; then
  lb_ok "Frozen spec: $LB_SPEC_FILE unchanged"
  exit 0
fi

if printf '%s\n' "$changed" | grep -Eq '^docs/adr/[0-9]{4}-[a-z0-9-]+\.md$'; then
  lb_ok "Frozen spec: $LB_SPEC_FILE changed together with an ADR"
  exit 0
fi

lb_fail "Frozen spec: $LB_SPEC_FILE changed without an ADR in docs/adr/. Record the deviation in an ADR instead of editing the spec silently."
exit 1
