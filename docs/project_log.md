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

## 2026-10-26: LB-014: ADRs for the spec §8.3 choices

**Done**
- This was started while the owner was away, because writing the ADRs needed no owner decision. The branch is stacked on LB-004's, to avoid conflicts in this log and the READMEs.
- **Seven ADRs, all Proposed** (NFR-MNT-03). Each names its rejected alternatives with reasons:
  - **ADR-0012, Ktor and kotlinx.serialization.** Rejected: platform clients, which would duplicate the lenient decoding; Ktorfit; JVM-only JSON libraries.
  - **ADR-0013, SQLDelight.** Rejected: Room KMP (an annotated-entity model, and KSP on every target); Realm (deprecated); files.
  - **ADR-0014, Koin, with a graph-verification test.** Rejected: kotlin-inject and Metro (compile-time plugins); manual DI; Hilt.
  - **ADR-0015, `kotlin.time` for instants and kotlinx-datetime for `Europe/Madrid`.** Rejected: `expect`/`actual` over platform APIs; fixed offsets.
  - **ADR-0016, Coil 3 on Android and `AsyncImage` on iOS.** This refines spec §8.3, which names Coil 3 for both: its multiplatform support renders through Compose, which ADR-0001 rules out on iOS. Owner to confirm.
  - **ADR-0017, MapLibre Native on both platforms.** Rejected: Google Maps with MapKit; Mapbox; a web map. The tile provider stays with OQ-10.
  - **ADR-0018, Multiplatform Settings.** Rejected: DataStore KMP used directly; a settings table; `expect`/`actual`.
- **No dependency is added.** Each library enters the catalog with the first story that uses it.
- **Linked from** the ADR index and from `architecture.md` (the dependency rules and the cross-cutting table).

**Open / next**
- **Owner:** accept or amend ADR-0012 to ADR-0018. ADR-0016 deviates from spec §8.3.
- **LB-014 stays in progress** until OQ-16 picks the analytics and crash backend and its ADR is written.

## 2026-10-26: LB-004, part 1: core common and config

**Context**
- The owner was away for about 10 hours and asked the agent to carry on without questions.
- **Decisions the docs leave open:** the agent took the documented fallback or the most conservative option, and marked each one below for the owner to confirm.
- **Merges:** none happened while the owner was away, because only the owner merges.

**Agent decisions, to be confirmed by the owner**
- **Split LB-004 in two, as was done for LB-001.**
  - **Part 1 (this PR):** exactly the acceptance criteria, using only the Kotlin standard library.
  - **Part 2:** `Logger`, `AppDispatchers`, `AppLifecycle` and `LocationProvider`.
    - They need kotlinx-coroutines.
    - `LocationProvider`'s Android implementation needs Google Play Services Location (`architecture.md` §6). That is a third-party SDK, which needs the owner's approval (`AGENTS.md` §10).
  - LB-004 stays in progress.
- **Defaults for keys whose values the docs leave open** (`api_spec.md` §8):
  - `night_service_text`: the FR-STOP-06 sentence, in es and en (OQ-08).
  - `fare_tier_texts`: neutral "20 % / 50 % discount" texts, with no eligibility claims (OQ-09).
  - `map_style_url`: empty. Choosing a tile provider is a licensing matter for the owner (OQ-10), so none is set here.

**Done**
- **TDD:** seven red-green cycles, plus two refactors.
- **Every LB-004 acceptance criterion has a test:**
  - `AppError` (`Network`, `Server(status)`, `Data`, `NotFound`) and `AppResult`, matched exhaustively.
  - `AppClock` (a `fun interface`) and `ServerCorrectedClock`: the device clock plus the median of the last 5 `Date` offsets. A device clock 90 s behind the server is corrected, and before the first sample the device clock is used as is.
  - `RemoteConfigKey`, a sealed catalogue of the 14 keys with their compiled defaults, and `CompiledDefaultsRemoteConfig` (NFR-REL-05).
  - `pollInterval`, which clamps intervals under 10 000 ms to 10 s.
  - Geo, in `:shared:core:common`, package `geo`:
    - `GeoPoint.distanceMetersTo` (Haversine) is within 0.15 % of WGS84 geodesic references computed with Vincenty's formula.
    - `Polyline` round-trips Google's example at precision 5 and a Logroño path at precision 6.
    - `BoundingBox.expandedBy` and `around` are tested at 2 km and 5 km.
- **Found by the tests:** a JVM class-initialisation cycle.
  - When `PollIntervalTest` ran first, `RemoteConfigKey.all` held `null`.
  - The list is now built lazily.
