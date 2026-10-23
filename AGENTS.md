# AGENTS.md

Guidance for AI coding agents and human contributors working on **LogroBus**. This is the canonical instruction file: `CLAUDE.md` only points here. Keep this file short, current and actionable; long-form material belongs in `docs/`.

## Language policy (strict)

| Context | Language |
| --- | --- |
| Project artefacts: code, identifiers, comments, commit messages, branch names, pull request titles and descriptions, issues, docs, ADRs | Technical English, always |
| Agent conversation: replies, questions, plans, summaries and explanations shown to the owner | Peninsular Spanish (es-ES), unless the owner explicitly asks for another language |
| Owner's messages | English or Spanish; both are valid input, and the reply is still in Peninsular Spanish |

- In Spanish replies, keep code, identifiers, file paths and established technical terms in their English form (`ViewModel`, `pull request`, `StateFlow`).
- Two exceptions are required by other rules: `README.es.md`, the Peninsular Spanish mirror of `README.md` (§12), and the localised UI strings, where es-ES is the default and English is also shipped.

## 1. The project in one minute

LogroBus is a native Android and iOS app for Logroño's urban bus network: 13 day and night (Búho) lines, about 254 stops, real-time arrivals, live vehicles and door-to-door journey planning. Data comes from the public but undocumented and unversioned API behind `https://transporteurbano.logrono.es` (an Express BFF over SIRI real-time feeds, OpenTripPlanner and a Strapi CMS).

| Layer | Technology |
| --- | --- |
| Shared (KMP) | Clean architecture by feature module: domain, data, networking, persistence, normalisation, telemetry contracts and **presentation models** (`UiState`, UI models, mappers) |
| Presentation layer | Platform-specific: Android ViewModels plus Compose; Swift ViewModels plus SwiftUI (ADR-0007) |
| Android app | Kotlin, Jetpack Compose, Material 3 Expressive |
| iOS app | Swift, SwiftUI, Liquid Glass |

### The three sections

The app has exactly three top-level tabs. This "Live Mobility and Planning" structure replaces the five-tab navigation of spec §7.1 (ADR-0002).

1. **Lines** (`Líneas`). A "Your favourite lines" carousel at the top, then the full list grouped into Day and Night (Búho) lines and sorted by code, with a search bar. Line detail shows a map, a direction toggle, the ordered stop list with next arrivals, and live buses.
2. **Near Me** (`Cerca de mí`). A full-screen map with the user's location and nearby stop pins. Tapping a pin opens a bottom sheet with the next arrivals at that stop. When no stop is selected, the sheet lists saved stops (with live arrivals) and nearby stops, which is the list equivalent of the map.
3. **How to Get There** (`Cómo llegar`). Origin and destination search, recommended routes combining lines, walking times and step-by-step directions.

The app always opens on **Lines** (OQ-15). Saved stops with live arrivals live in the Near Me sheet, one tap away (OQ-17). Settings and "About data" (MVP), and later news, fares and recharge points, open from a toolbar action. They are never extra tabs.

## 2. Documentation map and precedence

| Document | Contains | Update it when |
| --- | --- | --- |
| `README.md`, `README.es.md` | Project overview for humans, in English and Spanish: features, stack, setup, status, recent changes | **In every PR, in both languages (mandatory, §12)** |
| `logrono-bus-app-functional-spec.md` | Baseline requirements v0.1 with `FR-`, `NFR-`, `DD-`, `API-`, `OQ-` ids | Never edit silently; record deviations in an ADR |
| `docs/README.md` | Index of all docs and id conventions | You add or rename a doc |
| `docs/architecture.md` | Modules, layers, data flow, platform integration | Structure or a cross-cutting pattern changes |
| `docs/api_spec.md` | Source API contract, DTOs, normalisation, fixtures | You observe a contract change or a new data quirk |
| `docs/design_guidelines.md` | The three sections, M3 Expressive and Liquid Glass rules, tokens, states | UI patterns or components change |
| `docs/backlog.md` | Epics and stories (`LB-nnn`) with status | You start, finish or split a story |
| `docs/open_questions.md` | Open questions (`OQ-nn`) with owner and answer | A question is raised or resolved |
| `docs/project_log.md` | Dated log of work and decisions | At the end of every working session |
| `docs/adr/` | Architecture decision records | A significant or hard-to-reverse decision is made |
| `docs/requirements.md` | MVP scope baseline and the requirements added after the spec (`FR-NAV`, `FR-NEAR`, …) | Scope changes, with the owner's approval |
| `docs/acceptance_criteria.md` | Given/When/Then criteria for every MVP story | A story or requirement changes |
| `docs/testing_strategy.md` | Test levels, TDD, test harness, what each PR must test | Test practice or tooling changes |
| `docs/engineering_workflow.md` | Branching, commits, PRs and templates, review, merge, dependencies | The workflow changes |
| `docs/ci_cd.md` | Atomic CI checks, workflows, the repository-visibility rule | A check or workflow changes |
| `docs/release_process.md` | Versioning, variants, signing, distribution, release checklist | Release practice changes |
| `docs/security_privacy.md` | Data inventory, permissions, controls, draft store declarations | A data flow, permission or SDK changes |
| `docs/code_style.md` | Detailed style rules and lint configuration | A style rule changes |
| `docs/agent_harness.md`, `docs/playbooks/` | Multi-agent harness; agent-agnostic workflows and review checklists | A workflow, hook or adapter changes |

