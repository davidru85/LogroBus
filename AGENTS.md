# AGENTS.md

Guidance for AI coding agents and human contributors working on **LogroBus**. This is the canonical instruction file: `CLAUDE.md` only points here. Keep this file short, current and actionable; long-form material belongs in `docs/`.

## 1. The project in one minute

LogroBus is a native Android and iOS app for Logroño's urban bus network: 13 day and night (Búho) lines, about 254 stops, real-time arrivals, live vehicles and door-to-door journey planning. Data comes from the public but undocumented and unversioned API behind `https://transporteurbano.logrono.es` (an Express BFF over SIRI real-time feeds, OpenTripPlanner and a Strapi CMS).

| Layer | Technology |
| --- | --- |
| Shared core | Kotlin Multiplatform (KMP): domain, data, networking, persistence, normalisation, ViewModels |
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

When documents disagree, this order wins: the project owner's explicit instruction in the current task, then accepted ADRs, then this file, then the other `docs/`, then the functional spec. Fix the lower-ranked document in the same change, or raise an open question if the conflict is not yours to settle.

**Current status (2026-10-22):** documentation only, committed on `main`. No Gradle or Xcode project exists yet. The first work item is epic E0 in `docs/backlog.md`. Check `docs/project_log.md` for the latest state.

## 3. Repository layout (target)

```
LogroBus/
├── AGENTS.md, CLAUDE.md
├── README.md, README.es.md  # project overview, English and Spanish (kept in sync)
├── logrono-bus-app-functional-spec.md
├── docs/                    # living docs and ADRs
├── gradle/libs.versions.toml  # the only place versions are declared
├── build-logic/             # Gradle convention plugins
├── shared/
│   ├── core/                # AppClock, Logger, dispatchers, AppResult, RemoteConfig, LocationProvider API
│   ├── domain/              # entities, repository interfaces, use cases (no I/O, no platform APIs)
│   ├── network/             # Ktor clients, DTOs, lenient decoding
│   ├── database/            # SQLDelight schema and queries
│   ├── data/                # repositories, mappers, normaliser, sync and polling engines
│   ├── presentation/        # shared ViewModels, UiState, display models
│   ├── testing/             # fakes, TestAppClock, fixture loader
│   └── umbrella/            # Koin graph + iOS framework "Shared"
├── androidApp/              # Compose UI, navigation, design system, MapLibre, WorkManager
├── iosApp/                  # Xcode project: SwiftUI UI, design system, MapLibre, ViewModel bridge
├── fixtures/                # golden API payloads for tests (spec §10.2)
├── seed/                    # static data snapshot bundled into both apps (FR-ERR-01)
└── scripts/                 # fixture recording, seed refresh
```

Identifiers (OQ-14): Android `applicationId` and iOS bundle id are `com.ruizurraca.logrobus`, which is also the Kotlin package root. The display name is "LogroBus" and the URL scheme is `logronobus://`. Store identifiers cannot change after the first release.

## 4. Toolchain and commands

Toolchain: JDK 21, the latest stable Android Studio with the Kotlin Multiplatform plugin, Xcode 26 or later (the Liquid Glass APIs need the iOS 26 SDK), Android `minSdk` 26, iOS deployment target 18.0 (ADR-0003). Kotlin, AGP, Compose and every library version live only in `gradle/libs.versions.toml`.

The commands below describe the target setup. **Verify them, and correct this section, when LB-001 lands.**

```bash
./gradlew build                                   # compile everything and run all checks
./gradlew allTests                                # shared tests on every target
./gradlew check                                   # tests + ktlint + detekt + Kover verification
./gradlew ktlintFormat                            # auto-format Kotlin
./gradlew :androidApp:assembleDebug               # Android debug APK
./gradlew :androidApp:installDebug                # install on a connected device or emulator
./gradlew :shared:umbrella:linkDebugFrameworkIosSimulatorArm64   # build the iOS framework only

xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=<simulator>' test   # iOS build + tests
(cd iosApp && swiftlint --strict)                 # Swift lint

scripts/record-fixtures.sh                        # re-capture golden payloads from the live source
```

Report the real output of the checks you ran. If you could not run something, for example iOS on a non-macOS machine, say so.

## 5. Architecture rules

Details live in `docs/architecture.md`. These rules are not negotiable without an ADR.