- **Review:** the code-reviewer and data-rules-reviewer subagents both approved with minor findings. These were fixed with TDD:
  - **Median for the clock offset.** Under the mean, one stale `Date` (a response cached by a proxy) moved the offset from +90 s to −10 min 30 s for four samples.
  - **Malformed polylines** now raise a documented `IllegalArgumentException` instead of `StringIndexOutOfBoundsException`, and precision is limited to 0..10.
  - **`GeoPoint` and `BoundingBox`** print `(redacted)` in `toString` (NFR-PRIV-02), and negative box distances are rejected.
  - **Refactor:** `AppResult` is a sealed class, so Swift keeps its generic; `serverOffset` is internal; the box test is checked to 0.5 %; an exhaustive `when` guards the key catalogue.
  - **Not applied:**
    - The `asin` NaN guard: in 2 million random antipodal pairs, `sqrt(h)` never exceeded 1, so no test could prove it.
    - Re-clamping after jitter: it belongs to the `PollingEngine` and is noted on LB-009.
  - **Recorded in `api_spec.md` §8:** line ETAs reuse `poll_arrivals_ms`.
- **Verified:**
  - 21 common tests and 6 config tests pass on the Android host, and both modules pass on the iOS simulator;
  - ktlint and detekt are clean;
  - `run-all.sh` passes.

**Open / next**
- **Owner:** confirm the three defaults above, and decide on Play Services Location for part 2.
- **`TestAppClock`** (`:shared:core:testing`) arrives with the first test that needs a settable clock, likely in part 2 or LB-005.
- **LB-005** feeds `ServerCorrectedClock.onServerDate` from every BFF response.

## 2026-10-25: LB-002 done: CI failure probes

**Decisions (owner)**
- **LB-002 is closed.** Its planned work without code yet moves to the stories that will have that code:
  - the Android screenshot and iOS snapshot jobs, to LB-011;
  - the iOS ViewModel test job, to LB-101;
  - the nightly workflow, to LB-412, unless an instrumented test needs it first.
  - Their acceptance criteria now say so.

**Done**
- PR #13 merged, and the merge was detected by the watcher. The probes were the task named in its hand-over.
- **The method:** three throwaway draft PRs from `main`, with one deliberate fault each.
  - Each fault was first seen failing locally.
  - Each PR's result was posted as a PR comment, the PR was closed unmerged and its branch deleted.
  - The `pre-push` hook runs no tests, so no hook was bypassed.
- **#14, a regression** (`TopLevelDestination.start = NEAR_ME`) failed every test layer:
  - the shared tests on the Android host (`lb012_theAppOpensOnLines`, `lb001_swiftEntryPointExposesTheStartDestination`);
  - `Shared iOS tests`, on the simulator;
  - Robolectric (`lb001_appLaunchesOnTheLinesTab`, plus the Spanish variant);
  - XCUITest (`test_lb001_launchesOnLines`, plus the Spanish variant).
  - `Coverage` failed too, because `koverVerify` runs the tests first.
  - Both gates failed.
- **#15, a ktlint violation** (2-space indentation): `ktlint` failed with "Unexpected indentation", and so did the Kotlin gate.
- **#16, a coverage drop** (an untested object in `:shared:core:presentation`): `Coverage` failed with 45.45 % against the 80 % rule, and so did the Kotlin gate.
- `README parity` failed in all three, as expected, because a probe does not update the docs.
- **Every LB-002 acceptance criterion is now met.** `acceptance_criteria.md` records the evidence.

**Open / next**
- E0 continues with the data layer:
  - LB-003, the golden fixtures, which needs OQ-11 (data reuse) settled before fixtures are committed to the public repository (`ci_cd.md` §1.1);
  - LB-004, the core common, config and location modules.

## 2026-10-25: LB-002: faster iOS app job

**Decisions (owner)**
- **`iOS build and tests` gets a 10-minute budget,** as a measured exception to the 5-minute rule (`ci_cd.md` §2). The launch UI tests stay on every pull request.
- **OWASP Dependency-Check is dropped.** The dependency review on pull requests and Dependabot alerts on `main` are the NFR-SEC-05 equivalent. The nightly workflow will be created when its first job exists: Android instrumented tests, iOS 18 fallback snapshots or Maestro.
- **Throwaway draft PRs approved** for the last LB-002 criterion: a failing test, a lint violation and a coverage drop each fail CI. They are the next task after this PR.

**Done**
- First task started under the new rule: the agent detected PR #12's merge on its own and went on with the task named in the hand-over.
- **Measured before changing anything.** The last 8 successful runs of `iOS build and tests` took 5 to 12 minutes, against an 8-minute budget.
  - Gradle and the XCFramework took about 3 minutes.
  - XcodeGen took 16 s.
  - `xcodebuild test` took 169–496 s, and almost all of it went before "Testing started" completed: the simulator's first boot on a fresh runner.
- **Gradle cache.**
  - **Restores:** none of those runs restored it. The basic provider keys it on a hash of the Gradle files, with no fallback key, and every recent LB-002 PR changed them.
  - **Saves:** on `main`, the parallel jobs race for one key. One saves it, which `gh cache list` confirms, and the others log "save failed".
  - **So** pull requests that leave the Gradle files alone get the cache.
- **`scripts/ios/test.sh`** now:
  - looks up the simulator's UDID (`IOS_SIMULATOR` and `IOS_SIMULATOR_OS` replace `IOS_DESTINATION`);
  - generates the project, then starts `simctl bootstatus -b` in the background;
  - runs `build-for-testing`, waits for the boot, then runs `test-without-building`, printing each phase's time.
  - Locally, from a shut-down simulator, it passed in 38 s, and an unknown simulator fails with a hint.
