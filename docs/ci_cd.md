# CI/CD

| Field | Value |
| --- | --- |
| Status | Repository public since 2026-10-23 (LB-415), with `main` protected. The policy, Kotlin (tests, Android build and lint, ktlint, detekt, Kover, dependency review) and iOS workflows run on every pull request, behind the required gates. LB-002 is done (2026-10-25). The screenshot jobs came with LB-011 and the iOS ViewModel and snapshot jobs with LB-101; the nightly workflow follows in LB-412 |
| Last updated | 2026-11-08 |
| Related | `engineering_workflow.md`, `testing_strategy.md`, `release_process.md`, `agent_harness.md`, ADR-0006 |

## 1. Operating rule: CI runs only while the repository is public

**Owner rule (2026-10-22): CI/CD checks are not executed while the GitHub repository is private.**

- **The repository has been public since 2026-10-23** (LB-415, owner decision), so CI runs on every pull request:
  - `main` is protected on GitHub with `scripts/github/protect-main.sh` (`engineering_workflow.md` §1.1);
  - the required status checks are the policy jobs in §3, plus `Kotlin gate` and `iOS gate`, which stand for every Kotlin and iOS job (§4).
- Every workflow job keeps `if: ${{ !github.event.repository.private }}`. If the repository ever becomes private again, the jobs are skipped and the local gates become authoritative again.
- **The local gates still run before every pull request:**
  - The git hooks (`commit-msg`, `pre-commit`, `pre-push`), installed by `scripts/bootstrap.sh`.
  - `scripts/checks/run-all.sh`, whose final summary is pasted into every pull request (`.github/pull_request_template.md`). It runs the same checks before CI does, and it is the only evidence for a check that has no CI job yet.
- Every CI job that answers a check runs exactly one local command (§3), so local and CI results are interchangeable. The only exceptions are the gate jobs (§4), which just read the other jobs' results, and the `Contract drift` job of `weekly-fixtures.yml` (§5.1), which reads the failed job's log and files an issue.

### 1.1 Before making the repository public

