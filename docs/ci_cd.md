# CI/CD

| Field | Value |
| --- | --- |
| Status | Repository public since 2026-10-23 (LB-415), with `main` protected. The policy workflow and the Kotlin jobs run on every pull request: build-logic, architecture, shared unit tests, and Android unit tests, build and lint. ktlint, detekt, Kover, dependency review and the iOS jobs are built in LB-002 |
| Last updated | 2026-10-23 |
| Related | `engineering_workflow.md`, `testing_strategy.md`, `release_process.md`, `agent_harness.md`, ADR-0006 |

## 1. Operating rule: CI runs only while the repository is public

**Owner rule (2026-10-22): CI/CD checks are not executed while the GitHub repository is private.**

- **The repository has been public since 2026-10-23** (LB-415, owner decision), so CI runs on every pull request:
  - `main` is protected on GitHub with `scripts/github/protect-main.sh` (`engineering_workflow.md` §1.1);
  - the required status checks are the policy jobs in §3 plus `Kotlin gate`, which stands for every Kotlin job (§4).
- Every workflow job keeps `if: ${{ !github.event.repository.private }}`. If the repository ever becomes private again, the jobs are skipped and the local gates become authoritative again.
- **The local gates still run before every pull request:**
  - The git hooks (`commit-msg`, `pre-commit`, `pre-push`), installed by `scripts/bootstrap.sh`.
  - `scripts/checks/run-all.sh`, whose final summary is pasted into every pull request (`.github/pull_request_template.md`). It runs the same checks before CI does, and it is the only evidence for a check that has no CI job yet.
- Every CI job runs exactly one local command (§3), so local and CI results are interchangeable. The only exceptions are the gate jobs (§4), which just read the other jobs' results.

### 1.1 Before making the repository public

