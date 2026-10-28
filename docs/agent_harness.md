# Agent harness

| Field | Value |
| --- | --- |
| Status | Active |
| Last updated | 2026-10-22 |
| Decision | ADR-0006 |

LogroBus is built with several AI coding agents (Claude Code and others, such as Codex, Gemini CLI and GitHub Copilot). The harness makes every agent follow the same rules, run the same quality gates and use the same workflows, without copying rules into agent-specific files.

## 1. Principles

1. **One rule book.** `AGENTS.md` is canonical. Agent-specific files only point to it.
2. **Gates are scripts.** Every check lives in `scripts/checks/` and runs from a shell, never only inside an agent's configuration (NFR-MNT-05).
3. **Workflows are documents.** Multi-step procedures live in `docs/playbooks/`. Agent skills and commands are thin wrappers that point to them.
4. **Enforcement does not depend on the agent.** Git hooks and CI enforce the rules for everyone. Agent hooks add earlier feedback; they never replace those gates.

## 2. Layers

| Layer | Location | Scope | Enforces |
| --- | --- | --- | --- |
| Rules | `AGENTS.md` (+ linked docs) | Every agent and human | Everything, by instruction |
| Workflows | `docs/playbooks/` | Every agent and human | Start story, finish story, ADRs, fixtures, review checklists |
| Gates | `scripts/checks/` | Every agent, human and CI | README parity, frozen spec, doc links, secrets, commit and PR titles, plus Gradle and SwiftLint once code exists |
| Local enforcement | `.githooks/` (installed by `scripts/bootstrap.sh`) | Every commit and push, by anyone | Conventional commits, secrets, no pushes to `main`, frozen spec, doc links, README parity (`engineering_workflow.md` §1.1) |
| Remote enforcement | `.github/workflows/` | Every PR, **once the repository is public** (`ci_cd.md` §1) | The same gates as atomic jobs |
| Agent adapters | `CLAUDE.md`, `.claude/`, `GEMINI.md`, `.github/copilot-instructions.md` | One agent each | Load the rule book; earlier feedback through hooks |

## 3. Adapter matrix

| Agent | Reads the rules through | Extra integration |
| --- | --- | --- |
| Claude Code | `CLAUDE.md` (imports `@AGENTS.md`) | `.claude/settings.json` (permissions and hooks), `.claude/agents/` (reviewers), `.claude/skills/` (workflows) |
| OpenAI Codex | `AGENTS.md`, natively | None. It runs `scripts/` manually, as instructed by `AGENTS.md` |
| Gemini CLI | `GEMINI.md` (imports `@AGENTS.md`) | None |
| GitHub Copilot | `.github/copilot-instructions.md`, plus `AGENTS.md` natively | None |
| Other agents (Cursor and similar) | `AGENTS.md`, natively | None |

**What agents without hooks must do manually**, as stated in `AGENTS.md` §10:
- At session start: `scripts/agent/session-start.sh`.
- After editing a file, optionally: `scripts/agent/post-edit.sh <file>`.
- Before finishing: `scripts/checks/run-all.sh`.

## 4. Claude Code adapter

### 4.1 Permissions (`.claude/settings.json`)

| List | Contents | Why |
| --- | --- | --- |
| `allow` | Gradle, `scripts/checks/*`, `scripts/agent/*`, `xcodebuild`, `swiftlint`, `shellcheck`, read-only git and `gh pr` queries, and `git push` to any branch but `main` (owner decision, 2026-10-28) | Routine work without prompts. The deny list catches the usual spellings of a push to `main` (`origin main`, `origin HEAD:main`) and of a force push (`--force`, `-f`), but not every one, such as `--force-with-lease` or a `+branch` refspec. `main`'s branch protection on GitHub rejects any direct push, and AGENTS.md forbids force pushes |
| `ask` | `gh pr create`, `gh pr merge`, `git reset --hard`, `scripts/record-fixtures.sh` (calls the live source), `scripts/bootstrap.sh`, `scripts/github/*` (repository administration) | Outward-facing or hard-to-reverse actions |
| `deny` | Editing the functional spec; reading `local.properties`, keystores, `.p12`, `.p8` and `.env`; pushing to `main`; force pushes; `rm -rf` | Frozen baseline, secrets, branch policy |

Personal overrides go in `.claude/settings.local.json`, which is gitignored and must never relax the deny list.

### 4.2 Hooks

| Event | Script | Behaviour |
| --- | --- | --- |
| `SessionStart` | `scripts/agent/session-start.sh` | Adds context: the language policy, the current branch, a warning when on `main`, a warning when git hooks are not installed |
| `PostToolUse` (`Edit\|Write`) | `scripts/agent/post-edit.sh` | Formats and lints the edited file with ktlint, SwiftLint or shellcheck when installed. Exit code 2 feeds unfixable findings back to the agent |
| `Stop` | `scripts/agent/stop-check.sh` | Blocks finishing when only one README was modified (the README rule); never blocks twice in a row |

### 4.3 Reviewer subagents (`.claude/agents/`)

| Subagent | Checklist | Use for |
| --- | --- | --- |
| `code-reviewer` | `playbooks/review-checklists.md` §1 | Every change |
| `data-rules-reviewer` | §2 | `shared/` data-layer changes |
| `android-ui-reviewer` | §3 | `android/` changes |
| `ios-ui-reviewer` | §4 | `iosApp/` changes |

### 4.4 Skills (`.claude/skills/`)

| Skill | Playbook |
| --- | --- |
| `/start-story LB-xxx` | `playbooks/start-story.md` |
| `/finish-story` | `playbooks/finish-story.md` |
| `/new-adr <topic>` | `playbooks/new-adr.md` |
| `/record-fixtures` | `playbooks/record-fixtures.md` |

## 5. Maintaining the harness

- **A rule changes:** edit `AGENTS.md` (and the detailed doc). If the rule can be checked mechanically, add or extend a script in `scripts/checks/`, wire it into `run-all.sh`, the git hooks and a CI job, and document it in `ci_cd.md` §3.
- **A workflow changes:** edit its playbook. Skills that point to it need no change.
- **A new agent arrives:** add a pointer file in that agent's convention, register it in §3 and in `AGENTS.md` §2, and never copy rules into it.
- **Never put logic in hook configuration.** Hooks call scripts, so every agent and CI share the same behaviour.