- **Two variants, measured on CI** (table in `ci_cd.md` §5):
  - **Boot in parallel with Gradle:** 516 s and 458 s. The boot was fully hidden, but it slowed Gradle to 225–257 s.
  - **Boot in parallel with the app build (kept):** 319, 526 and 648 s. Gradle took 42–56 s, the build absorbed the boot, and the tests never waited.
  - **Before,** the median was about 550 s.
  - **Result:** neither variant is reliably faster, and the gain over the old median is small and uncertain. On the 3-core runner, the simulator's first boot costs minutes wherever it runs.
  - **Correction:** after two runs, the agent wrote that the kept variant had the better mean. A third run (648 s) disproved it, and the docs were corrected.
  - **What the PR does deliver:** the per-phase `TIME:` lines, a clear failure for a missing simulator, and a short Gradle phase.
- **Not cached, on purpose:**
  - `~/.konan`: 1.2 GB per Kotlin version, about as slow to restore as to download, and it would crowd the 10 GB cache quota;
  - DerivedData: the app builds in seconds.
  - `ci_cd.md` §5 records both decisions.

**Open / next**
- **Budget:** decided above (10 minutes). Revisit it if the `TIME:` lines show the median drifting past it.
- Rest of LB-002:
  - next: the throwaway draft PRs (approved);
  - then: the nightly workflow, once it has a job, and the iOS snapshot and ViewModel tests, which have no code to test before E1.

## 2026-10-24: LB-002: dependency graph and dependency review

**Decisions (owner)**
- **ADR-0011 accepted** after PR #11 merged: the ktlint CLI and the Kover scope, on top of the detekt version the owner had already chosen.
- **Merges are no longer announced** (2026-10-25). The agent watches the pull request it opened. Once it is merged, the agent syncs `main` and goes on with the next task. On a close without merge, requested changes or new comments, it stops and reports. Recorded in `AGENTS.md` §10, `engineering_workflow.md` §5 and `playbooks/finish-story.md` step 9.
- **Dependabot alerts enabled**, which also enables the dependency graph that the dependency review needs. Before this, the repository's dependency graph was off: its SBOM endpoint returned 404. The agent enabled it through the API (`PUT /repos/…/vulnerability-alerts`) with the owner's approval. Dependabot security update PRs stay off.

**Done**
- TDD:
  - **red:** `lb002_dependencyReviewWaitsForTheGradleDependencyGraph`;
  - **green:** `ArchitectureRules.jobNeeds`, then the two jobs.
- GitHub cannot read Gradle build files. A new `Dependency graph` job therefore runs `gradle/actions/dependency-submission`:
  - it is pinned by SHA (v6.4.0), uses the basic cache and validates the wrapper;
  - it resolves every configuration and submits the graph;
  - it runs on Kotlin-area pull requests and on every push to `main`;
  - it is the only job with `contents: write`.
- `Dependency review` (`actions/dependency-review-action@v5`):
  - it needs the graph job, and waits for GitHub to process the snapshot;
  - it fails on a known critical vulnerability in any scope.
  - The Kotlin gate needs both jobs.
- **The first review found a real issue.**
  - **The finding:** GHSA-c4c3-7fpv-j4q5 (critical) in Netty 4.1.93. A probe of every configuration traced it, and 4.1.110, to gRPC in AGP's Unified Test Platform (`unified-test-platform-*`), the runner for instrumented tests. Netty never reaches the app.
  - **Exposure:** the flaw is an SNI-routing bypass in TLS servers, and here Netty only talks to a local emulator, so practical exposure is low.
  - **The fix (owner decision):** patch it rather than allow-list it or hide those configurations. The owner chose 4.1.139.Final, the latest 4.1.x, which has no known advisories.
  - **TDD:** red `UnifiedTestPlatformTest`, then green.
  - **How:** the Android convention adds the Netty BOM to those configurations, through the DependencyHandler. Added straight to the configuration, the BOM lost its platform attribute and changed nothing.
  - **Probe:** in all 18 configurations, every Netty module now resolves to 4.1.139.Final.
- **The second review found two more libraries.** Once Netty was patched, it failed on Bouncy Castle 1.79 (two critical advisories) in Android lint, the test runner and AGP's own plugin classpath, and the first run had not shown them.
  - **A full query to stop finding issues one at a time:** an init script listed the 511 coordinates resolved across every configuration, settings and buildscript classpaths included. GitHub's advisory API was then queried for them in batches (`GET /advisories?ecosystem=maven&severity=critical&affects=…`). It found exactly three critical issues:
    - Netty, already patched;
    - Bouncy Castle;
    - FreeMarker 2.3.32 (GHSA-27j2-h3m2-8237), on AGP's plugin classpath.
  - **The fix (owner decision):** patch them as Netty was patched.
    - TDD: red `AgpToolConfigurationTest`, then green.
    - `AgpToolSecurityFloors.kt` adds the Netty and Bouncy Castle BOMs to every AGP tool configuration (`unified-test-platform-*`, `androidLintTool`), from both the Android and the KMP conventions.
    - The root build script raises Bouncy Castle (its BOM, 1.85, the version Robolectric already uses) and FreeMarker (2.3.35) on the plugin classpath.
  - **Verified:**
    - the query again found no critical advisory;
    - AGP generated a new debug keystore with Bouncy Castle 1.85 (empty `ANDROID_USER_HOME`);
    - `apksigner` verified the debug APK;
    - Android lint and `run-all.sh` pass.