The owner decides when to go public. This checklist was run on 2026-10-23, before the repository was made public:
- [x] Review the spec for content that should not be published. Spec §3.4 names non-production hosts and spec §3.3 names test payment pages left in production. Both come from the public web bundle and contain no credentials. **The owner decided to publish the spec unchanged.**
- [x] Decide the project licence (OQ-21). **No licence is granted: all rights reserved.** There is no `LICENSE` file; both READMEs state it.
- [ ] Confirm the data-reuse position (OQ-11). **Still open:** it gates the app release, and the repository holds no source data yet. Revisit it before LB-003 commits the golden fixtures to the public repository.
- [x] Confirm that no secrets exist in the git history. Every blob reachable from any ref was scanned (34 commits, 174 blobs, no findings). GitHub secret scanning and push protection are enabled.
- [x] After going public: confirm that the policy jobs run on a test PR (PR #4: all six ran and passed), then run `scripts/github/protect-main.sh` and check through the GitHub API that the protection is active.

## 2. Atomic checks: principles

1. **One concern per job.** A job answers exactly one question, such as "is the PR title valid?" or "do `:shared:data` unit tests pass?". Its job name is the status-check name.
2. **Short by design.** Target under 5 minutes per job, with a hard `timeout-minutes` of 10 at most. If a job's typical duration exceeds 5 minutes, split it: by module, by test type, by platform or by shard.
3. **Parallel and independent.** Jobs do not depend on each other unless one consumes another's artefact, such as iOS tests needing the Kotlin framework.
4. **Reproducible locally.** Each job runs a single script from `scripts/checks/` or `scripts/ci/`, or a single Gradle task. The failure message tells you the command to run locally.
5. **Relevant only.** Workflows are split by area with path filters, so a docs-only PR does not build iOS.
6. **Fail fast and cheaply.** Cheap policy checks run on Linux. macOS runners are used only for jobs that need Xcode.

## 3. Check catalogue

| Check (job name) | Workflow | Local command | Runner | Budget | Status |
| --- | --- | --- | --- | --- | --- |
| PR title | `policy-checks.yml` | `scripts/checks/check-conventional-title.sh "<title>"` | Linux | < 1 min | Active, required |
| README parity | `policy-checks.yml` | `scripts/checks/check-readme-parity.sh origin/main` | Linux | < 1 min | Active, required |
| Frozen spec | `policy-checks.yml` | `scripts/checks/check-spec-frozen.sh origin/main` | Linux | < 1 min | Active, required |
| Doc links | `policy-checks.yml` | `scripts/checks/check-doc-links.sh` | Linux | < 1 min | Active, required |
| Secrets scan | `policy-checks.yml` | `scripts/checks/check-secrets.sh --all` | Linux | < 1 min | Active, required |
| Shell scripts | `policy-checks.yml` | `shellcheck -x …` | Linux | < 1 min | Active, required |
| Kotlin changes | `kotlin-checks.yml` | `scripts/ci/changed-areas.sh origin/main` | Linux | < 1 min | Active (§4) |
| Kotlin gate | `kotlin-checks.yml` | none: it reads the results of the other Kotlin jobs, all listed in its `needs` (`lb002_kotlinGateWaitsForEveryKotlinJob`) | Linux | < 1 min | Active, required (§4) |
| Build-logic tests | `kotlin-checks.yml` | `./gradlew :build-logic:convention:test` | Linux | < 3 min | Active, through the gate |
| ktlint | `kotlin-checks.yml` | `./gradlew ktlintCheck` | Linux | < 3 min | LB-002 |
| detekt | `kotlin-checks.yml` | `./gradlew detekt` | Linux | < 4 min | LB-002 |
| Shared unit tests: `<module>` (matrix over every shared module, checked by `lb002_kotlinWorkflowRunsUnitTestsForEverySharedModule`) | `kotlin-checks.yml` | `./gradlew :shared:<module>:ciUnitTest` | Linux | < 5 min each | Active, through the gate |
| Coverage (data, domain) | `kotlin-checks.yml` | `./gradlew koverVerify` | Linux | < 5 min | LB-002 |
| Architecture rules | `kotlin-checks.yml` | `./gradlew :tools:architecture-tests:test` | Linux | < 3 min | Active, through the gate |
| Android unit tests: `<module>` (matrix over every Android module, checked by `lb002_kotlinWorkflowRunsUnitTestsForEveryAndroidModule`): ViewModel tests and Robolectric UI tests | `kotlin-checks.yml` | `./gradlew :android:<module>:ciUnitTest` | Linux | < 5 min each | Active, through the gate |
| Android lint (every Android module) | `kotlin-checks.yml` | `./gradlew lintDebug` | Linux | < 5 min | Active, through the gate |
| Android build | `kotlin-checks.yml` | `./gradlew :android:app:assembleDebug` | Linux | < 5 min | Active, through the gate |
| Android screenshots: `<feature>` (matrix) | `kotlin-checks.yml` | `./gradlew :android:feature:<feature>:verifyRoborazziDebug` | Linux | < 5 min each | LB-002 |
| Dependency review | `kotlin-checks.yml` | `actions/dependency-review-action` | Linux | < 1 min | LB-002 |
| SwiftLint | `ios-checks.yml` | `swiftlint --strict` | Linux (SwiftLint container) | < 2 min | LB-002 |
| Kotlin/Native framework | `ios-checks.yml` | `./gradlew :shared:umbrella:linkDebugFrameworkIosSimulatorArm64` | macOS | < 8 min | LB-002 |
| Shared iOS tests: `<module>` (matrix) | `ios-checks.yml` | `./gradlew :shared:<module>:iosSimulatorArm64Test` | macOS | < 5 min each | LB-002 |
| iOS build | `ios-checks.yml` | `xcodebuild build …` | macOS | < 8 min | LB-002 |
| iOS ViewModel and snapshot tests: `Feature<X>` (matrix) | `ios-checks.yml` | `swift test` / `xcodebuild test -only-testing:<Feature>Tests` | macOS | < 8 min each | LB-002 |

`ciUnitTest` is an alias task registered by the convention plugins (LB-001). It gives every module a stable task name that does not depend on Kotlin Multiplatform target names.

**Scheduled workflows**, built in the stories listed:

| Workflow | Schedule | Jobs (one per concern) | Story |
| --- | --- | --- | --- |
| `nightly.yml` | Daily | Android instrumented tests; iOS 18 fallback snapshots; Maestro critical journeys (non-blocking); OWASP Dependency-Check | LB-002, LB-412 |
| `weekly-fixtures.yml` | Weekly | Re-record fixtures and detect contract drift; opens an issue on drift | LB-003 |
| `release.yml` | On a `v*` tag | Android upload; iOS upload | M5 (`release_process.md` §7) |

## 4. Path filters and gating

| Workflow | Runs when these paths change |
| --- | --- |
| `policy-checks.yml` | Any change |
| `kotlin-checks.yml` | `shared/**`, `android/**`, `tools/**`, `build-logic/**`, `gradle/**`, `*.gradle.kts`, `gradlew*`, `gradle.properties`, `version.properties`, `fixtures/**` (the contract tests decode them), the workflow itself, `.github/actions/gradle-setup/**`, `scripts/ci/**`. Every push to `main` runs it in full |
| `ios-checks.yml` | `shared/**`, `iosApp/**`, `.swiftlint.yml`, `version.properties` |

Because `main` is protected, path-filtered workflows need a small always-running `ci-gate` job that succeeds when the filtered jobs either passed or were legitimately skipped. Only that gate job is marked as required.

`kotlin-checks.yml` implements it as follows:
- The workflow runs on every pull request. Its first job, `Kotlin changes`, runs `scripts/ci/changed-areas.sh`, the single source of the path patterns above.
- The other jobs run only when Kotlin paths changed.
- `Kotlin gate` always runs. It fails if any job failed or was cancelled, and passes when every job passed or was skipped.
- `ChangedAreasScriptTest` (in `:tools:architecture-tests`) runs the script in throwaway git repositories. A wrong `kotlin=false` would let untested Kotlin code merge, so the tests cover large diffs, moves out of Kotlin paths and fixture changes.
- `ios-checks.yml` will get its own gate in the same way.

## 5. Speed and caching

- **Gradle:** `gradle/actions/setup-gradle` with the build cache and the configuration cache enabled, through the `.github/actions/gradle-setup` composite action.
  - It uses `cache-provider: basic`, the MIT-licensed cache. The default "enhanced" provider is proprietary, under the Gradle Technologies Terms of Use; it is free for public repositories, but switching to it is the owner's decision.
  - Caches are written only on `main`, and pull requests read them.
- **Caches:** the Kotlin/Native toolchain (`~/.konan`), Swift Package Manager checkouts and Xcode DerivedData for the iOS jobs.
- **Concurrency:** `concurrency` with `cancel-in-progress: true` per pull request, so a new push cancels the stale run.
- **Monitoring:** if a job's p50 passes 5 minutes, split it in the next CI pull request and record the split in `project_log.md`.

## 6. Workflow security

- Every workflow starts with `permissions: contents: read` and widens access per job only when it needs to.
- Untrusted input, such as the PR title or branch names, reaches scripts only through `env:`. It is never interpolated into `run:`.
- First-party actions (`actions/*`) are pinned by major version. Third-party actions are pinned by commit SHA.
- No secrets in pull request workflows. Release secrets (`release_process.md` §3) exist only in `release.yml`.
- Dependabot keeps the actions updated weekly. The `gradle` and `swift` ecosystems are added after LB-001.

## 7. CD

Continuous delivery is defined in `release_process.md`. Release workflows follow the same visibility rule. Until `release.yml` exists (M5), releases are manual (`release_process.md` §5).