When documents disagree, this order wins: the project owner's explicit instruction in the current task, then accepted ADRs, then this file, then the other `docs/`, then the functional spec. Fix the lower-ranked document in the same change, or raise an open question if the conflict is not yours to settle.

**Current status (2026-10-23):** LB-001 is in its last review: the Gradle build, the shared modules, and the Android and iOS app skeletons are in place (part 3, the iOS app, is in review). The repository is public and `main` is protected on GitHub (LB-415). CI runs the policy checks and the Kotlin and Android jobs on every pull request; LB-002 adds ktlint, detekt, Kover and the iOS jobs (`docs/ci_cd.md` §3). The first work item is epic E0 in `docs/backlog.md`. Check `docs/project_log.md` for the latest state.

## 3. Repository layout (target)

```
LogroBus/
├── AGENTS.md                # the rule book for every agent and human
├── CLAUDE.md, GEMINI.md     # agent pointers to AGENTS.md (no rules of their own)
├── README.md, README.es.md  # project overview, English and Spanish (kept in sync)
├── .claude/                 # Claude Code adapter: settings, hooks, reviewer agents, skills
├── .github/                 # PR templates, CODEOWNERS, Dependabot, workflows, Copilot pointer
├── .githooks/               # git hooks (installed by scripts/bootstrap.sh)
├── logrono-bus-app-functional-spec.md
├── docs/                    # living docs and ADRs
├── gradle/libs.versions.toml  # the only place versions are declared
├── build-logic/             # Gradle convention plugins
├── shared/
│   ├── core/                # common, config, telemetry, network, database, location, polling, presentation primitives, testing
│   ├── transit/             # domain/ + data/: lines, stops, arrivals, vehicles (DD rules live in transit/data)
│   ├── favourites/          # domain/ + data/: favourites, saved places, recents
│   ├── feature/             # lines, stops, nearme, planner, settings: use cases + presentation models (UiState, UI models, mappers)
│   └── umbrella/            # Koin graph, SharedDependencies for Swift, iOS framework "Shared"
├── android/                 # app/ (shell), core/ (designsystem, map, navigation), feature/<x>/ (Compose + Android ViewModels)
├── iosApp/                  # app target (shell) + Packages/LogroBusUI (DesignSystem, MapUI, Feature<X>: SwiftUI + Swift ViewModels)
├── tools/architecture-tests/  # Konsist module and package boundary rules
├── fixtures/                # golden API payloads for tests (spec §10.2)
└── scripts/                 # bootstrap.sh, checks/ (quality gates), ci/ (CI helpers), agent/ (agent hooks), github/ (repo admin), fixture tools
```

Identifiers (OQ-14): Android `applicationId` and iOS bundle id are `com.ruizurraca.logrobus`, which is also the Kotlin package root. The display name is "LogroBus" and the URL scheme is `logronobus://`. Store identifiers cannot change after the first release.

## 4. Toolchain and commands

Toolchain: JDK 21, the latest stable Android Studio with the Kotlin Multiplatform plugin, Xcode 26 or later (the Liquid Glass APIs need the iOS 26 SDK), Android `minSdk` 26, iOS deployment target 18.0 (ADR-0003). Kotlin (2.4.20), AGP (9.3.3), Compose and every library version live only in `gradle/libs.versions.toml`; the Gradle version (9.7.1) lives in the wrapper. If `ANDROID_HOME` is not set, create a git-ignored `local.properties` with `sdk.dir=…`.