- **Without the ordering, the review gives a false green.** If it ran alongside the graph job, it would compare a pull request without its Gradle dependencies and pass. The new guard test prevents that.
- Docs updated: `ci_cd.md` (catalogue, §6 permissions and scanning), `security_privacy.md`, `architecture.md`, `AGENTS.md`, ADR-0011 and its index, the backlog and both READMEs.

**Open / next**
- **Scope of the review:** it covers the Kotlin area, which holds every Gradle dependency. Actions changed only in `ios-checks.yml` or `policy-checks.yml` are covered by Dependabot alerts after the merge.
- **First run:** `main` had no submitted Gradle graph, so this PR's review compared against an empty base. It waited the full 3 minutes for the base snapshot, then reviewed every Gradle dependency as added; that is how it found the Netty issue. After the merge, `main` has a graph and reviews compare against it.
- **Drop each security floor** once AGP brings a fixed version itself (`ci_cd.md` §6). The nightly workflow will be the first to run instrumented tests with the patched Netty and Bouncy Castle.
- **Two high advisories remain**, which do not block: jdom2 2.0.6 (GHSA-2363-cqg2-863c, XXE) and jose4j 0.9.5 (GHSA-3677-xxcr-wjqv, DoS), both on AGP's plugin classpath. Revisit them with the next AGP upgrade.
- **Rest of LB-002:**
  - the nightly workflow, with OWASP Dependency-Check;
  - the iOS snapshot and ViewModel tests, which have no code to test before E1;
  - the throwaway-branch check that a failing test, a lint violation and a coverage drop each fail CI;
  - speeding up the iOS app job with caches.

## 2026-10-24: LB-002: ktlint, detekt and Kover (ADR-0011)

**Decisions (owner)**
- **detekt 2.0.0-alpha.6, pinned.** The stable 1.23.8 is built against Kotlin 2.0, and its type-resolved rules do not support Kotlin 2.4. ADR-0011 records the decision.

**Done**
- The owner was away for some hours, so the three tools went into one PR rather than three. This avoids stacking PRs that would conflict in the READMEs and this log. This was the agent's choice, not an owner decision.
- **ktlint 1.8.0 (CLI):**
  - the root tasks `ktlintCheck` and `ktlintFormat` use the shadowed artefact on the JDK 21 toolchain, over every Kotlin source and build script;
  - `.editorconfig` exempts `@Composable` functions from function naming;
  - **negative case:** the first run found 198 violations;
  - `ktlintFormat` fixed all of them, with no manual edits, in a separate `style:` commit; `check` passed afterwards.
- **detekt 2.0.0-alpha.6:**
  - `config/detekt/detekt.yml` holds only the differences from the defaults: complexity 15, Composable naming, `ForbiddenMethodCall` (print, println, `Clock.System.now`, `System.currentTimeMillis`) and `ForbiddenImport` (GlobalScope, Firebase);
  - `ciDetekt` runs the type-resolved tasks: `detektMainAndroid` and `detektHostTestAndroid` for KMP modules, `detektMain` and `detektTest` for Android modules, plus the iOS source sets;
  - **probe:** `println(kotlin.time.Clock.System.now())` in umbrella common code was reported twice, and the file was restored.
  - The clean-architecture import rule stays with `ModuleRules` and Konsist, which already enforce it; `code_style.md` is updated.
- **Kover 0.9.11:**
  - shared domain and data modules are gated at 80 % line coverage, and feature modules on their `domain`, `data` and `presentation` packages;
  - **probe:** an untested class in `:shared:transit:domain` failed with "lines covered percentage is 0.000000, but expected minimum is 80", and was removed.
- **CI:** the `ktlint`, `detekt` and `Coverage` jobs run behind the `Kotlin gate`, and the root `check` runs all three.
- Docs: ADR-0011, `code_style.md`, `ci_cd.md`, `testing_strategy.md`, `AGENTS.md` (status and commands), the backlog and both READMEs.
- The owner asked for an AFK run without permission prompts. Auto mode was already on. The classifier blocked my attempt to add local allow rules, as self-modification, so permissions are unchanged.

**Review (code-reviewer subagent): changes required, 1 major and 13 others.** Addressed:
- **CI false green**, a TDD cycle. A PR touching only `config/detekt/detekt.yml` or `.editorconfig` skipped ktlint and detekt. Both paths now count as Kotlin paths.
- **Coverage scope**, a TDD cycle. `coverageGate(path)` is now a tested rule, and it gates `:shared:core:presentation` too (shared presentation mappers, `testing_strategy.md` §3).
- **detekt:**
  - `ciDetekt` also runs every iOS, Apple and native source set (untyped);
  - `androidHostTest` is excluded from `FunctionNaming` and `MagicNumber`;
  - detekt's default `ForbiddenMethodCall` entries are kept;
  - `GlobalCoroutineUsage` is on;
  - the Firebase, token and fixture exemptions are suppressed at the call site, as documented.
