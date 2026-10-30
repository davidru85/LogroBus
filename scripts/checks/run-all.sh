#!/usr/bin/env bash
# Usage: scripts/checks/run-all.sh [base-ref]
#
# Single local entry point for every quality gate, usable by humans and any AI
# agent (NFR-MNT-05). It includes uncommitted changes, so run it before
# committing or opening a PR. Gates for code that does not exist yet are
# skipped with a notice.
set -uo pipefail
dir="$(cd "$(dirname "$0")" && pwd)"
root="$(git rev-parse --show-toplevel)"
base="${1:-origin/main}"
status=0

export LB_INCLUDE_WORKTREE=1

echo "== Documentation and policy =="
"$dir/run-docs-checks.sh" "$base" || status=1
"$dir/check-secrets.sh" --all || status=1
"$dir/check-commit-identity.sh" --current || status=1
if git rev-parse --verify --quiet "$base^{commit}" >/dev/null; then
  "$dir/check-commit-identity.sh" "$base..HEAD" || status=1
fi

echo "== Shell scripts =="
if command -v shellcheck >/dev/null 2>&1; then
  # shellcheck disable=SC2046
  if shellcheck $(cd "$root" && git ls-files --cached --others --exclude-standard -- 'scripts/*.sh' 'scripts/**/*.sh' '.githooks/*' | sed "s|^|$root/|"); then
    echo "OK:   Shell scripts: shellcheck found no issues"
  else
    status=1
  fi
else
  echo "SKIP: shellcheck not installed (brew install shellcheck)"
fi

echo "== Kotlin and Android =="
if [ -x "$root/gradlew" ]; then
  # No configuration cache and no file-system watching: either can miss newly created source directories
  # and report their tests as NO-SOURCE, a false green in the gate whose summary goes into every PR.
  if (cd "$root" && ./gradlew check --quiet --console=plain --no-configuration-cache --no-watch-fs); then
    echo "OK:   Gradle check: unit tests, architecture rules, build-logic tests, Android lint, ktlint, detekt and Kover passed"
  else
    status=1
  fi
else
  echo "SKIP: no Gradle wrapper yet (LB-001)"
fi

echo "== Swift =="
if [ -d "$root/iosApp" ]; then
  if command -v swiftlint >/dev/null 2>&1; then
    (cd "$root" && swiftlint --strict) || status=1
  else
    echo "SKIP: swiftlint not installed (brew install swiftlint)"
  fi
else
  echo "SKIP: no iosApp yet (LB-001)"
fi

echo
if [ "$status" -eq 0 ]; then
  echo "All gates passed."
else
  echo "Some gates failed. See the messages above."
fi
exit "$status"
