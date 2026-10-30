#!/usr/bin/env bash
# Shared helpers for scripts/checks/*. Source this file; do not execute it.
# Compatible with bash 3.2 (macOS default).

# The LB_* variables below are used by the scripts that source this file.
# shellcheck disable=SC2034

LB_REPO_ROOT="$(git rev-parse --show-toplevel)"
LB_SPEC_FILE="logrono-bus-app-functional-spec.md"
LB_CONVENTIONAL_RE='^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\([a-z0-9-]+\))?!?: [^ ].*$'
LB_TITLE_MAX=100
# Owner rule (AGENTS.md §12): the one identity of every commit, author and committer.
LB_COMMIT_NAME="David Ruiz"
LB_COMMIT_EMAIL="davidru85@gmail.com"
LB_COMMIT_IDENTITY="$LB_COMMIT_NAME <$LB_COMMIT_EMAIL>"

lb_fail() { printf 'FAIL: %s\n' "$*" >&2; }
lb_warn() { printf 'WARN: %s\n' "$*" >&2; }
lb_ok() { printf 'OK:   %s\n' "$*"; }

# Returns 0 when the given ref resolves to a commit.
lb_ref_exists() {
  git rev-parse --verify --quiet "$1^{commit}" >/dev/null 2>&1
}

# Prints the files changed on this branch relative to the merge base with BASE
# (one per line, sorted, unique). With LB_INCLUDE_WORKTREE=1 it also includes
# staged, unstaged and untracked files.
lb_changed_files() {
  local base="${1:-}"
  {
    if [ -n "$base" ] && lb_ref_exists "$base"; then
      git diff --name-only "$base...HEAD"
    fi
    if [ "${LB_INCLUDE_WORKTREE:-0}" = "1" ]; then
      git diff --name-only HEAD 2>/dev/null || true
      git ls-files --others --exclude-standard
    fi
  } | sed '/^$/d' | sort -u
}