- **ADR-0011 is now Proposed.** The owner decided only the detekt version, so the ktlint CLI and the Kover scope await confirmation. Its claims are narrowed: type-resolved rules cover common and Android code, and `build-logic` and `tools` get ktlint only.
- **Smaller fixes:**
  - the post-edit hook formats only with the pinned ktlint 1.8.0;
  - `run-all.sh` names every tool that `check` runs;
  - `config/` is listed in the READMEs and `AGENTS.md` §3;
  - stale lines are fixed in `ci_cd.md`, `architecture.md` and `testing_strategy.md`.
- **Verified by the reviewer:**
  - the `style:` commit changes no behaviour;
  - the new tasks are safe under the configuration cache and Isolated Projects;
  - each CI job runs one command.

**Open / next**
- The rest of LB-002: dependency review, the iOS snapshot and ViewModel tests, the nightly workflow, and the `iOS build and tests` budget (caches).
- Move detekt to 2.0 stable when it ships.
- The owner confirms ADR-0011 (the ktlint CLI and the Kover scope), or asks for changes.

## 2026-10-24: LB-002: the iOS workflow (SwiftLint, shared iOS tests, app build and tests)

**Done**
- **TDD cycle 1 (change detection):**
  - A preparatory refactor first: the script tests read each area by name.
  - Red then green: `changed-areas.sh` now also prints `ios=`. The area covers:
    - `iosApp/`, `.swiftlint.yml` and `shared/`;
    - the Gradle build (`gradle/`, `build-logic/`, `*.gradle.kts`, `gradlew*`, `gradle.properties`);
    - `version.properties`, `scripts/ios/`, `scripts/ci/`, the iOS workflow and the Gradle setup action.
  - Docs-only and Android-only changes skip the iOS jobs.
- **TDD cycle 2 (workflow):**
  - `lb002_iosGateWaitsForEveryIosJob` requires `ios-checks.yml`, with a gate that waits for every job.
  - The job-name guard now covers every workflow.
- **`ios-checks.yml`:**
  - `iOS changes`;
  - `SwiftLint` (Linux): `scripts/ci/install-swiftlint.sh` installs the pinned 0.65.1 binary after a SHA-256 check;
  - `Shared iOS tests` (`macos-26`): `./gradlew iosSimulatorArm64Test`;
  - `iOS build and tests` (`macos-26`, Xcode 26.6 pinned through `DEVELOPER_DIR`, XcodeGen from Homebrew): `scripts/ios/test.sh`, which generates the project and runs `xcodebuild test` on the iPhone 17e simulator with iOS 26.5;
  - `iOS gate`.
- **The first run on PR #10 passed:**
  - SwiftLint: 15 s, 0 violations in 13 files.
  - Shared iOS tests: 2 min 45 s. The `core:presentation` and `umbrella` simulator tests ran.
  - iOS build and tests: 8 min 44 s. The cold framework build and project generation took about 3 min, and `xcodebuild test` about 5 min 45 s.
  - The macOS jobs waited about 9 min for a runner.
- `scripts/ios/test.sh` passes locally in 40 s.
- Updated `ci_cd.md` (the catalogue and §4), `engineering_workflow.md`, `AGENTS.md`, both READMEs and the backlog. `protect-main.sh` now also requires `iOS gate`.

**Decisions**
- **One `Shared iOS tests` job, not a per-module matrix.** GitHub Free runs at most 5 macOS jobs at once, and a 19-job matrix would queue for a long time. Split it when its time grows.
- **SwiftLint on Linux with a pinned, checksummed binary**, instead of a container image, whose tags cannot be pinned by digest from here.

**Open / next**
- **After this PR is merged:** apply `protect-main.sh`, so that `iOS gate` becomes required.
- **The budget is exceeded:** `iOS build and tests` took 8 min 44 s, against 8 min. Next, cache `~/.konan` and the Xcode DerivedData, or split the build from the UI tests (`ci_cd.md` §5).
- **The rest of LB-002:** ktlint, detekt, Kover, dependency review, the iOS snapshot and ViewModel tests, and the nightly workflow.

## 2026-10-24: Skipped CI checks reviewed; readable matrix job names (LB-002)

**Done**
- **The owner asked to check and fix every skipped check on GitHub.**
  - Nothing on `main` was skipped.
  - On PR #8, a docs-only PR, the six Kotlin jobs were skipped by the path filter, as designed. The required `Kotlin gate` passed.
  - The two matrix jobs showed as `${{ matrix.module }}`, because GitHub does not expand a skipped job's name.
- TDD cycle:
  - `lb002_kotlinJobNamesReadWellWhenSkipped`, backed by the `jobNamesWithExpressions` rule and its unit test, forbids `${{ }}` in job names.
  - The matrix jobs are now "Shared unit tests" and "Android unit tests"; GitHub appends the module when they run.
