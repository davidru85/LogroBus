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

## 2026-10-23: LB-002 part 1: Kotlin checks in CI

**Done**
- `kotlin-checks.yml`, with one atomic job per concern:
  - `Build-logic tests` (`:build-logic:convention:test`);
  - `Architecture rules` (`:tools:architecture-tests:test`);
  - `Shared unit tests: <module>`, a matrix over the 19 shared modules (`ciUnitTest`).
- Path filtering with a gate:
  - `Kotlin changes` runs `scripts/ci/changed-areas.sh`. It reports `kotlin=false` for a docs-only change, so the Kotlin jobs are skipped.
  - `Kotlin gate` always runs. It passes when every job passed or was skipped, and it is the one Kotlin check made required, so docs-only PRs are not blocked.
  - Verified locally against a docs-only commit (false), a code merge (true) and a push with no base (true).
- The workflow also runs on pushes to `main`: it verifies each merge and writes the Gradle cache that pull requests read.
- The JDK and Gradle setup lives in the `.github/actions/gradle-setup` composite action. `gradle/actions` v6.4.0 is pinned by commit SHA, because it is third-party. Dependabot now also watches `.github/actions/*`.
- TDD cycle 1 (matrix guard):
  - Red: `lb002_kotlinWorkflowRunsUnitTestsForEverySharedModule` requires the matrix to list exactly the shared modules in `settings.gradle.kts`.
  - Green: the workflow, plus the `workflowMatrixModules` pure rule with its own test. The architecture tests now take `.github/workflows/*.yml` as input; without it, a matrix-only edit would leave them up to date.
  - A probe that dropped `:shared:feature:planner` from the matrix failed the test and was reverted.
  - **Deviation from `testing_strategy.md` §7.1:** the green commit (`2e0a586`) is typed `ci(build)` instead of `feat`, and it has no `TDD: refactor not needed` line. History is not rewritten. The refactor of cycle 2 also tidies cycle 1's code.
- First CI run: all 29 checks passed, each Kotlin job in 1.5 to 2.5 minutes.
- **Code review (code-reviewer subagent): changes required, 10 findings.** All are addressed:
  - TDD cycle 2: `ChangedAreasScriptTest` runs the real script in throwaway git repositories.
    - Red, `73e908f`: three tests failed with `kotlin=false`. The first was a large diff with an early Kotlin path, where `grep -q` exits early, the writer gets SIGPIPE, and `pipefail` turns the match into a miss. The other two were a file moved out of `shared/` and a `fixtures/` change.
    - The red commit was pushed on purpose. On GitHub, `Architecture rules` failed and `Kotlin gate` failed with it: the gate's failure path, seen in CI.
    - Green, `6cec702`: a here-string instead of a pipe, `git diff --no-renames`, and `fixtures/` as a Kotlin path. The architecture tests now take `scripts/ci` as input.
    - Refactor, `8f65a36`: a shared `repositoryRoot`, the matrix guard limited to `:shared:` modules, and a lost space restored.
    - Test isolation, `d05ccf9`: on ubuntu-latest (git 2.55), the large-diff test passed its assertion, but JUnit could not delete its temporary repository. The likely cause is background git maintenance started by thousands of loose objects. The test repositories now set `gc.auto=0` and `maintenance.auto=false`, and the test passes on Linux.
  - The push concurrency group is per commit, so back-to-back merges are each verified rather than cancelled.
  - `ci_cd.md` §1, §2 and §4, and the structure rows of both READMEs, are updated.
- The Gradle cache now uses `cache-provider: basic`, which is MIT-licensed. The default "enhanced" provider is proprietary, under the Gradle Technologies Terms of Use (free for public repositories). Opting in is the owner's decision.
- `protect-main.sh` now also requires `Kotlin gate`.
- **Runner hiccup:** one `Build-logic tests` run hung until its 10-minute timeout, and GitHub kept no log for it. A re-run of the failed jobs passed in 1m38s. If this repeats, add test-report artefacts and look again.

**Open / next**
- **After this PR is merged:**
  - Apply `protect-main.sh`. Applying it before the merge would make PRs based on the old `main` wait for a `Kotlin gate` that never reports.
  - Confirm, on the first docs-only PR, that `Kotlin changes` reports `false`, the Kotlin jobs are skipped and `Kotlin gate` passes.
- The rest of LB-002: ktlint, detekt, Kover, the Android jobs (with LB-001 part 2), `ios-checks.yml` (with part 3), dependency review and the nightly workflow.
- The next story is LB-001 part 2 (Android).

## 2026-10-23: LB-415: repository public, `main` protected

**Done**
- Merged `main` into the LB-001 branch after PR #2, keeping both entries in the READMEs and in this log. PR #3 (LB-001 part 1) was then merged.
- Ran the pre-public checklist (`ci_cd.md` §1.1):
  - History scan of every blob reachable from any ref: 34 commits, 174 blobs, no secrets and no local paths.
  - Spec review: §3.4 (non-production hosts) and §3.3 (test payment pages) come from the public web bundle and hold no credentials.
