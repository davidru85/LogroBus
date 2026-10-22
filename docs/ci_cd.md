# CI/CD

| Field | Value |
| --- | --- |
| Status | Policy workflow written but inactive (private repository); code workflows designed, built in LB-002; the repository goes public before the MVP is completed (LB-415) |
| Last updated | 2026-10-22 |
| Related | `engineering_workflow.md`, `testing_strategy.md`, `release_process.md`, `agent_harness.md`, ADR-0006 |

## 1. Operating rule: no CI while the repository is private

**Owner rule (2026-10-22): CI/CD checks are not executed while the GitHub repository is private.**

- Every workflow job has `if: ${{ !github.event.repository.private }}`. While the repository is private, jobs are skipped: they consume no minutes and report as skipped.
- GitHub Actions could technically run on a private repository within the monthly minutes of the Free plan. Not running it is a project decision; only the owner can change it.
- **Until the repository is public, local gates are authoritative:**
  - The git hooks (`commit-msg`, `pre-commit`, `pre-push`), installed by `scripts/bootstrap.sh`.
  - `scripts/checks/run-all.sh`, whose final summary is pasted into every pull request (`.github/pull_request_template.md`).
  - The owner checks that summary before merging.
- Every CI job runs exactly one local command (§3), so local and CI results are interchangeable. Nothing has to change on the day CI becomes active.
- **Timeline (owner decision, 2026-10-22):** the repository stays private for now and **becomes public before the MVP is completed** (LB-415). From that moment:
  - CI runs automatically, because the guard reads the repository's visibility;
  - `main` is protected on GitHub with `scripts/github/protect-main.sh` (`engineering_workflow.md` §1.1);
  - the jobs in §3 become the required status checks.

### 1.1 Before making the repository public

The owner decides when to go public. Before doing so:
- [ ] Review the spec for content that should not be published. It describes the source system and hosts found in its public bundle (spec §3.4).
- [ ] Decide the project licence (OQ-21) and add `LICENSE`.
- [ ] Confirm the data-reuse position (OQ-11).
- [ ] Confirm that no secrets exist in the git history (`scripts/checks/check-secrets.sh --all` on every commit, or a history scan).
- [ ] After going public: run `scripts/github/protect-main.sh`, then verify that a direct push to `main` is rejected and that the policy jobs run on a test PR.

## 2. Atomic checks: principles

1. **One concern per job.** A job answers exactly one question, such as "is the PR title valid?" or "do `:shared:data` unit tests pass?". Its job name is the status-check name.
2. **Short by design.** Target under 5 minutes per job, with a hard `timeout-minutes` of 10 at most. If a job's typical duration exceeds 5 minutes, split it: by module, by test type, by platform or by shard.
3. **Parallel and independent.** Jobs do not depend on each other unless one consumes another's artefact, such as iOS tests needing the Kotlin framework.
4. **Reproducible locally.** Each job runs a single script from `scripts/checks/` or a single Gradle task. The failure message tells you the command to run locally.
5. **Relevant only.** Workflows are split by area with path filters, so a docs-only PR does not build iOS.
6. **Fail fast and cheaply.** Cheap policy checks run on Linux. macOS runners are used only for jobs that need Xcode.

## 3. Check catalogue