- **First strict SwiftLint run** (0.65.1, installed by the owner) on the Swift code from LB-001:
  - the only findings were the built-in `todo` rule rejecting the two `TODO(LB-010)` and `TODO(LB-012)` comments, which `code_style.md` §1 allows;
  - that rule is replaced by `todo_without_story`. Probe: it rejected `// TODO: …` and `// FIXME …`, and accepted `// TODO(LB-012): …`;
  - every other rule passes, including the trailing commas and test names prepared in PR #7.

**Decisions (owner)**
- **Keep the path filter.** Docs-only PRs skip the Kotlin jobs on purpose (`ci_cd.md` §2, principle 5), and the skipped checks now read well. The alternatives were rejected:
  - running every job on every PR;
  - jobs that run but do nothing, a misleading green.

## 2026-10-23: ADR-0010 accepted; the Android module-rule probe

**Decisions (owner)**
- **ADR-0010 accepted.** The iOS project is generated with XcodeGen, `Shared` is linked once through the `LogroBusUI` package, and a script refreshes it.
  - ADR-0004's status records the amendment.
  - ADR-0007's "validate in LB-001" consequence is annotated as validated; its decision is unchanged.
- **The negative Android probe from PR #6 is approved**, and was run.

**Done**
- Probe, on `main` after PR #7, reverted afterwards:
  - added `implementation(projects.android.feature.stops)` to `android/feature/lines`, then ran `./gradlew :android:feature:lines:help`;
  - the build failed at configuration with: `Module dependency rules violated in 'implementation': :android:feature:lines -> :android:feature:stops is not allowed: a Android feature (lines) module may not depend on a Android feature (stops) module (ADR-0007, docs/architecture.md §3.2)`;
  - `ModuleRules` therefore blocks the dependency in a real Android build, which closes LB-001's "on either platform" criterion on Android too.
- This PR is docs-only, so it is also the first docs-only PR since the Kotlin gate became required. Its CI run closes the check left open by LB-002 part 1:
  - `Kotlin changes` passed, and the six other Kotlin jobs were skipped;
  - `Kotlin gate` passed, logging `Kotlin job results: success skipped skipped skipped skipped skipped skipped`;
  - the PR was `MERGEABLE` / `CLEAN` under branch protection.

**Open / next**
- The `ModuleRules` message says "a Android feature". Fix the article in build-logic, with a test (small, LB-002 or the next build-logic change).
- LB-002: `ios-checks.yml` (macOS runner), ktlint, detekt, Kover.

## 2026-10-23: LB-001 part 3 of 3: the iOS app skeleton (LB-001 done)

**Done**
- TDD cycle 1 (shared): `SharedDependencies`, the Swift entry point of `architecture.md` §5.2. It exposes the start destination.
  - With a source of its own, the umbrella finally links the `Shared` framework; it had been `NO-SOURCE`.
  - Swift sees it as `SharedDependencies.shared.startDestination`.
- TDD cycle 2 (architecture tests): `iosApp/project.yml` (XcodeGen), the `LogroBusUI` package and an ignored `.xcodeproj`.
  - The spec covers the bundle id, iPhone only, iOS 18.0, Swift 6, Spanish as the development language, and `MARKETING_VERSION` from the generated `Version.xcconfig`.
  - The package has the DesignSystem, MapUI and Feature<X> targets, still empty, plus `Shared` as an XCFramework binary target.
  - `scripts/ios/generate-project.sh` builds and copies the XCFramework, writes `Version.xcconfig` and runs XcodeGen. `bootstrap.sh` calls it on macOS.
- TDD cycle 3 (iOS): an XCUITest launches the app in English and Spanish and expects "Lines" and "Líneas".
  - The placeholder reads the start destination from Kotlin.
  - Swift maps it to the `tab.*` keys of a `Localizable.xcstrings` catalog: Spanish as the source language, plus English.
- TDD cycle 4 (iOS): a hosted unit test target (`iosAppTests`, Swift Testing) checks the **built** bundle: iPhone only, iOS 18.0, and the store bundle id.
  - It caught a real defect: the built `Info.plist` had `UIDeviceFamily` [1,2], because XcodeGen's application preset sets the device family on the target, overriding the project-level value. The setting moved to the target.
- All iOS tests pass on the iPhone 17e simulator (iOS 26.5) with Xcode 27.1: 3 bundle tests and 2 launch UI tests.
- Docs:
  - `architecture.md` §5.2 now describes the integration as built;
  - `release_process.md` §1 says the version file is generated by the script;
  - `AGENTS.md` §4 lists the iOS commands, and §8 the Swift test naming;
  - both READMEs say how to run the iOS app.