- Made the repository public, and enabled GitHub secret scanning and push protection.
- PR #4 was the test PR: the six policy jobs ran in CI for the first time, and all passed.
- Applied `scripts/github/protect-main.sh`. The GitHub API confirms the settings:
  - `main` is protected, with a pull request required (0 approvals) and the six policy jobs as strict required checks;
  - the rules are enforced for administrators, conversations must be resolved, and force pushes and deletions are blocked;
  - linear history is not required.
  - No real direct push to `main` was attempted: if the protection had failed, that commit would have landed on `main`. The API check stands in for it.
- Updated `ci_cd.md`, `engineering_workflow.md`, `AGENTS.md`, `architecture.md`, `requirements.md`, the playbooks, the PR templates and both READMEs.

**Decisions (owner)**
- **Go public now**, ahead of the MVP, so CI runs.
- **Licence (OQ-21):** none is granted, all rights reserved. There is no `LICENSE` file; both READMEs state it.
- **Spec:** published unchanged.
- **Commit identity:** new commits use the GitHub noreply address. Older commits keep the address they were made with, because history is not rewritten.
- **LB-002 comes next, in part:** a `kotlin-checks.yml` workflow with the jobs that already have code to check. The Android and iOS jobs follow once those apps exist.

**Open / next**
- OQ-11 (data reuse) is still open. Revisit it before LB-003 commits golden fixtures to the public repository.
- The `ci-gate` job (`ci_cd.md` §4) must become a required check when LB-002 adds the path-filtered Kotlin workflow.

## 2026-10-22: Dependabot PR #2: actions/checkout v5 to v7

**Done**
- Reviewed Dependabot's bump of `actions/checkout` from v5 to v7 in `policy-checks.yml`. v7.0.0 only blocks fork checkouts on `pull_request_target` and `workflow_run`, which this repository does not use.
- Added the README, project-log and workflow commits to the Dependabot branch.
- The first docs commit claimed a project-log entry that a failed script step had not written. This commit adds it; history is not rewritten.

**Decisions**
- Dependabot PRs get no exception from the README rule. The README update is added to their branch before merging (`engineering_workflow.md` §9).

## 2026-10-22: LB-001 part 1 of 3: Gradle build, shared modules, architecture rules

**Done**
- Gradle 9.7.1 wrapper, with the distribution checksum pinned; version catalog; `build-logic` included build.
- `ModuleRules` (16 tests) enforces the ADR-0007 module dependencies at configuration time.
- `logrobus.kmp.library` convention plugin: Android KMP library plugin with host tests, `iosArm64` and `iosSimulatorArm64`, namespaces derived from the module path, the `ciUnitTest` alias.
- The 19 shared modules, with the umbrella `Shared` framework and XCFramework configured.
- `:tools:architecture-tests` (Konsist, 6 tests): exact module list, the convention plugin applied, no version literals, no cross-feature imports, clean domain imports, module namespaces.
- TDD with two cycles, each with red, green and refactor commits. The guards were proven against deliberate violations, which were then reverted.

**Decisions**
- Versions:
  - **Kotlin 2.4.20**, explicitly supported by SKIE 0.10.15 (2.4.21 is not yet).
  - **AGP 9.3.3**: KGP 2.4.20 is fully supported up to AGP 9.3.1, and 9.4 is out of range.
  - **Gradle 9.7.1**: KGP is fully supported up to 9.7.0.
  - compileSdk 37, minSdk 26, JVM toolchain 21, Android bytecode JVM 17.
- **No placeholder code** in shared modules without a test. The `Shared` framework links once the Android and iOS red tests introduce the first shared value (parts 2 and 3).
- **ktlint, detekt and Kover move to LB-002** (CI pipeline). `code_style.md` is updated.
- **Android feature modules may also depend on domain modules and shared core modules**, which is inward and clean. `architecture.md` §3.2 is updated.

**Review (code-reviewer subagent): verdict "changes required", 16 findings, all addressed**
- **TDD cycle 3 (build-logic):**
  - Dependencies are validated when declared, so RuntimeOnly buckets and late additions are covered.
  - Platform libraries are banned in domain modules.
  - Ktor and SQLDelight are limited to their core module, data modules and features.
  - New Testing kind.
  - The namespace and bucket helpers are unit-tested (23 tests).
- **TDD cycle 4 (architecture):**
  - The `ArchitectureRules` pure rules are unit-tested with good and bad samples.
  - Fixed: the umbrella false positive, multi-argument includes, every form of version literal (now also in build-logic sources), and `java.`/`javax.`/`dalvik.` in domain code.
  - New presentation-imports rule, portable paths, and an isolated-projects-safe input tree (15 tests).
- **Hygiene:**
  - The 9.7.1 wrapper jar now matches the official checksum.
  - The Android JVM target and toolchain come from the catalog.
  - Docs fixed: `AGENTS.md` status, rule 1 and scopes; the `architecture.md` dependency table; the `testing_strategy.md` heading.

**Open / next**
- LB-001 part 2: Android app and modules. Part 3: iOS app with XcodeGen and the `LogroBusUI` Swift package.

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
