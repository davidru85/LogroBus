# Project log

A dated record of what was done and decided, newest first. Add an entry at the end of every working session. Keep entries short and link to the backlog stories, ADRs and open questions involved.

## Entry template

```markdown
## YYYY-MM-DD: <short title>

**Done**
- …

**Decisions**
- …

**Open / next**
- …
```

---

## 2026-10-22: Dependabot PR #2: actions/checkout v5 to v7

**Done**
- Reviewed Dependabot's bump of `actions/checkout` from v5 to v7 in `policy-checks.yml`. v7.0.0 only blocks fork checkouts on `pull_request_target` and `workflow_run`, which this repository does not use.
- Added the README, project-log and workflow commits to the Dependabot branch.
- The first docs commit claimed a project-log entry that a failed script step had not written. This commit adds it; history is not rewritten.

**Decisions**
- Dependabot PRs get no exception from the README rule. The README update is added to their branch before merging (`engineering_workflow.md` §9).

## 2026-10-22: Merge strategy: merge commits, no squash

**Decisions**
- PRs are not squashed when merged. The method is a merge commit only: no squash, no rebase.
- The merge commit takes the PR title (a Conventional Commit) and the PR body. The TDD commits are preserved in `main`'s history, and the first-parent history stays green.
- Branch protection no longer requires linear history.

**Done**
- Updated `engineering_workflow.md` §1.1, §3 and §5, `testing_strategy.md` §7.1, `AGENTS.md` §12, and `playbooks/finish-story.md`.
- Updated `scripts/github/protect-main.sh` and added `scripts/github/configure-merge.sh`, which the owner runs and which works on a private repository.
- Claude Code now denies `gh pr merge --squash` and `gh pr merge --rebase`.

**Open / next**
- `shellcheck` 0.11.0 installed. All harness scripts lint clean, with `.shellcheckrc` and explicit `source=` directives.
- Git hooks installed (`scripts/bootstrap.sh`), and GitHub merge settings applied (`scripts/github/configure-merge.sh`): merge commits only; the PR title and body are used for the merge commit; branches are deleted on merge.
- Next: open the first PR with this work (`docs-chore` template); after it, start LB-001.

## 2026-10-22: Owner decisions on scope, TDD, architecture and repository

**Done**
- Wrote ADR-0007 (clean architecture, feature modules, platform-specific presentation, shared presentation models), ADR-0008 (offline mode deferred) and ADR-0009 (telemetry contracts). Amended ADR-0001 and ADR-0004.
- Rewrote `architecture.md` for the feature-module layout.
- Updated `AGENTS.md`, `requirements.md`, `acceptance_criteria.md`, `backlog.md`, `open_questions.md`, `testing_strategy.md`, `engineering_workflow.md`, `ci_cd.md`, `code_style.md`, `design_guidelines.md`, `security_privacy.md`, `api_spec.md`, the playbooks, the PR template, the reviewer agents and both READMEs.
- Added `scripts/github/protect-main.sh`. The pre-push hook now only warns about README parity, because TDD pushes after every cycle.

**Decisions**
- **Offline functionality** is not in the MVP. Static data is still cached; the offline mode moves to v1.0 (LB-607, ADR-0008).
- **TDD protocol:** each phase gets its own commit (red, green, refactor), then a push. With nothing to refactor, the refactor commit is skipped and noted in the green commit body.
- **Privacy labels and disclosures** are the final MVP step (LB-414, which replaces LB-701).
- **`main` will be protected on GitHub** once the repository is public. The repository stays private now and goes public before the MVP is completed (LB-415, which replaces LB-708). OQ-20 is resolved.
- **No iPad in the MVP** (OQ-19 resolved, NFR-PLAT-03).
- **Telemetry:** contracts for analytics and crash reporting are built now (LB-017). The vendor is likely Firebase Analytics and Crashlytics, but not final (OQ-16).
- **Figma designs** for the MVP arrive near the end of the MVP (LB-413). Branding comes with them (OQ-18 resolved).
- **Architecture:** clean architecture with feature modules; a platform-specific presentation layer; presentation models shared through KMP (ADR-0007).

**Open / next**
- Confirm the interpretations noted in the chat: shared mappers count as presentation models; no empty refactor commits; squash merge keeps the TDD commits only in the PR.
- Install the git hooks, move this work to a branch, and open the first PR.

## 2026-10-22: Pre-scaffolding definition: requirements, guidelines and harnesses

**Done**
- **Requirements:**
  - `requirements.md`: the MVP scope baseline and the requirements added after the spec (FR-NAV, FR-NEAR, FR-LINE-12 to 16, FR-PLAN-11 to 14, NFR-PLAT, NFR-REL-05, NFR-MNT-04 and 05), plus the FR-HOME adaptation.
  - `acceptance_criteria.md`: criteria for all 61 MVP stories.
- **Engineering guidelines:**
  - `testing_strategy.md`, including TDD.
  - `engineering_workflow.md`, including the PR templates.
  - `ci_cd.md`, `release_process.md`, `security_privacy.md` and `code_style.md`.
  - `.editorconfig` and `.swiftlint.yml`.
- **Agent harness (ADR-0006):**
  - `agent_harness.md` and the playbooks.
  - `scripts/checks/*` and `scripts/agent/*`, `.githooks/`, `scripts/bootstrap.sh`.
  - Claude Code settings, hooks, four reviewer subagents and four skills.
  - `GEMINI.md` and `.github/copilot-instructions.md`.