| Check (job name) | Workflow | Local command | Runner | Budget | Status |
| --- | --- | --- | --- | --- | --- |
| PR title | `policy-checks.yml` | `scripts/checks/check-conventional-title.sh "<title>"` | Linux | < 1 min | Written, inactive while private |
| README parity | `policy-checks.yml` | `scripts/checks/check-readme-parity.sh origin/main` | Linux | < 1 min | Written, inactive while private |
| Frozen spec | `policy-checks.yml` | `scripts/checks/check-spec-frozen.sh origin/main` | Linux | < 1 min | Written, inactive while private |
| Doc links | `policy-checks.yml` | `scripts/checks/check-doc-links.sh` | Linux | < 1 min | Written, inactive while private |
| Secrets scan | `policy-checks.yml` | `scripts/checks/check-secrets.sh --all` | Linux | < 1 min | Written, inactive while private |
| Shell scripts | `policy-checks.yml` | `shellcheck -x …` | Linux | < 1 min | Written, inactive while private |
| ktlint | `kotlin-checks.yml` | `./gradlew ktlintCheck` | Linux | < 3 min | LB-002 |
| detekt | `kotlin-checks.yml` | `./gradlew detekt` | Linux | < 4 min | LB-002 |
| Shared unit tests: `<module>` (matrix over `core/*`, `transit/*`, `favourites/*`, `feature/*`) | `kotlin-checks.yml` | `./gradlew :shared:<module>:ciUnitTest` | Linux | < 5 min each | LB-002 |
| Coverage (data, domain) | `kotlin-checks.yml` | `./gradlew koverVerify` | Linux | < 5 min | LB-002 |
| Architecture rules | `kotlin-checks.yml` | `./gradlew :tools:architecture-tests:test` | Linux | < 3 min | LB-002 |
| Android ViewModel tests: `<feature>` (matrix) | `kotlin-checks.yml` | `./gradlew :android:feature:<feature>:ciUnitTest` | Linux | < 5 min each | LB-002 |
| Android lint | `kotlin-checks.yml` | `./gradlew :android:app:lintDebug` | Linux | < 5 min | LB-002 |
| Android build | `kotlin-checks.yml` | `./gradlew :android:app:assembleDebug` | Linux | < 5 min | LB-002 |
| Android screenshots: `<feature>` (matrix) | `kotlin-checks.yml` | `./gradlew :android:feature:<feature>:verifyRoborazziDebug` | Linux | < 5 min each | LB-002 |
| Dependency review | `kotlin-checks.yml` | `actions/dependency-review-action` | Linux | < 1 min | LB-002 |
| SwiftLint | `ios-checks.yml` | `swiftlint --strict` | Linux (SwiftLint container) | < 2 min | LB-002 |
| Kotlin/Native framework | `ios-checks.yml` | `./gradlew :shared:umbrella:linkDebugFrameworkIosSimulatorArm64` | macOS | < 8 min | LB-002 |
| Shared iOS tests: `<module>` (matrix) | `ios-checks.yml` | `./gradlew :shared:<module>:iosSimulatorArm64Test` | macOS | < 5 min each | LB-002 |
| iOS build | `ios-checks.yml` | `xcodebuild build …` | macOS | < 8 min | LB-002 |
| iOS ViewModel and snapshot tests: `Feature<X>` (matrix) | `ios-checks.yml` | `swift test` / `xcodebuild test -only-testing:<Feature>Tests` | macOS | < 8 min each | LB-002 |

`ciUnitTest` is an alias task registered by the convention plugins (LB-001). It gives every module a stable task name that does not depend on Kotlin Multiplatform target names.

**Scheduled workflows**, also inactive while the repository is private:

| Workflow | Schedule | Jobs (one per concern) | Story |
| --- | --- | --- | --- |
| `nightly.yml` | Daily | Android instrumented tests; iOS 18 fallback snapshots; Maestro critical journeys (non-blocking); OWASP Dependency-Check | LB-002, LB-412 |
| `weekly-fixtures.yml` | Weekly | Re-record fixtures and detect contract drift; opens an issue on drift | LB-003 |
| `release.yml` | On a `v*` tag | Android upload; iOS upload | M5 (`release_process.md` §7) |

## 4. Path filters and gating

| Workflow | Runs when these paths change |
| --- | --- |
| `policy-checks.yml` | Any change |
| `kotlin-checks.yml` | `shared/**`, `android/**`, `tools/**`, `build-logic/**`, `gradle/**`, `*.gradle.kts`, `gradle.properties`, `version.properties` |
| `ios-checks.yml` | `shared/**`, `iosApp/**`, `.swiftlint.yml`, `version.properties` |

When branch protection becomes available, path-filtered workflows need a small always-running `ci-gate` job that succeeds when the filtered jobs either passed or were legitimately skipped. Only that gate job is marked as required.

## 5. Speed and caching

- **Gradle:** `gradle/actions/setup-gradle` with the build cache and the configuration cache enabled.
- **Caches:** the Kotlin/Native toolchain (`~/.konan`), Swift Package Manager checkouts and Xcode DerivedData for the iOS jobs.
- **Concurrency:** `concurrency` with `cancel-in-progress: true` per pull request, so a new push cancels the stale run.
- **Monitoring:** if a job's p50 passes 5 minutes, split it in the next CI pull request and record the split in `project_log.md`.

## 6. Workflow security

- Every workflow starts with `permissions: contents: read` and widens access per job only when it needs to.
- Untrusted input, such as the PR title or branch names, reaches scripts only through `env:`. It is never interpolated into `run:`.
- First-party actions (`actions/*`) are pinned by major version. Third-party actions are pinned by commit SHA.
- No secrets in pull request workflows. Release secrets (`release_process.md` §3) exist only in `release.yml`.
- Dependabot keeps the actions updated weekly. It runs independently of GitHub Actions, so it keeps working while CI is inactive. The `gradle` and `swift` ecosystems are added after LB-001.

## 7. CD

Continuous delivery is defined in `release_process.md`. The same rule applies: release workflows do not run while the repository is private, so releases are manual until then (`release_process.md` §5).
