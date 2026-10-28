# Testing strategy

| Field | Value |
| --- | --- |
| Status | Draft, pre-scaffold |
| Last updated | 2026-10-24 |
| Based on | Spec §10.1–10.2, NFR-MNT-01, `architecture.md` §8 |

This document covers what we test, at which level, with which tools, and what every pull request must include. The test harness described in §4 is built during M0, in stories LB-003 to LB-010.

## 1. Principles

1. **Deterministic by default.** Unit and integration tests never touch the network, the real clock, or the device time zone.
2. **The live source is off-limits in tests.** Only the fixture-recording job (§4.3) and the nightly Maestro flows call `transporteurbano.logrono.es`.
3. **Every data rule is pinned by a test.** Each `DD-nn` rule has at least one test whose name starts with `ddNN_`.
4. **Every UX state is pinned by a screenshot.** Each data-driven screen has a reference image for every state in spec §7.3.
5. **A bug fix starts with a failing test.** The regression test is written first and stays in the suite.
6. **Tests come first.** Test-driven development is the default way of working (§7).

## 2. Test levels

| Level | Scope | Location | Tooling | Runs |
| --- | --- | --- | --- | --- |
| Unit | Mappers, normaliser, DD rules, ETA display rules, direction inference, geo utilities, fare classification (v1.0) | `shared/*/src/commonTest` | kotlin.test, `TestAppClock` | Every PR |
| Contract | DTO decoding of golden payloads; envelope and per-item tolerance | `shared/transit/data`, `shared/feature/planner` commonTest | `FixtureLoader` | Every PR |
| Integration | Repositories, static sync atomicity, network failure and retry, polling and backoff | `shared/transit/data`, `shared/favourites/data`, `shared/core/polling` commonTest | Ktor `MockEngine`, in-memory SQLDelight driver, Turbine | Every PR |
| Presentation mappers | Shared mappers from domain results to `UiState` and UI models, every UX state | `shared/feature/*/src/commonTest`, `shared/core/presentation` | kotlin.test, `TestAppClock` | Every PR |
| Android ViewModels | State transitions, events, effects and lifecycle | `android/feature/*/src/test` | JUnit, Turbine, fakes from `:shared:core:testing` | Every PR |
| iOS ViewModels | State transitions, events, effects and task cancellation | `iosApp/Packages/LogroBusUI/Tests` | Swift Testing, `SharedDependencies.preview` fakes | Every PR that touches `iosApp/` or `shared/` |
| Architecture rules | Module and package boundaries, telemetry event types | `tools/architecture-tests` | Konsist | Every PR |
| Android UI | Screen states and primary interactions | `android/feature/*/src/test` (Robolectric), `android/app/src/androidTest` | Compose UI test, Roborazzi | Every PR (Robolectric); nightly (instrumented) |
| iOS UI | Screen states and primary interactions, plus checks of the built bundle (device family, orientations, deployment target, bundle id) | `iosApp/Packages/LogroBusUI/Tests` (snapshots), `iosApp/iosAppTests` (hosted, Swift Testing), `iosApp/iosAppUITests` | Swift Testing, swift-snapshot-testing, XCUITest | Every PR that touches `iosApp/` or `shared/`; otherwise nightly |
| Accessibility (automated) | Labels, touch targets, contrast | Same as the UI tests | Android Accessibility Test Framework checks; XCUITest `performAccessibilityAudit()` | With the UI tests |
| End to end | The five critical journeys (`architecture.md` §8) against the live source | `maestro/` | Maestro | Nightly, non-blocking |
| Performance | Cold start, first ETA, search latency, map frame rate | `android/benchmark`, `iosApp/iosAppUITests` | Macrobenchmark plus Baseline Profiles; `XCTApplicationLaunchMetric` | Weekly and before each release (LB-411) |
| Accessibility (manual) | TalkBack and VoiceOver walkthrough, 200 % font, Reduce Transparency, Increase Contrast | Checklist in `design_guidelines.md` §8 | Devices | Before each beta and release |

## 3. What every pull request must include

| Change | Required tests |
| --- | --- |
| Mapper, normaliser or DD rule | Unit tests, including one `ddNN_` test per rule touched |
| New or changed DTO | Contract test against a recorded or synthetic fixture |
| Repository, sync, polling or caching | Integration test with `MockEngine` and the in-memory database, covering success, failure and retry |
| Shared presentation mapper | Unit tests for every `UiState` it can produce |
| Android or iOS ViewModel | Tests for every state it can emit, for each event, and for cancellation |
| Compose or SwiftUI screen | Screenshot per UX state (light, dark, 200 % font), plus one interaction test for the primary action |
| Bug fix | A regression test that fails before the fix |
| Docs, scripts or CI only | `scripts/checks/run-docs-checks.sh` passes |

**Coverage gates (Kover):**