1. **Dependency direction:** UI → `presentation` → `domain` ← `data` → `network`, `database`. Everything may use `core`. `domain` never depends on Ktor, SQLDelight, `network`, `database` or platform APIs.
2. **DTOs stop at the data layer.** Mappers in `:shared:data` turn DTOs into domain entities (spec §5.1) and fix every source quirk exactly once (spec §5.3 and §5.4, `docs/api_spec.md`).
3. **UI only renders.** Compose and SwiftUI screens render `UiState` from shared ViewModels and send events back. They contain no business rules, networking or ETA logic. Platform code owns navigation, permission prompts, map rendering, localised strings and OS services (widgets, background tasks).
4. **Shared code emits meaning, platforms emit words.** Presentation exposes semantic display models such as `EtaDisplay.Minutes(4, lowConfidence = true)`. Each platform turns them into localised text and accessibility labels (spec §5.7) with its own string resources.
5. **Time:** never use `Clock.System.now()`, `Date()` or `System.currentTimeMillis()` for business logic. Use `AppClock.now()`, which is corrected by the server offset (spec §8.6). Store `kotlin.time.Instant` in UTC and always display `Europe/Madrid`, whatever the device time zone is.
6. **Money** is `Int` euro cents. Format it only at the UI edge, with the device locale.
7. **Polling** goes through `PollingEngine` only, subscribed by visible screens. Polling stops in the background and uses remote-config intervals with a 10 s floor. Never add an ad-hoc timer loop.
8. **Offline-first:** lines, stops, shapes, fares and recharge points are read from SQLDelight, seeded from `seed/` on first launch, and replaced atomically by background sync. Screens never wait on the network for static data.
9. **Lenient networking:** decode collections item by item, and drop and log bad items. Accept any 2xx (POSTs return `201`). Treat a non-2xx status, a non-JSON body or a missing envelope key as a recoverable error for that resource only (NFR-REL-02).
10. **Data-driven, not hard-coded:** the line catalogue, colours, name overrides, night-service text, fare-tier texts, polling intervals and kill switches come from the API or from remote config (spec §8.9). There is no line 8, and the code must not assume a fixed set of lines.
11. **Errors:** repositories return `AppResult` with a typed `AppError` (`Network`, `Server`, `Data`, `NotFound`). ViewModels map errors to the standard UX states of spec §7.3.
12. **DI** uses Koin. The graph is assembled in `:shared:umbrella`. Swift obtains ViewModels through the `ViewModels` factory object, never through Koin directly.

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
- Views observe shared ViewModels only through the `@Observable` wrappers in `iosApp/Bridge/` (ADR-0004).

**Both platforms:**
- Every data-driven screen implements the states of spec §7.3: loading, fresh, stale, expired, refreshing, empty, offline, error and partial.
- Accessibility (NFR-A11Y): spoken labels for ETAs and line badges, 200 % font scale without truncation, touch targets of at least 48 dp or 44 pt, a list equivalent for every map task, and live updates that never steal focus.
- Strings: Spanish (es-ES) is the default and English ships in v1. No hard-coded UI text. Line and stop names are shown as delivered (after overrides) and never translated.

## 8. Coding conventions

- **Language:** code, comments, commits and docs are in English. UI copy is in es and en.
- **Kotlin:** official code style, clean under ktlint and detekt. In shared modules everything is `internal` unless Swift or another module needs it. Inject dispatchers; never use `GlobalScope`.
- **Naming:** `*Dto` (network), `*Mapper` (data), `XRepository` interface in domain with `XRepositoryImpl` in data, use cases as verb phrases with `operator fun invoke` (`ObserveStopArrivals`), `<Screen>ViewModel` exposing `state: StateFlow<<Screen>UiState>` and `fun onEvent(event: <Screen>Event)`.
- **Swift-facing API:** UI models use plain types (`Int`, `String`, `Long` ARGB colours) rather than domain value classes, so they export cleanly through SKIE.
- **Swift:** Swift 6 toolchain, clean under SwiftLint, `@MainActor` for UI-facing types, small composable views. Feature folders mirror the three sections: `Features/Lines`, `Features/NearMe`, `Features/TripPlanner`.
- **Comments** explain why, not what. When code implements a spec rule, cite its id, for example `// DD-05: VM next-stop times are local time labelled Z`.
- **Tests:** name them `dd05_reinterpretsWallClockInMadrid_duringCest`, without backtick names containing spaces, because Kotlin/Native and older Android runtimes reject them.

## 9. Testing requirements

- Unit-test every mapper, normalisation rule, ETA display rule, direction inference and fare classification. `data` and `domain` need at least 80 % line coverage (NFR-MNT-01, measured with Kover).
- Contract tests decode the golden payloads in `fixtures/`. **Unit and integration tests never call the live source.** Only the scheduled fixture-recording job and the nightly Maestro flows do.
- Repository tests use Ktor `MockEngine` and an in-memory SQLDelight driver. Use Turbine for flows, and `runTest` with `TestAppClock` for time.
- Screenshot tests cover every UX state in light, dark and 200 % font: Roborazzi on Android, swift-snapshot-testing on iOS.
- Time-zone tests cover the CET/CEST transitions on the last Sundays of March and October.

## 10. Agent workflow

**Before coding:**
1. Find the story in `docs/backlog.md`, or add one if the owner asks for something new, and read the spec sections it references.
2. Check `docs/open_questions.md`. If the work depends on an open question, implement the documented fallback or ask.

**While coding:**
- Work in small vertical slices with tests in the same change, and keep logic in shared code unless it is inherently platform-specific.
- Add every dependency to `libs.versions.toml`; a significant one also needs an ADR. When you bump Kotlin, check SKIE compatibility first.
- Be a polite client of the live source: no load tests, no polling faster than the configured intervals, and always send the `User-Agent` header.

**After coding:**
1. Run the relevant checks from §4 and report the actual results.
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

- Use Conventional Commits with the story id, for example `feat(near-me): stop sheet arrivals [LB-205]`. Scopes: `lines`, `near-me`, `planner`, `shared`, `data`, `network`, `db`, `android`, `ios`, `design`, `docs`, `build`, `ci`.
- Branch names follow `feat/LB-205-stop-sheet` or `fix/LB-xxx-short-name`, one story per branch.
- **README rule (mandatory):** every merged PR must leave `README.md` (English) and `README.es.md` (Spanish) up to date. The README changes therefore go inside the PR itself, before it is merged:
  - At minimum, add an entry under "Recent changes" / "Cambios recientes" and refresh "Project status" / "Estado del proyecto".
  - Also update every other section the PR affects (features, stack, structure, getting started).
  - The two files must stay equivalent. Never update only one language.
  - A PR without both updates is not ready to merge, even if it only touches docs.
- Agents commit or push only when asked.