**Reviews (code-reviewer and ios-ui-reviewer subagents): both "changes required".** Addressed:
- **A false green in CI**, a TDD cycle. A PR touching only `iosApp/project.yml`, a `Package.swift` or `.gitignore` skipped the architecture tests that read them. `changed-areas.sh` now counts those paths as Kotlin paths.
- **No iOS feature-to-feature check**, a TDD cycle. LB-001 requires one on both platforms. `crossFeatureSwiftDependencies` parses the package manifest. Probe: a `FeatureLines -> FeatureStops` dependency was reported.
- **Portrait lock**, a TDD cycle. No doc decided it, Android does not do it, and it goes against WCAG 1.3.4 (NFR-A11Y-01). The app now supports portrait and both landscape orientations, checked on the built bundle.
- **ADR conflict.** ADR-0004 says `embedAndSignAppleFrameworkForXcode`; the build links through the package. **ADR-0010 is proposed for the owner to accept.** It records XcodeGen, the single link through the package, the refresh script and the release argument.
- **Weak spec test.** The iOS spec test now reads the app target's own settings. Probe: a project-level device family now fails.
- **SwiftLint, prepared but not run:**
  - trailing commas removed;
  - `allowed_symbols` dropped, since SwiftLint 0.55 or later skips function names anyway;
  - test names kept under 40 characters.
- **`generate-project.sh`:**
  - takes `debug|release`, so Release builds no longer link the debug Kotlin framework;
  - runs Gradle without the configuration cache and file-system watching;
  - requires XcodeGen 2.46.0 or later.
- **`bootstrap.sh`:** the header now says it runs a Kotlin/Native build, and `LOGROBUS_SKIP_IOS=1` skips it.
- **Docs:**
  - the `architecture.md` tree and testing table;
  - `testing_strategy.md`;
  - the `ci_cd.md` paths;
  - `AGENTS.md`;
  - the umbrella build comment;
  - `release_process.md`.
- **Noted, not changed:**
  - **TDD scope.** Some green commits carried code no red test required: the app stub and the script in `d436ce3`, and `bootstrap.sh` in `8fc6e7a`. Harness and lint changes belong in their own commits. History is not rewritten.
  - **Commit scope.** `ci(policy)` is not in the `AGENTS.md` §12 scope list.

**Decisions**
- **The app links `Shared` once, through the `LogroBusUI` package's binary target.** It does not use `embedAndSignAppleFrameworkForXcode`, which would link a second copy next to the package's.
  - Swift Package Manager resolves binary targets before any build phase, so after a Kotlin change you rerun `scripts/ios/generate-project.sh`.
  - The documented fallback, feature folders in the app target, was not needed.
- **`Version.xcconfig` comes from the generation script, not from a build phase:** Xcode resolves build settings before any phase runs.
- **SKIE stays in LB-010.** Until then, Kotlin enums are classes in Swift, so the label `switch` needs a `default`.

**Learned**
- **The false `NO-SOURCE` results had a second cause.** In a new source directory, a test stayed `NO-SOURCE` even with `--no-configuration-cache`; it ran only with `--no-watch-fs`.
  - The Gradle daemon's file-system watching on macOS misses newly created directories.
  - `run-all.sh` now turns off both the configuration cache and file-system watching, and `AGENTS.md` §4 explains why.
- **XcodeGen computes `TEST_HOST` from the target name** (`iosApp.app`). The product is `LogroBus.app`, so the unit test target spells it out.
- **My mistake, fixed before any commit:** I overwrote the existing `.swiftlint.yml` from LB-016 without reading it first. I restored it from git, then added only the generated-path exclusions and `_` in identifiers, for the test naming.

**Open / next**
- SwiftLint is not installed on this machine, so `run-all.sh` skips the Swift gate, and `.swiftlint.yml` has not been checked against the new Swift code yet.
- The owner accepts or changes ADR-0010. On acceptance, ADR-0004's index status records the amendment, and ADR-0007's "validate in LB-001" consequence is resolved.
- Check the empty `Shared.framework` stub that Xcode embeds at the first archive. Change the `.gitignore` pattern when the first remote Swift package needs a committed `Package.resolved` (LB-013).
- LB-002: `ios-checks.yml` (macOS runner: framework, generation, `xcodebuild test`, SwiftLint), plus ktlint, detekt and Kover.
- From PR #6: the negative Android build for a feature-to-feature dependency, pending the owner's go-ahead.

## 2026-10-23: LB-001 part 2 of 3: the Android app skeleton

**Done**
- After PR #5 was merged, `protect-main.sh` was applied. `Kotlin gate` is now a required check, alongside the six policy jobs; the GitHub API confirms it.
- TDD cycle 1 (build-logic): Android namespaces keep their platform segment. Without it, `:android:feature:lines` and the Android target of `:shared:feature:lines` would share a namespace, and AGP rejects duplicates.
- TDD cycle 2 (architecture): the 9 Android modules, each with its convention plugin.
  - Conventions:
    - `logrobus.android.application` and `logrobus.android.library` take the SDK levels and the JVM target from the catalog;
    - they enable Compose with its BOM and Robolectric-ready unit tests;
    - they add the `ciUnitTest` alias (`testDebugUnitTest`) and enforce `ModuleRules`.
  - `:android:app`: `applicationId` `com.ruizurraca.logrobus`, minSdk 26, targetSdk 37, versionName 0.1.0 from the new `version.properties` (`release_process.md` §1), versionCode 1 locally.
  - Refactor: the catalog helpers and `verifyModuleRules` moved to their own files.