| Modules | Line coverage | Enforcement |
| --- | --- | --- |
| Shared domain and data modules (`transit`, `favourites`, and the `domain`/`data` packages of `feature/*`) | At least 80 % (NFR-MNT-01) | Blocks the PR |
| Shared presentation mappers (`:shared:core:presentation`, and the `presentation` packages of `feature/*`) | At least 80 % | Blocks the PR |
| Android ViewModels | Target 70 % | Reported only, once the first ViewModels exist (Android modules are not instrumented yet) |

## 4. Test harness

### 4.1 `:shared:core:testing`

| Component | Purpose |
| --- | --- |
| `TestAppClock` | Settable `now()`, `advanceBy(duration)`, and a configurable server offset. Replaces `AppClock` in every test |
| `TestDispatchers` | `StandardTestDispatcher` wiring for `AppDispatchers`, used with `runTest` virtual time |
| `FixtureLoader` | Reads `fixtures/<name>.json` through Okio. The fixtures directory reaches the tests through a Gradle-provided system property or generated constant, so commonTest works on every target |
| `mockSource { … }` | Small DSL over Ktor `MockEngine`: `get("/api/linesDiscovery/stops") respondsWith fixture("stops_2026-10-22.json")`, `respondsWith status(503)`, `respondsWith timeout()`, `withDateHeader(instant)` |
| `Fake*Repository` | In-memory implementations of every domain repository interface, with controllable emissions and errors |
| `FakeLocationProvider` | Scripted permission states and location sequences |
| `FakeRemoteConfig` | Overrides for single keys on top of the compiled defaults |
| Builders | `aLine()`, `aStop()`, `anArrival()`, `aVehicle()` with sensible defaults and named overrides |

### 4.2 Fixtures

- `fixtures/` holds the golden payloads listed in spec §10.2 and `api_spec.md` §9.
- Recorded fixtures are named `<endpoint>_<variant>_<yyyy-mm-dd>.json`. Hand-written ones start with `synthetic_` (for example `synthetic_vm_dd05_cest.json`).
- Recorded fixtures are never edited by hand. If a test needs a variation, add a synthetic fixture.

### 4.3 Fixture recorder (`scripts/record-fixtures.sh`, LB-003)

- Fetches each endpoint once, at no more than 1 request per second, with the app's `User-Agent` and `Accept-Encoding: gzip`.
- Writes pretty-printed JSON to `fixtures/` with today's date.
- Compares the JSON *shape* (the set of key paths and value types, extracted with `jq`) with the previous capture of the same endpoint, and prints a diff.
- Exits non-zero when the shape changed. The weekly CI job (`ci_cd.md` §3) uses this exit code to flag contract drift (risk R1).

### 4.4 Time and time zones

- Tests use fixed instants from `TestAppClock`, never the wall clock.
- DD-05 and the ETA arithmetic are tested on both sides of the 2026 daylight-saving transitions:
  - 2026-03-29 01:00 UTC (CET → CEST).
  - 2026-10-25 01:00 UTC (CEST → CET).
- Kotlin tests force `TimeZone.of("Europe/Madrid")` explicitly. They must not depend on the time zone of the machine running them.

### 4.5 Android host tests (Robolectric)

Android unit tests run on the JVM with Robolectric, through each module's `ciUnitTest` alias (`testDebugUnitTest`). A shared module can use it in its Android host tests too, with no resources: `:shared:core:location` does, for the Fused Location Provider. The `logrobus.android.*` and `logrobus.kmp.library` conventions and the test classes set up four things that API 37 needs:

- **Resources:** `isIncludeAndroidResources = true`, so Robolectric reads the merged resources and manifest. Qualifiers such as `@Config(qualifiers = "es")` select the language.
- **JDK access:** `--add-opens=java.base/jdk.internal.access=ALL-UNNAMED` (`ROBOLECTRIC_JDK_ACCESS` in build-logic, set by both conventions). Robolectric's Android 17 (API 37) sandbox needs it on JDK 21.
- **Graphics:** test classes that assert on Compose UI use `@GraphicsMode(GraphicsMode.Mode.NATIVE)`. In the default LEGACY mode, text measures a few pixels wide and `assertIsDisplayed()` fails. Roborazzi needs NATIVE too.
- **Espresso:** Compose UI Test brings Espresso 3.5.0, which calls `InputManager.getInstance()`, removed in API 37. Modules with Compose UI tests add `libs.androidx.test.espresso.core` (3.7.0).

### 4.6 Screenshots

- **Android:** Roborazzi (1.77.0) on the JVM, with Robolectric's native graphics.
  - Reference images live under each module's `src/test/screenshots/` (`android/core/designsystem/…`, `android/feature/<x>/…`).
  - Verification runs on every PR, in the `android-screenshots` job. Recording is an explicit task (`recordRoborazziDebug`); look at every recorded image before committing it.
  - **SDK 36:** screenshot tests run on `SCREENSHOT_SDK` (36) through `@Config(sdk = …)`. On SDK 37, the target, Robolectric 4.17 drew only the first capture of a test run, and later ones came out as the bare window background. The module's other tests keep the target SDK.
  - A screen's states run as a parameterised test (`ParameterizedRobolectricTestRunner`), one capture per test, from a compose rule; a component's variants may share one test.
  - 200 % font: wrap the content in `LocalDensity provides Density(density, fontScale = 2f)`.
