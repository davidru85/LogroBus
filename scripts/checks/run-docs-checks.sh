#!/usr/bin/env bash
# Usage: scripts/checks/run-docs-checks.sh [base-ref]
#
# Runs the documentation and policy gates: README parity, frozen spec and
# relative doc links. Used by CI on every pull request and by run-all.sh.
set -uo pipefail
dir="$(cd "$(dirname "$0")" && pwd)"
base="${1:-origin/main}"
status=0

"$dir/check-readme-parity.sh" "$base" || status=1
"$dir/check-spec-frozen.sh" "$base" || status=1
"$dir/check-doc-links.sh" || status=1

exit "$status"