- **CI and test harness:**
  - The `policy-checks.yml` workflow: six atomic jobs, inactive while the repository is private.
  - Two PR templates, `CODEOWNERS` and Dependabot for GitHub Actions.
  - The design of the test harness and the code workflows.
- Fixed the line 10 headsigns in the `design_guidelines.md` mock-ups. Brought the backlog references in line with the new requirement ids.

**Decisions**
- Several AI agents will work on the repository: `AGENTS.md` is canonical, and each agent gets a thin adapter (ADR-0006).
- OQ-16 (remote config, analytics and crash backend) is deferred until before the beta. OQ-18 (branding) is deferred, with a neutral placeholder theme in the meantime.
- **The owner's CI rule:** checks do not run while the repository is private. Local gates (git hooks plus `run-all.sh`) are authoritative, and their summary is pasted into every PR.
- CI checks are atomic: one concern per job, under 5 minutes, split when they grow.
- TDD is the default (`testing_strategy.md` §7).
- GitHub Free offers no branch protection for private repositories, so local enforcement is used (`engineering_workflow.md` §1.1).
- New open questions: OQ-19 (iPad), OQ-20 (visibility and plan), OQ-21 (licence), OQ-22 (domain).

**Open / next**
- The owner reviews the non-technical summary and the proposed defaults (OQ-19).
- Run `scripts/bootstrap.sh` to install the git hooks.
- From now on, changes go through a branch and a PR. Then start LB-001.

## 2026-10-22: Language policy

**Done**
- Added a strict language policy to the top of `AGENTS.md` and mentioned it in both READMEs.

**Decisions**
- Repository artefacts (code, comments, commits, pull requests, docs) are always in technical English.
- Agents talk to the owner in Peninsular Spanish unless told otherwise. The owner may write in English or Spanish.
- `README.es.md` and the localised UI strings are the only Spanish content in the repository.

**Open / next**
- Define the pre-scaffolding requirements, guidelines and harnesses (scope pending the owner's answers).

## 2026-10-22: Bilingual README and the README rule

**Done**
- Created the project README in English (`README.md`) and Spanish (`README.es.md`), with links to switch language.
- Added the README rule to `AGENTS.md` (§2, §10, §11, §12) and linked both READMEs from `docs/README.md`.

**Decisions**
- Every merged PR must update both READMEs. The update goes inside the PR, at least in "Recent changes" and "Project status".

**Open / next**
- Start M0 with LB-001.

## 2026-10-22: Owner decisions on OQ-13, OQ-14, OQ-15 and OQ-17

**Done**
- Recorded the owner's answers in `open_questions.md`, ADR-0002, ADR-0003, `AGENTS.md`, `architecture.md` and `design_guidelines.md`.

**Decisions**
- **OQ-13:** the iOS deployment target is 18.0, with Liquid Glass on iOS 26+ and a material fallback below. ADR-0003 is now accepted.
- **OQ-14:** the `applicationId`, bundle id and Kotlin package root are `com.ruizurraca.logrobus`. The name "LogroBus" and the scheme `logronobus://` are unchanged.
- **OQ-15:** the app always opens on Lines. Deep links open their target tab. G1 is met with one tap to Near Me.
- **OQ-17:** saved stops with live arrivals are shown in the Near Me sheet ("My stops").

**Open / next**
- Choose the https domain for deep links (spec §6.15) before LB-603.
- Start M0 with LB-001.

## 2026-10-22: Project documentation bootstrapped

**Done**
- Read the functional specification v0.1 (`logrono-bus-app-functional-spec.md`).
- Created `AGENTS.md` as the canonical agent instructions, and `CLAUDE.md` as a pointer that imports it.
- Created `docs/`:
  - `README.md`, `architecture.md`, `api_spec.md`, `design_guidelines.md`
  - `backlog.md`, `open_questions.md`, `project_log.md`
  - `adr/`, with ADR-0001 to ADR-0005.
- Initialised the git repository on branch `main` and made the first commit.

**Decisions**
- **Three-section navigation** (Lines, Near Me, How to Get There) replaces the five-tab structure of spec §7.1. Home was dissolved: favourite lines moved to the Lines carousel, and saved and nearby stops moved to the Near Me sheet. News, fares, recharge points and info moved to a More entry (ADR-0002, accepted).
- **Native UI on both platforms** over a shared KMP core: Compose with Material 3 Expressive, and SwiftUI with Liquid Glass. This resolves OQ-01 (ADR-0001, accepted).
- **iOS deployment target 18.0**, with the Liquid Glass fallback (ADR-0003, proposed; OQ-13).
- **Swift interop** through a single umbrella framework, `Shared`, plus SKIE (ADR-0004, proposed).
- **Android UI stack:** M3 Expressive, Navigation 3 with per-tab back stacks, and `NavigationSuiteScaffold` (ADR-0005, proposed).
- **MVP scope** is milestones M0–M4, built around the three sections. News, fares, recharge points, the widget and notifications move to M5 (v1.0); their spec priorities still apply to the public release.

**Open / next**
- The owner should confirm OQ-13 (iOS target), OQ-14 (identifiers), OQ-15 (launch tab) and OQ-17 (favourite stops placement).
- Start M0 with LB-001 (scaffold), then LB-003 (fixtures) and LB-004 to LB-006 (core, network, normaliser).
- Decide where to keep the functional spec: it currently sits at the repository root.