The owner decides when to go public. This checklist was run on 2026-10-23, before the repository was made public:
- [x] Review the spec for content that should not be published. Spec §3.4 names non-production hosts and spec §3.3 names test payment pages left in production. Both come from the public web bundle and contain no credentials. **The owner decided to publish the spec unchanged.**
- [x] Decide the project licence (OQ-21). **No licence is granted: all rights reserved.** There is no `LICENSE` file; both READMEs state it.
- [ ] Confirm the data-reuse position (OQ-11). **Still open:** the 19 golden fixture captures were committed to the public repository by owner decision on 2026-11-08 (recorded in [`acceptance_criteria.md`](acceptance_criteria.md) §LB-003), and the item still gates the app release: it must be resolved before any public release of the app.
- [x] Confirm that no secrets exist in the git history. Every blob reachable from any ref was scanned (34 commits, 174 blobs, no findings). GitHub secret scanning and push protection are enabled.
- [x] After going public: confirm that the policy jobs run on a test PR (PR #4: all six ran and passed), then run `scripts/github/protect-main.sh` and check through the GitHub API that the protection is active.

## 2. Atomic checks: principles

1. **One concern per job.** A job answers exactly one question, such as "is the PR title valid?" or "do `:shared:data` unit tests pass?". Its job name is the status-check name.
2. **Short by design.** Target under 5 minutes per job, with a hard `timeout-minutes` of 10 at most. If a job's typical duration exceeds 5 minutes, split it: by module, by test type, by platform or by shard.
   - **Exception: the macOS jobs.** The Kotlin/Native toolchain and the simulator's first boot dominate them, and splitting does not shorten them (§5). Their budgets are in §3 and their timeouts in `ios-checks.yml`.
   - **`iOS build and tests` has a 10-minute budget** (owner decision, 2026-10-25), measured at 5–11 min. It has grown since: 17 to 23 minutes by LB-405, its tests alone about 10. LB-406's accessibility audits add 6 to 7 more, so its limit is 35 minutes (owner decision, 2026-11-04, §5).
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
| ktlint | `kotlin-checks.yml` | `./gradlew ktlintCheck` (ktlint 1.8.0 CLI, ADR-0011) | Linux | < 3 min | Active, through the gate |
| detekt | `kotlin-checks.yml` | `./gradlew ciDetekt` (detekt 2.0.0-alpha.6 with type resolution, ADR-0011) | Linux | < 4 min | Active, through the gate |
| Shared unit tests (`<module>`) (matrix over every shared module, checked by `lb002_kotlinWorkflowRunsUnitTestsForEverySharedModule`) | `kotlin-checks.yml` | `./gradlew :shared:<module>:ciUnitTest` | Linux | < 5 min each | Active, through the gate |
| Coverage (data, domain, feature packages) | `kotlin-checks.yml` | `./gradlew koverVerify` (Kover 0.9.11, 80 % lines, ADR-0011) | Linux | < 5 min | Active, through the gate |
| Architecture rules | `kotlin-checks.yml` | `./gradlew :tools:architecture-tests:test` | Linux | < 3 min | Active, through the gate |
| Android unit tests (`<module>`) (matrix over every Android module, checked by `lb002_kotlinWorkflowRunsUnitTestsForEveryAndroidModule`): ViewModel tests and Robolectric UI tests | `kotlin-checks.yml` | `./gradlew :android:<module>:ciUnitTest` | Linux | < 5 min each | Active, through the gate |
| Android lint (every Android module) | `kotlin-checks.yml` | `./gradlew lintDebug` | Linux | < 5 min | Active, through the gate |
| Android build | `kotlin-checks.yml` | `./gradlew :android:app:assembleDebug` | Linux | < 5 min | Active, through the gate |
| Android screenshots (`<module>`) (matrix over every Android module with reference images, checked by `lb011_kotlinWorkflowVerifiesTheScreenshotsOfEveryModuleThatHasThem`: `:android:app`, `:android:core:designsystem` and `:android:feature:settings` since LB-012, `:android:feature:lines` since LB-101, `:android:feature:nearme` since LB-201, and `:android:feature:planner` since LB-301) | `kotlin-checks.yml` | `./gradlew <module>:verifyRoborazziDebug`; on failure, the actual and diff images are uploaded | Linux | < 5 min each | Active, through the gate (LB-011) |
| Dependency graph | `kotlin-checks.yml` | `gradle/actions/dependency-submission`: resolves every Gradle configuration and submits it to GitHub's dependency graph, which cannot read Gradle build files (§6) | Linux | < 5 min | Active, through the gate |
| Dependency review | `kotlin-checks.yml` | `actions/dependency-review-action`, after the dependency graph job (`lb002_dependencyReviewWaitsForTheGradleDependencyGraph`): fails on a known critical vulnerability in an added or changed dependency, in any scope (NFR-SEC-05). It needs GitHub, so locally `./gradlew dependencies` only lists what it checks | Linux | < 4 min | Active, through the gate (pull requests only) |
| iOS changes | `ios-checks.yml` | `scripts/ci/changed-areas.sh origin/main` (area `ios`) | Linux | < 1 min | Active (§4) |
| iOS gate | `ios-checks.yml` | none: it reads the results of the other iOS jobs, all listed in its `needs` (`lb002_iosGateWaitsForEveryIosJob`) | Linux | < 1 min | Active, required after the merge (§4) |
| SwiftLint | `ios-checks.yml` | `swiftlint --strict` (CI installs the pinned Linux binary, checked by SHA-256: `scripts/ci/install-swiftlint.sh`) | Linux | < 2 min | Active, through the gate |
| Shared iOS tests | `ios-checks.yml` | `./gradlew iosSimulatorArm64Test` (one job, not a matrix: GitHub Free runs at most 5 macOS jobs at once) | macOS | < 8 min | Active, through the gate |
| iOS build and tests (includes the Kotlin/Native framework) | `ios-checks.yml` | `scripts/ios/test.sh iosAppTests iosAppUITests`, the app's own test targets: generates the project, boots the iPhone 17e simulator (iOS 26.5) while `xcodebuild build-for-testing` runs, then runs `test-without-building` (Xcode 26.6), printing each phase's time; it retries the tests once if the simulator vanishes (§5) | macOS | < 10 min (measured exception, §5) | Active, through the gate |
| iOS ViewModel and snapshot tests: one per test target of `LogroBusUI` (matrix) | `ios-checks.yml` | `scripts/ios/test.sh <Target>Tests`, which builds and runs that target alone (`-only-testing`): `DesignSystemTests`, `FeatureLinesTests`, `FeatureNearMeTests` (LB-201), `FeaturePlannerTests` (LB-301), `FeatureSettingsTests` | macOS | < 10 min each with warm caches; limit 30 min (owner decision, 2026-11-02) | Active, through the gate (LB-101; owner decision, 2026-11-02: one job per target, each rebuilding the framework, to take the package's tests out of the app's job). The app-hosted `GlassSnapshotTests` stay in the app's job. An architecture test fails when the matrix and `Package.swift`'s test targets differ |

`ciUnitTest` is an alias task registered by the convention plugins (LB-001). It gives every module a stable task name that does not depend on Kotlin Multiplatform target names.

**Scheduled workflows**, built in the stories listed:

| Workflow | Schedule | Jobs (one per concern) | Story |
| --- | --- | --- | --- |
| `nightly.yml` | Daily | Android instrumented tests; iOS 18 fallback snapshots; Maestro critical journeys (non-blocking). Created when the first of them exists. No OWASP Dependency-Check: the dependency review and Dependabot alerts are the NFR-SEC-05 equivalent (§6, owner decision 2026-10-25) | LB-412, or the first instrumented test |
| `weekly-fixtures.yml` | Weekly, Mondays 04:17 UTC (also `workflow_dispatch`) | `Fixtures` re-records every committed fixture and compares its shape with the source (`scripts/record-fixtures.sh --check`); `Contract drift` opens one issue per ISO week when the check fails (§5.1) | LB-003 |
| `release.yml` | On a `v*` tag | Android upload; iOS upload | M5 (`release_process.md` §7) |

## 4. Path filters and gating

| Workflow | Runs when these paths change |
| --- | --- |
| `policy-checks.yml` | Any change |
| `kotlin-checks.yml` | `shared/**`, `android/**`, `tools/**`, `build-logic/**`, `gradle/**`, `*.gradle.kts`, `gradlew*`, `gradle.properties`, `version.properties`, `fixtures/**` (the contract tests decode them), `config/**` (detekt) and `.editorconfig` (ktlint), `iosApp/project.yml`, `iosApp/Packages/*/Package.swift`, `.gitignore` and `ios-checks.yml` (the architecture tests read them), every Swift source and string catalogue under `iosApp/` (the localisation tests read every iOS target's, LB-405), the workflow itself, `.github/actions/gradle-setup/**`, `scripts/ci/**`. Every push to `main` runs it in full |
| `ios-checks.yml` | `iosApp/**`, `.swiftlint.yml`, `shared/**`, the Gradle build (`gradle/**`, `build-logic/**`, `*.gradle.kts`, `gradlew*`, `gradle.properties`), `version.properties`, `scripts/ios/**`, `scripts/ci/**`, the workflow itself, `.github/actions/gradle-setup/**`. Every push to `main` runs it in full |

Because `main` is protected, path-filtered workflows need a small always-running `ci-gate` job that succeeds when the filtered jobs either passed or were legitimately skipped. Only that gate job is marked as required.

`kotlin-checks.yml` implements it as follows:
- The workflow runs on every pull request. Its first job, `Kotlin changes`, runs `scripts/ci/changed-areas.sh`, the single source of the path patterns above.
- The other jobs run only when Kotlin paths changed.
- `Kotlin gate` always runs. It fails if any job failed or was cancelled, and passes when every job passed or was skipped.
- `ChangedAreasScriptTest` (in `:tools:architecture-tests`) runs the script in throwaway git repositories. A wrong `kotlin=false` would let untested Kotlin code merge, so the tests cover large diffs, moves out of Kotlin paths and fixture changes.
- On a PR that touches no Kotlin path, such as a docs-only PR, the other Kotlin jobs show as skipped. That is expected: only the gate is required, and it passes. This was first seen on PR #8.
- Job names are plain text, because GitHub shows a skipped job's name unexpanded. Matrix jobs get their values appended automatically, for example "Shared unit tests (:shared:core:common)". `lb002_kotlinJobNamesReadWellWhenSkipped` enforces this.
- `ios-checks.yml` has its own gate, `iOS gate`, built the same way on the `ios` area.

## 5. Speed and caching

- **Gradle:** `gradle/actions/setup-gradle` with the build cache and the configuration cache enabled, through the `.github/actions/gradle-setup` composite action.
  - It uses `cache-provider: basic`, the MIT-licensed cache. The default "enhanced" provider is proprietary, under the Gradle Technologies Terms of Use; it is free for public repositories, but switching to it is the owner's decision.
  - Caches are written only on `main`, and pull requests read them.
- **Caches** (measured on 2026-10-25, LB-002):
  - **Gradle User Home.** The basic provider keys it on a hash of the Gradle files (`setup-java-<os>-gradle-<hash>`), with no fallback key.
    - A pull request that changes a build script, the catalog or `version.properties` therefore starts cold: some thirty jobs download every dependency at once, which on 2026-11-02 met Maven Central's 429s and the Plugin Portal's "not found" in a different job on almost every run (PR #38, `scripts/ci/gradle-retry.sh` below).
    - On `main`, the parallel jobs race to save the same key. One saves, and the others log "save failed", which is harmless.
  - **Kotlin/Native (`~/.konan`): not cached.** It takes 1.2 GB per Kotlin version once extracted, and restoring it would take about as long as downloading it. It would also crowd the 10 GB cache quota, where each Gradle cache already takes 0.5–0.8 GB.
  - **Xcode DerivedData and Swift packages: not cached.** The app and its tests build in seconds, and there are no remote packages yet. Revisit this with the first remote package (LB-013).
- **Simulator boot.** On a fresh macOS runner, the simulator's first boot took most of the 3–8 minutes that `xcodebuild test` used. `scripts/ios/test.sh` prints each phase's time (`TIME:` lines). Two variants were measured on CI (2026-10-25):

  | Variant | Job total | Gradle | App and tests build | Tests |
  | --- | --- | --- | --- | --- |
  | Before: `xcodebuild test` builds, boots and tests (8 runs) | median about 550 s (301–716) | 103–193 s, with no cache | 169–496 s for build, boot and tests together | |
  | Boot in parallel with Gradle (2 runs) | 458–516 s, mean 487 s | 225–257 s | 46–75 s | 91–135 s |
  | **Boot in parallel with the app build (kept, 3 runs)** | 319–648 s, mean 498 s | **42–56 s** | 105–359 s | 121–154 s |

  - **Why the boot cannot be hidden for free:** on the 3-core runner, the boot slows whichever phase it overlaps.
  - **Neither variant is reliably faster:** the runner's spread, from 319 to 648 s, is larger than the difference between them. Against the median of about 550 s from before, the gain is small and uncertain.
  - **Why boot during the app build:** Gradle, the phase that reports Kotlin errors, stays under a minute with its cache, instead of being slowed by the boot.
  - **In both variants the tests never waited for the boot.** Besides the boot's contention, the rest of the time goes to the tests' first launches on the newly booted simulator.
  - **Splitting the job would not shorten it:** each part would need its own macOS runner, Gradle build and boot, and GitHub Free allows only 5 macOS jobs at once.
  - **Its budget is therefore 10 minutes** (owner decision, 2026-10-25, §2). The launch UI tests stay on every pull request.
  - **The accessibility audits (LB-406, 2026-11-04):** `AccessibilityAuditUITests` walks every screen at the default and the largest text, and again in dark mode, about 3.5 minutes locally and 6 to 7 on CI's runner, which runs these tests about twice as slowly. With the job at 17 to 23 minutes, its limit went from 25 to 35 minutes (owner decision, 2026-11-04), rather than a job of its own, which would build the framework and the app again on a sixth macOS runner, or a nightly run, which would find a regression a day late.
  - **The package's tests left it (LB-101, 2026-11-02):** it took 19 minutes on PR #35. Since LB-101, it runs only the app's test targets, and each test target of `LogroBusUI` runs in a matrix job of its own (§3). Each of those jobs builds the framework again: with the app's job and the shared iOS tests, a pull request uses 5 macOS runners at once, the most that GitHub Free allows.
- **Concurrency:** `concurrency` with `cancel-in-progress: true` per pull request, so a new push cancels the stale run.
- **Monitoring:** if a job's p50 passes 5 minutes, split it in the next CI pull request and record the split in `project_log.md`.
- **Runner flakes:** the agent watching a pull request reruns a failure outside the change once (`AGENTS.md` §10). One that recurs is made harmless in the pipeline:
  - **A vanishing simulator** (PRs #21, #28 and #33): after a clean build, `xcodebuild test-without-building` found no device (exit code 70), and a rerun passed. Since PR #33, `scripts/ios/test.sh` then waits up to 2 minutes for the simulator to come back, boots it and retries the tests once. Any other failure, such as a failing test, fails at once.
  - **The wrapper's own download** (PR #54, 2026-11-08, twice in a row): every Kotlin job died at about 20 s with `Downloading from https://services.gradle.org/distributions/gradle-9.7.1-bin.zip failed: timeout (10000ms)`. A fresh runner has an empty `GRADLE_USER_HOME` and thirty jobs asked for the distribution at once; `gradle-wrapper.properties` allowed a 10 s read timeout and no retry, and `scripts/ci/gradle-retry.sh` cannot help because `./gradlew` itself is what failed. The wrapper now waits up to 120 s and retries four times, 2 s apart, and `lb003_theWrapperRetriesItsOwnDownload` keeps both floors. The distribution checksum stays pinned, so a retried download is still verified.
  - **A rate-limited download** (PR #36 and `main`, 2026-11-02): Maven Central answered `429 Too Many Requests` for a Robolectric jar while many jobs downloaded at once with an empty Gradle cache, after a dependency change. Gradle retries 408, 429 and 5xx answers, by default 3 times from 1 s, about 3 s in all. Since PR #37, `gradle.properties` gives it 6 tentatives from 2 s, about a minute (`org.gradle.internal.repository.max.tentatives` and `initial.backoff`, internal Gradle properties checked against Gradle 9.7.1; `ArchitectureTest` keeps their floors). Other answers, such as a 404, still fail at once.
  - **Kotlin's artifacts "not found" on the Gradle Plugin Portal** (PR #36 and PR #38, 2026-11-02): configuring build-logic, one job could not find `kotlin-stdlib:2.4.0` at `plugins.gradle.org`, which every other job of the run resolved. build-logic's settings had no `pluginManagement`, so its plugins (`kotlin-dsl`, and with it Kotlin) came from the Portal alone, which only proxies Maven Central for them. Since PR #38, build-logic lists `mavenCentral()` before `gradlePluginPortal()`, as the root settings do, and `ArchitectureTest` keeps both in that order.
  - **A dependency a repository did not serve, in any job** (PR #36 and PR #38, 2026-11-02): the Portal then answered "not found" for `kotlin-dsl`'s plugin marker too, which only it serves, and Maven Central answered 429 for longer than Gradle's own retries above. Pull request jobs restore no Gradle cache ("no match found"), so some thirty jobs download everything at once. Since PR #38, every Gradle step of the workflows, and `scripts/ios/generate-project.sh`, runs through `scripts/ci/gradle-retry.sh`: when Gradle failed only because a repository did not serve a dependency (a "not found", a 429 or a 5xx), it runs Gradle again, 60 s later, up to three runs in all, with `--refresh-dependencies` so that Gradle asks again for a file it remembered as missing. A "No matching variant" error counts too: it follows a module metadata file that was not served, when Gradle falls back to a POM whose artifacts for other platforms match no variant. A failing test or a compile error fails at once. `GradleRetryScriptTest` pins both, and `ArchitectureTest` keeps every workflow step going through the script.
  - **macOS runners that are slow or never come** (PR #37 and PR #38, 2026-11-02): with five macOS jobs at once, one matrix job built the framework and the app in 18 minutes with cold caches and reached its 20-minute limit; twice, GitHub never assigned a job a runner ("not acquired … capacity constraints" on macOS arm64). The matrix jobs' limit is 30 minutes since PR #38 (owner decision). A job that gets no runner cannot be prevented from the pipeline: rerun it.
- **Runner image changes** are not flakes: a rerun fails the same way, so the pipeline is fixed in the pull request that meets them.
  - **Ubuntu 26.04** (PR #34, 2026-11-02): `ubuntu-latest` moved to it, and SwiftLint 0.65.1's Linux binary needs `libxml2.so.2`, which it no longer ships. The SwiftLint job pins `ubuntu-24.04` until a SwiftLint release runs on 26.04. `scripts/ci/install-swiftlint.sh` now fails when the binary cannot run, instead of reporting it installed.

### 5.1 The weekly fixture check (`weekly-fixtures.yml`, LB-003)

- **What it does.** Every Monday at 04:17 UTC, and on demand through `workflow_dispatch`, the `Fixtures` job records every fixture of spec §10.2, plus `sections.json` (API-12's fifth path, which the spec's list omits) and the two CartoCiudad captures, 19 in all, into a temporary directory, with `scripts/record-fixtures.sh --check`. It compares the JSON *shape* of each capture, the key paths and their types, with the committed file in `fixtures/`, and fails when a field was added, removed or changed type.
- **Why the shape and not the bytes.** The live endpoints answer with new values every minute: ETAs, timestamps and vehicle positions. A byte diff would fail on every run, while the shape is the part that the app's contract tests rely on (risk R1, [`api_spec.md`](api_spec.md) §9, [`testing_strategy.md`](testing_strategy.md) §4.3).
- **How it calls the source.** At most one request per second, the project `User-Agent` and `Accept-Encoding: gzip`, following spec §8.7 and risk R2. Each fixture takes one request, so a run takes about 25 s.
- **Why `timeout-minutes: 10`.** The run itself takes about 25 s; the limit caps a run that hangs on a slow endpoint (the requests time out at 10 s, the static ones at 30 s) and stays within §2's hard limit. The `Contract drift` job needs 3 minutes: it only calls `gh`, to download one job log and to file the issue.
- **On drift it opens an issue and blocks nothing.** The `Contract drift` job runs only when `Fixtures` failed, whether a shape changed, a committed fixture is missing or an endpoint did not answer; the lines the check printed say which. It opens one issue per ISO week, titled `Contract drift: weekly fixtures <year>-W<week>`, unless an issue with that exact title is already open, so a retried run does not open a second one. The issue carries the failing run's URL, an excerpt of the failed job's log (the checker's lines), the link to [`playbooks/record-fixtures.md`](playbooks/record-fixtures.md), and the reminder that a shape change is risk R1 and belongs in [`api_spec.md`](api_spec.md) §10. The `Fixtures` job passes nothing on: it runs `scripts/record-fixtures.sh --check` and fails with it, and the second job reads the log itself, because those lines quote the source's own JSON key names and untrusted text must never reach `$GITHUB_OUTPUT`, where a key holding the output's heredoc delimiter would let the rest of the run's output be parsed as further outputs. `gh run view --log-failed` refuses while the run is in progress, so the log comes from the jobs API for the run's failed jobs. Before it reaches the issue body the excerpt is sanitised: ANSI escapes and `\r` stripped, any run of three or more backticks replaced with `'''`, a line that is only `RECORD_FIXTURES_REPORT` replaced with a note, and the last 200 lines and 16 000 bytes kept, headed by a line saying how many earlier lines were omitted. It is then indented four spaces rather than fenced, so no text of the source can close a code block and inject markup into a public issue. The workflow is not part of any gate (§3–4), so a drift never blocks a merge. `Contract drift` only reports: it runs no project code, just `gh` against the API.
- **It never commits.** `--check` writes nothing and no step commits: a human or an agent re-records, reviews the diff and opens the pull request, as the playbook says.
- **Security.** The workflow needs no secret. `Contract drift` is the only job outside a pull request with a write permission (`issues: write` is the only write scope; `contents: read` and `actions: read` let it read the failed job's log, §6). No untrusted text crosses a job output or a `run:` interpolation: the fixtures job hands the second job nothing but its failure, and the log the second job downloads is treated as data in the shell, sanitised, and indented into the issue body.

## 6. Workflow security

- Every workflow starts with `permissions: contents: read` and widens access per job only when it needs to.
- Untrusted input, such as the PR title or branch names, reaches scripts only through `env:`. It is never interpolated into `run:`.
- First-party actions (`actions/*`) are pinned by major version. Third-party actions are pinned by commit SHA.
- No secrets in pull request workflows. Release secrets (`release_process.md` §3) exist only in `release.yml`.
- **One write permission in pull request workflows:** the `Dependency graph` job has `contents: write`, which submitting to the dependency graph needs. It runs only the Gradle build, as every Kotlin job does. Pull requests from forks get a read-only token, so for them the job fails and the gate blocks the merge.
- **Dependency scanning (NFR-SEC-05):**
  - The dependency graph and Dependabot alerts are enabled (2026-10-24, owner decision). Dependabot security update PRs are not.
  - **The equivalent that NFR-SEC-05 allows:** the dependency review on pull requests, plus Dependabot alerts on `main`, stand in for OWASP Dependency-Check, which was dropped from the plan (owner decision, 2026-10-25). Without an NVD API key it is slow, and with one it needs a repository secret.
  - Pushes to `main` submit the Gradle graph that pull requests are compared against. GitHub reads the actions in the workflows by itself.
  - The dependency review runs on pull requests that touch the Kotlin area, which holds every Gradle dependency. A pull request that only changes the actions of `ios-checks.yml` or `policy-checks.yml` is not reviewed: Dependabot alerts cover those actions once they reach `main`.
  - **Security floors for the build's tools and tests.** The first reviews found three libraries with critical advisories in AGP 9.3's tools. The Dependabot alerts of 2026-11-02 found seven more, with high or moderate advisories, in the build's tools and tests; six of them get a floor. None of them reaches the app.
    | Library | Found | Floor | Where it comes from |
    | --- | --- | --- | --- |
    | Netty | 4.1.93 and 4.1.110 (GHSA-c4c3-7fpv-j4q5) | 4.1.139.Final | the instrumented test runner (`unified-test-platform-*`) |
    | Bouncy Castle | 1.79 (GHSA-574f-3g2m-x479, GHSA-9pwp-9qqc-pr26) | 1.85 | Android lint (`androidLintTool`), the test runner and AGP's plugin classpath |
    | FreeMarker | 2.3.32 (GHSA-27j2-h3m2-8237) | 2.3.35 | AGP's plugin classpath |
    | Guava | 32.0.1, 33.4.0 and 33.6.0 (GHSA-xxph-c9ww-hj94) | 33.7.2-jre | Android lint, the test runner, AGP's plugin classpath, and Robolectric in the unit and host tests and in `:android:core:testing` |
    | OpenTelemetry API | 1.41.0 (GHSA-rcgg-9c38-7xpx, GHSA-ccrj-gw8r-854m) | 1.67.0 | KGP's Swift export (`swiftExportClasspath`), which no build of ours runs |
    | Commons Lang | 3.16.0 (GHSA-j288-q9x7-2f5v) | 3.18.0 | Android lint, the test runner and AGP's plugin classpath |
    | HttpClient | 4.5.6 (GHSA-7r82-7xv7-xcpj) | 4.5.14 (fixed in 4.5.13; 4.5.14 is the last 4.5, which AGP's plugin classpath already uses) | Android lint and the test runner |
    | jose4j | 0.9.5 (GHSA-3677-xxcr-wjqv) | 0.9.6 | AGP's plugin classpath, through bundletool |
    | JDOM | 2.0.6 (GHSA-2363-cqg2-863c) | 2.0.6.1 | AGP's plugin classpath, through Jetifier |

    The fixed versions are catalog entries, under one comment that explains the floors:
    - in each module, the Android and KMP conventions add them to AGP's tool configurations, KGP's Swift export classpath and the test dependencies (`testImplementation`, `androidHostTestImplementation`), never to the app's own classpaths (`SecurityFloors.kt`);
      - a test-support module, which only tests may use (`ModuleRules`), also gets them on its own dependencies, which its compile and runtime classpaths extend: `implementation` in an Android module, `androidMainImplementation` in a KMP one, whose common and iOS configurations get none. Its own tests' classpaths get them as test classpaths, or through the module's variant;
      - `:android:core:testing` (LB-406) brings Robolectric as a dependency of its own: its Guava 33.6.0 reopened the Guava alert on 2026-11-04 (UTC), and these floors close it;
      - Netty, Bouncy Castle and Guava come as BOMs, the others as plain constraints;
      - the Swift export classpath gets no BOM. KGP adds its tools there as default dependencies, which apply only while the configuration declares none, so a BOM would remove them instead of raising them;
    - the root `build.gradle.kts` raises Bouncy Castle, FreeMarker, Guava, Commons Lang, jose4j and JDOM on the plugin classpath.

    The floors are constraints, so a newer version from a tool always wins. **On every AGP, Kotlin or Robolectric upgrade:**
    - drop the floors that the tool has caught up with;
    - query the advisories for every resolved coordinate, as the PR #12 log entry describes.
  - **One alert has no floor:** Kotlin Gradle plugin 2.4.0 (GHSA-r937-wjx7-w2jp, moderate, fixed in 2.4.20-Beta1).
    - It is the Kotlin that Gradle 9.7.1 embeds, which `kotlin-dsl` uses to compile build-logic. The modules use KGP 2.4.20.
    - Gradle 9.8 embeds Kotlin 2.4.10, which is still affected.
    - A constraint raises it, but then `kotlin-dsl` runs outside the Kotlin version it is built for, and every build prints "Unsupported Kotlin plugin version". It is therefore not raised.
    - The alert closes with the first Gradle release that embeds Kotlin 2.4.20 or later.
  - The first external Swift dependency is swift-snapshot-testing (LB-011, tests only). `iosApp/Package.resolved` pins it and its transitive packages.
    - `scripts/ios/generate-project.sh` copies the file into the generated workspace, and Xcode keeps those versions while they satisfy the requirements.
    - Builds do not pass `-onlyUsePackageVersionsFromResolvedFile`. The file's `originHash` differs between Xcode versions, and with that flag CI's Xcode 26.6 rejected a file written by Xcode 27.1 as out of date.
    - After a deliberate update, copy the generated file back into `iosApp/`.
    - An architecture test keeps the version pinned in `Package.swift` and in `project.yml` the same.
- Dependabot keeps the actions updated weekly. The `gradle` and `swift` ecosystems are added after LB-001.

## 7. CD

Continuous delivery is defined in `release_process.md`. Release workflows follow the same visibility rule. Until `release.yml` exists (M5), releases are manual (`release_process.md` §5).