These commands work today:

```bash
scripts/bootstrap.sh                              # once per clone: install git hooks, report missing tools, generate the iOS project
scripts/checks/run-all.sh                         # every quality gate (docs, secrets, shell, Gradle check; SwiftLint once iosApp exists)
scripts/agent/session-start.sh                    # session context, for agents without hooks
./gradlew ciUnitTest                              # JVM unit tests: build-logic, shared modules (Android host), Android modules (Robolectric), architecture
./gradlew :tools:architecture-tests:test          # module layout and package boundaries (Konsist)
./gradlew check                                   # every verification task, including Android lint (ktlint, detekt and Kover join in LB-002)
./gradlew build                                   # compile everything, including the release APK, and run all checks
./gradlew allTests                                # shared tests on every target (Android host, iOS simulator on macOS)
./gradlew :android:app:assembleDebug              # Android debug APK
./gradlew :android:app:installDebug               # install on a connected device or emulator
./gradlew :shared:umbrella:linkDebugFrameworkIosSimulatorArm64   # build the iOS framework only

scripts/ios/generate-project.sh                   # macOS: Shared XCFramework + Version.xcconfig + iosApp.xcodeproj (XcodeGen); rerun after Kotlin changes
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=<iPhone simulator>' test   # iOS build + bundle tests + launch UI tests
swiftlint --strict                                # Swift lint (config in .swiftlint.yml)
```

If you have just created a new source directory, run Gradle once with `--no-configuration-cache --no-watch-fs`. Both a reused configuration-cache entry and the daemon's file-system watching (seen on macOS) can miss the new directory and report the tests as `NO-SOURCE`, a false green. `scripts/checks/run-all.sh` always runs with both turned off, and CI always configures from scratch.

The commands below arrive with later stories (ktlint, detekt and Kover in LB-002; fixtures in LB-003).

```bash
./gradlew check                                   # tests + ktlint + detekt + Kover verification
./gradlew ktlintFormat                            # auto-format Kotlin

scripts/record-fixtures.sh                        # re-capture golden payloads from the live source
```

Report the real output of the checks you ran. If you could not run something, for example iOS on a non-macOS machine, say so.

## 5. Architecture rules

Details live in `docs/architecture.md`. These rules are not negotiable without an ADR.

1. **Clean architecture and feature modules (ADR-0007):**
   - Dependencies point inwards: platform presentation → shared feature (use cases and presentation models) → domain modules ← data modules. `:shared:core:*` is available to all, except that domain modules may use only `:shared:core:common`.
   - Production code never depends on `:shared:core:testing`, which may itself use core and domain modules for fakes and builders.
   - Ktor belongs to `:shared:core:network` and SQLDelight to `:shared:core:database`; data modules and the `data` packages of features may also use them.
   - `ModuleRules` in build-logic enforces these rules when a dependency is declared. `:tools:architecture-tests` enforces the package rules.
   - Domain modules never depend on Ktor, SQLDelight, data modules or platform APIs.
   - **Feature modules never depend on other feature modules**, on either platform. Cross-feature navigation goes through the app shell.