- **iOS:** swift-snapshot-testing on the pinned CI simulator, with references under `__Snapshots__/`. A reduced set also runs nightly on an iOS 18 simulator to cover the fallback appearance without Liquid Glass (NFR-PLAT-01).
- Each screen needs these variants: light, dark, 200 % font, and every UX state.

### 4.7 Maestro flows

- One flow per critical journey, in `maestro/`, tagged `critical`.
- Flows run nightly against the live source on an emulator and a simulator.
- A failing flow opens an issue but never blocks merges: the live source is outside our control.

## 5. Device and OS matrix

| Purpose | Android | iOS |
| --- | --- | --- |
| Minimum OS | API 26 emulator | iOS 18 simulator (fallback appearance) |
| Latest OS | Latest stable API emulator | Latest stable iOS simulator (Liquid Glass) |
| Performance budgets (NFR-PERF-01) | Pixel 6a (physical) | iPhone 12 (physical) |
| Large screen (FR-NAV-05) | Tablet or foldable emulator | Not applicable: iPhone only (NFR-PLAT-03) |

## 6. Flaky tests

- A flaky unit, contract, integration or ViewModel test is a bug: fix it in the next PR.
- A test may be quarantined (`@Ignore` / `.disabled`) only with the owner's approval. The annotation must carry the reason and a tracking link, and a quarantined test must be fixed within two weeks.
- Flaky Maestro flows are expected, given the live source. Investigate them only when they fail for three nights in a row.

## 7. Test-driven development (TDD)

TDD is the default for every change that adds or changes behaviour.

| Area | Practice |
| --- | --- |
| All shared Kotlin modules, and the Android and iOS ViewModels | **Mandatory** red → green → refactor, one commit per phase (§7.1) |
| Android and iOS UI | **State-first:** write the `UiState` fakes, previews and screenshot tests for every UX state before wiring real data, and the interaction test before the behaviour |
| Bug fixes | Mandatory: the regression test comes first (principle 5) |
| Scripts and CI checks | Show the check failing on a negative case before relying on it |
| Exempt | Throwaway spikes on `spike/*` branches (never merged), generated code, and purely visual tweaks already covered by screenshots (re-record them instead) |

### 7.1 The cycle: one commit per phase, then push

This protocol is mandatory (owner decision, 2026-10-22).

| Step | Action | Commit |
| --- | --- | --- |
| 1. Red | Pick the next acceptance criterion, or a smaller step towards it. Write the smallest test that expresses it and run it. It must fail **for the expected reason**: a failed assertion, or a missing API, but not a typo | `test(<scope>): <behaviour> [LB-xxx]`, with the body line `TDD: red`. Only test code (plus the minimum stubs needed to compile) |
| 2. Green | Write the minimum production code that makes the test pass. Run the module's tests | `feat(<scope>)` or `fix(<scope>)`: `<behaviour> [LB-xxx]`, with the body line `TDD: green` |
| 3. Refactor | Improve names and structure while the tests stay green. Run `scripts/checks/run-all.sh` | `refactor(<scope>): <what> [LB-xxx]`, with the body line `TDD: refactor` |
| 4. Push | `git push` the story branch | none |

- **Red commits are expected.** The git hooks never run tests on commit or push, so a red commit is allowed. PRs are merged with a merge commit, never squashed (`engineering_workflow.md` §5), so the red, green and refactor commits are preserved in `main`'s history as TDD evidence. The first-parent history of `main` (the merge commits) is always green; use `git log --first-parent` and `git bisect --first-parent`.
- **Nothing to refactor:** skip the refactor commit, and write `TDD: refactor not needed` in the green commit body. Do not create empty commits.
- **One cycle covers one behaviour.** Several small cycles per acceptance criterion are normal.

### 7.2 Traceability from criteria to tests

- **Acceptance tests:** each acceptance criterion in `acceptance_criteria.md` is covered by at least one automated test whose name starts with the story id, for example `lb205_requestsArrivalsForAllServedLines`. A criterion marked `[manual: …]` is the only exception.
- **Data-rule tests:** each DD rule test starts with `ddNN_` (principle 3).

### 7.3 Evidence

- **AI agents:** before implementing, show the failing test run in the progress report to the owner (`playbooks/start-story.md`).
- **Pull requests:** the PR template has a TDD checklist, and reviewers may ask for the red run.
- **Code without a test:** a test written after the fact for new behaviour is a review finding. Exempt cases (see the table above) are not.