- TDD cycle 3 (shared): `TopLevelDestination` in `:shared:core:presentation`.
  - It lists LINES, NEAR_ME and PLANNER, and starts on LINES (FR-NAV-01, FR-NAV-02, OQ-15). It is the first value shared by both apps.
  - The tests are named `lb012_…`, after the app-shell story that owns the requirements. They pass on the Android host and on the iOS simulator.
- TDD cycle 4 (Android): a Robolectric Compose test launches `MainActivity` and expects "Lines" in English and "Líneas" in Spanish.
  - The app draws edge-to-edge and shows a placeholder with the start destination title. Android maps the shared destination to its own string resources.
  - Manual check: on the API 36 emulator, a cold start of 881 ms, with no crash.
- LB-002 work in this PR, as planned in the PR #5 log entry. Without it, the Kotlin gate would have passed Android changes without running a single Android test.
  - A TDD cycle requires the workflow matrix to list every Android module.
  - New jobs: `Android unit tests: <module>`, `Android build` and `Android lint`.
- Clean-copy check (`git archive`): `./gradlew build allTests` succeeds. That covers the release APK, the iOS simulator tests and lint. `AGENTS.md` §4 now lists the verified commands.

**Reviews (code-reviewer and android-ui-reviewer subagents): both "changes required", no blockers.** Addressed:
- **Dark-mode system bars**, a TDD cycle. `enableEdgeToEdge()` picked light icons in dark mode on the always-light placeholder, so they were invisible. Both bars now use `SystemBarStyle.light` until the brand theme (LB-011).
- **Tab copy**, a TDD cycle. The labels now follow `design_guidelines.md` §2.1 and §9:
  - keys `tab_lines`, `tab_near_me` and `tab_directions`;
  - en "Lines", "Near me", "Directions"; "How to get there" stays the screen title;
  - the mapping lives in its own file, `TopLevelDestinationLabels.kt`.
  The LB-012 criterion said "Near Me, How to Get There". It now points to the guidelines, and **the English copy is still a proposal for the owner**.
- **Namespaces**, a TDD cycle. The architecture tests' `expectedNamespace` now keeps the `android` segment, as build-logic does, and the namespace rule covers `android/` sources.
- **Gate needs**, a TDD cycle. `lb002_kotlinGateWaitsForEveryKotlinJob` fails if a Kotlin job is missing from the gate's `needs`. Probe: dropping `android-lint` was reported.
- **Lint:**
  - CI runs `lintDebug` in every Android module;
  - `MissingApplicationIcon` is suppressed, with a pointer to LB-413;
  - all 9 reports say "No issues found".
- **Smaller fixes:**
  - `run-all.sh` runs Gradle without the configuration cache;
  - the `versionCode` comment no longer claims CI support that does not exist;
  - a missing `VERSION_NAME` fails with a clear message.
- **Not done, for the owner to decide:** a negative build of an Android feature-to-feature dependency, to show `ModuleRules` failing in a real Android build. The reviewer's attempt to run it was denied by its permissions. The rule itself is unit-tested, and the conventions call `verifyModuleRules`.
- **Follow-ups:**
  - `ModuleRules` lets `:android:app` depend on anything except tools, which is wider than `architecture.md` §3.2.
  - Check how `TopLevelDestination.start`, a companion member, looks from Swift under SKIE (part 3).

**Decisions**
- **First shared value:** `TopLevelDestination` rather than a placeholder. It carries real behaviour (FR-NAV-01, FR-NAV-02) that LB-012 builds on, and shared code carries only the meaning.
- **Placeholder screen without a theme:** there is no `MaterialTheme` in the app, and the platform theme is `Theme.Material.Light.NoActionBar`. Theming belongs to the design system (LB-011).
- **Mixed story in one PR:** the Android CI jobs (LB-002) ship with the Android code (LB-001), so the code is never merged untested. The commits keep their own story ids.

**Learned**
- **API 37 under Robolectric 4.17 needs three adjustments** (`testing_strategy.md` §4.5):
  - `--add-opens=java.base/jdk.internal.access=ALL-UNNAMED`;
  - Espresso 3.7.0, because Compose UI Test brings 3.5.0, which calls the removed `InputManager.getInstance()`;
  - NATIVE graphics, or text measures 5 px wide and `assertIsDisplayed()` fails.
- **Configuration cache:** a reused entry missed newly created source directories, and reported the new tests as `NO-SOURCE`, a false green. Run once with `--no-configuration-cache` after creating a source directory (`AGENTS.md` §4). CI always configures from scratch.
- **The iOS framework still links as `NO-SOURCE`:** the umbrella module has no sources of its own, even though it exports `TopLevelDestination`. Part 3 must give it one.

**Open / next**
- LB-001 part 3: the iOS app (XcodeGen, the `LogroBusUI` package, the `Shared` framework, a launch test).
- On the first docs-only PR: confirm that the Kotlin jobs are skipped and the gate passes.

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