2. **DTOs stop at the data layer.** Mappers in the data modules (`:shared:transit:data`, and planner's `data` package) turn DTOs into domain entities (spec §5.1) and fix every source quirk exactly once (spec §5.3 and §5.4, `docs/api_spec.md`).
3. **The presentation layer is platform-specific, and its models are shared.**
   - Android ViewModels (Kotlin) and iOS ViewModels (Swift `@Observable`) call shared use cases and build state with the **shared** `UiState` types and pure mappers from `:shared:feature:<x>/presentation`.
   - ViewModels orchestrate (subscriptions, events, navigation effects). They contain no business rules, no networking and no ETA or formatting logic: those live in shared mappers and use cases.
   - Views only render state and send events.
4. **Shared code emits meaning, platforms emit words.** Presentation models carry semantic values such as `EtaDisplay.Minutes(4)` with `lowConfidence = true`. Each platform turns them into localised text and accessibility labels (spec §5.7) with its own string resources.
5. **Time:** never use `Clock.System.now()`, `Date()` or `System.currentTimeMillis()` for business logic. Use `AppClock.now()`, which is corrected by the server offset (spec §8.6). Store `kotlin.time.Instant` in UTC and always display `Europe/Madrid`, whatever the device time zone is.
6. **Money** is `Int` euro cents. Format it only at the UI edge, with the device locale.
7. **Polling** goes through `PollingEngine` only, subscribed by visible screens. Polling stops in the background and uses remote-config intervals with a 10 s floor. Never add an ad-hoc timer loop.
8. **Cache-backed static data, no offline mode in the MVP (ADR-0008):**
   - Lines, stops and shapes are downloaded on first use, cached in SQLDelight and replaced atomically by background sync.
   - Without data and without network, a screen shows the error state with Retry.
   - Offline mode (seed bundle, offline banner, cached arrivals) is v1.0 scope: do not build it in the MVP.
9. **Lenient networking:** decode collections item by item, and drop and log bad items. Accept any 2xx (POSTs return `201`). Treat a non-2xx status, a non-JSON body or a missing envelope key as a recoverable error for that resource only (NFR-REL-02).
10. **Data-driven, not hard-coded:** the line catalogue, colours, name overrides, night-service text, fare-tier texts, polling intervals and kill switches come from the API or from remote config (spec §8.9). There is no line 8, and the code must not assume a fixed set of lines.
11. **Errors:** repositories return `AppResult` with a typed `AppError` (`Network`, `Server`, `Data`, `NotFound`). The shared mappers turn errors into the standard UX states of spec §7.3.
12. **DI** uses Koin. The graph is assembled in `:shared:umbrella`. Android ViewModels get use cases through Koin. Swift gets them from the `SharedDependencies` factory object, never through Koin directly.
13. **Telemetry only through contracts (ADR-0009).** Use `AnalyticsTracker` (typed `AnalyticsEvent`) and `CrashReporter` from `:shared:core:telemetry`. Never reference a vendor SDK (Firebase or any other) outside the platform implementation classes.

## 6. Data rules that bite

The full list is in spec §5.3; the implementation map is in `docs/api_spec.md` §6. Each `DD-nn` rule needs at least one unit test whose name starts with its id.

- Stop ids arrive as `152`, `"0632"`, `"38"` and `"1:100"`. Strip the `1:` prefix and leading zeros, then parse to `Int`; a blank value becomes `null` (DD-01, DD-02).
- **Never join vehicle positions (VM) and arrivals (ET) by vehicle id.** They use different id spaces (DD-03).
- Directions: `asc` and `Ida` mean OUTBOUND; `desc` and `Vuelta` mean INBOUND. Numeric ET codes are inferred per spec §5.6; if they stay ambiguous, the direction is `null` (DD-04).
- VM next-stop times are Madrid local time mislabelled as `Z`. Reinterpret them only when they fall more than 30 min after `locationRecordedAtTime` (DD-05), and test both CET and CEST.
- `lineRef` can be the literal `"[object Object]"`; treat it as `lineId = null` (DD-06). Map `""` to `null` in every string field (DD-07).
- Colours: the `rgba()` value from API-01 is the source of truth (DD-08). Lines 4 and B1 share a colour, as do 9 and B2, so always show the line code with the colour.
- `predictionInaccurate: true` means a low-confidence ETA, shown with "~" and never hidden (DD-09).
- Pattern stops 383–385 are missing from the stop catalogue; create synthetic provisional stops for them (DD-10). Apply name overrides from remote config, never from code (DD-11).
- Planner responses can carry `rejection` and `routes` together; show the routes if there are any (DD-17). Planner stop ids may not exist locally (DD-18).
- Opposite sides of a street share stop names; disambiguate with the headsign, "towards X" (DD-16).

## 7. Platform UI rules

Full guidance is in `docs/design_guidelines.md`.

**Android (Compose, Material 3 Expressive):**
- Use `MaterialExpressiveTheme` with `MotionScheme.expressive()` and a fixed brand colour scheme in light and dark. Line colours are data and are never themed.
- Confine `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` to wrappers in the `designsystem` package, so an API change touches one place.
- The three tabs use `NavigationSuiteScaffold` (a navigation bar on phones, a rail on large screens), with one back stack per tab (Navigation 3).
- Build stateless composables that take `UiState` plus event lambdas, collect state with `collectAsStateWithLifecycle()`, and add a `@Preview` for each UX state. Draw edge-to-edge.

**iOS (SwiftUI, Liquid Glass):**
- Use system components first: `TabView` with `Tab`, `NavigationStack`, `.searchable`, `.sheet` with detents, and toolbars. They adopt Liquid Glass automatically on iOS 26 and later.
- Put glass only on the floating control layer: map buttons, chips floating over the map and sheet chrome. Never put it on content such as list rows, ETA rows, line badges or cards. Group neighbouring glass controls in a `GlassEffectContainer`.
- Use the `DesignSystem/` helpers (`adaptiveGlass`, glass button styles) so iOS 18–25 fall back to materials (ADR-0003). Don't call `.glassEffect` directly in feature code.
- Each screen has a Swift `@MainActor @Observable` `XViewModel` in its `Feature<X>` target. It consumes shared use cases through SKIE (`for await` inside `.task {}`) and builds the shared `UiState` with the shared mappers (ADR-0007).
- The MVP is iPhone-only: no iPad layouts or device family (NFR-PLAT-03).

**Both platforms:**
- Every data-driven screen implements the states of spec §7.3: loading, fresh, stale, expired, refreshing, empty, error and partial. The offline state comes in v1.0 (ADR-0008).
- Accessibility (NFR-A11Y): spoken labels for ETAs and line badges, 200 % font scale without truncation, touch targets of at least 48 dp or 44 pt, a list equivalent for every map task, and live updates that never steal focus.
- Strings: Spanish (es-ES) is the default and English ships in v1. No hard-coded UI text. Line and stop names are shown as delivered (after overrides) and never translated.

## 8. Coding conventions

- **Language:** follow the language policy at the top of this file. In short, everything in the repository is in technical English, and UI copy ships in es-ES and en.
- **Kotlin:** official code style, clean under ktlint and detekt. In shared modules everything is `internal` unless Swift or another module needs it. Inject dispatchers; never use `GlobalScope`.
- **Naming:** `*Dto` (network), `*Mapper` (data), `XRepository` interface in domain with `XRepositoryImpl` in data, use cases as verb phrases with `operator fun invoke` (`ObserveStopArrivals`), `<Screen>ViewModel` exposing `state: StateFlow<<Screen>UiState>` and `fun onEvent(event: <Screen>Event)`.
- **Swift-facing API:** UI models use plain types (`Int`, `String`, `Long` ARGB colours) rather than domain value classes, so they export cleanly through SKIE.
- **Swift:** Swift 6 toolchain, clean under SwiftLint, `@MainActor` for UI-facing types, small composable views. Feature targets mirror the shared feature modules: `FeatureLines`, `FeatureStops`, `FeatureNearMe`, `FeaturePlanner`, `FeatureSettings`.
- **Comments** explain why, not what. When code implements a spec rule, cite its id, for example `// DD-05: VM next-stop times are local time labelled Z`.
- **Tests:** name them `dd05_reinterpretsWallClockInMadrid_duringCest`, without backtick names containing spaces, because Kotlin/Native and older Android runtimes reject them. Swift follows the same scheme: `lb001_appIsIphoneOnly` with Swift Testing, and `test_lb001_…` with XCTest, which needs the `test` prefix.

## 9. Testing requirements

- **TDD is mandatory, with one commit per phase** (`docs/testing_strategy.md` §7):
  1. Red: code the failing test, then commit `test(scope): … [LB-xxx]`.
  2. Green: code the minimum to pass, then commit `feat|fix(scope): … [LB-xxx]`.
  3. Refactor: improve the code, then commit `refactor(scope): … [LB-xxx]`.
  4. Push.
  Tests covering acceptance criteria are named after the story (`lb205_…`).
- Unit-test every mapper, normalisation rule, ETA display rule, direction inference and fare classification. `data` and `domain` need at least 80 % line coverage (NFR-MNT-01, measured with Kover).
- Contract tests decode the golden payloads in `fixtures/`. **Unit and integration tests never call the live source.** Only the scheduled fixture-recording job and the nightly Maestro flows do.
- Repository tests use Ktor `MockEngine` and an in-memory SQLDelight driver. Use Turbine for flows, and `runTest` with `TestAppClock` for time.
- Screenshot tests cover every UX state in light, dark and 200 % font: Roborazzi on Android, swift-snapshot-testing on iOS.
- Time-zone tests cover the CET/CEST transitions on the last Sundays of March and October.

## 10. Agent workflow

Follow the playbooks in `docs/playbooks/`: `start-story.md`, `finish-story.md`, `new-adr.md`, `record-fixtures.md` and `review-checklists.md`. In Claude Code they are available as `/start-story`, `/finish-story`, `/new-adr` and `/record-fixtures`, plus the reviewer subagents (`docs/agent_harness.md`).

**Before coding:**
1. Make sure `scripts/bootstrap.sh` has been run in this clone, so the git hooks are installed. Agents without hooks run `scripts/agent/session-start.sh` themselves.
2. Find the story in `docs/backlog.md`, or add one if the owner asks for something new. Read its criteria in `docs/acceptance_criteria.md` and the requirement ids it cites.
3. Check `docs/open_questions.md`. If the work depends on an open question, implement the documented fallback or ask.

**While coding:**
- Work in small vertical slices with tests in the same change, and keep logic in shared code unless it is inherently platform-specific.
- Add every dependency to `libs.versions.toml`; a significant one also needs an ADR. When you bump Kotlin, check SKIE compatibility first.
- Be a polite client of the live source: no load tests, no polling faster than the configured intervals, and always send the `User-Agent` header.

**After coding:**
1. Run `scripts/checks/run-all.sh` (plus any relevant command from §4) and report the actual results.
2. Update the story status in `docs/backlog.md`.
3. Add an entry to `docs/project_log.md` covering what changed, the decisions taken and the follow-ups.
4. Update `README.md` and `README.es.md`. This is mandatory for every PR (§12).
5. Update `api_spec.md`, `architecture.md`, `design_guidelines.md`, an ADR or this file if what you learned changes them.

**Ask the project owner before:** changing scope or navigation, superseding an ADR, adding analytics or third-party SDKs, anything that touches legal matters (data reuse, tile licensing, attribution), or changing app identifiers.

**Never:**
- Parse `/env.js` at runtime, or reference the leaked non-production hosts listed in spec §3.4.
- Use `/assets/data/schedules.json` (DD-14) or the legacy static recharge-points file (DD-12).
- Ship OpenStreetMap's public tile servers in release builds (risk R6, OQ-10).
- Render CMS HTML in a JavaScript-enabled WebView. Sanitise it with the allow-list in NFR-SEC-02.
- Send coordinates or free text to analytics or logs (NFR-PRIV-02, NFR-OBS-03).
- Commit secrets, signing keys, keystores or `local.properties`.
- Build v2 features (accounts, TSC cards, payments, QR tickets) or call the authenticated endpoints of spec §4.14.

## 11. Definition of done

A story is done when all of these hold (spec §10.6, plus the docs):

- Its requirements and acceptance criteria are implemented and verified on Android and iOS.
- All UX states of spec §7.3 are implemented and covered by screenshot tests.
- Spoken labels have been checked with TalkBack and VoiceOver.
- Strings exist in Spanish and English.
- Analytics events are wired, consent-gated and verified in the debug view.
- ktlint, detekt and SwiftLint report no new findings, and the tests are green.
- The backlog status and project log are updated.
- `README.md` and `README.es.md` are updated and say the same thing (§12).

## 12. Git and pull request conventions

- Use Conventional Commits with the story id, for example `feat(near-me): stop sheet arrivals [LB-205]`. Scopes: `lines`, `near-me`, `planner`, `shared`, `data`, `network`, `db`, `android`, `ios`, `design`, `docs`, `backlog`, `build`, `build-logic`, `architecture`, `harness`, `ci`.
- Branch names follow `feat/LB-205-stop-sheet` or `fix/LB-xxx-short-name`, one story per branch.
- **README rule (mandatory):** every merged PR must leave `README.md` (English) and `README.es.md` (Spanish) up to date. The README changes therefore go inside the PR itself, before it is merged:
  - At minimum, add an entry under "Recent changes" / "Cambios recientes" and refresh "Project status" / "Estado del proyecto".
  - Also update every other section the PR affects (features, stack, structure, getting started).
  - The two files must stay equivalent. Never update only one language.
  - A PR without both updates is not ready to merge, even if it only touches docs.
- **Nobody pushes to `main`.** Every change goes through a branch and a pull request that uses a template (`docs/engineering_workflow.md` §3.1), and is merged by the owner with a **merge commit**. There is no squash and no rebase, so the TDD commits are preserved (`docs/engineering_workflow.md` §5).
- **CI runs on every pull request** now that the repository is public (`docs/ci_cd.md` §1), and its required checks must pass before merging. CI would not run on a private repository (owner rule). The local gates still run first: the git hooks plus `scripts/checks/run-all.sh`, whose summary must be pasted into the PR, because it covers the checks CI does not run yet.
- Agents commit or push only when asked.
