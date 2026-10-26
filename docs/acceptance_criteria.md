# Acceptance criteria

| Field | Value |
| --- | --- |
| Status | Baseline 1.0 for the MVP (M0–M4) |
| Last updated | 2026-10-22 |
| Based on | [`requirements.md`](requirements.md), spec §6 and §9, [`backlog.md`](backlog.md) |

Verifiable acceptance criteria for every MVP story (epics E0–E4). A story is done only when all of its criteria pass **on both platforms** and the definition of done in `AGENTS.md` §11 is met.

**Format rules**
- Feature stories use one-line scenarios: **Given** a context, **when** an action happens, **then** an observable result follows. Technical stories use a **Done when** checklist.
- Each criterion is checked by an automated test (unit, integration, UI, screenshot, benchmark or Maestro) or by a named manual check, such as `[manual: TalkBack]` or `[manual: VoiceOver]`.
- Criteria apply to both platforms unless marked `[Android]` or `[iOS]`.
- `Refs:` cites the `FR-`, `NFR-`, `DD-` and `OQ-` ids behind the story. Where the spec already states an acceptance criterion, it is reused verbatim and marked *(spec)*.
- Fixture names refer to `fixtures/` (`api_spec.md` §9).
- Freshness follows spec §5.7: under 45 s is fresh, 45 to 120 s is stale, over 120 s is expired.
- Times are shown in `Europe/Madrid`. User-facing texts are quoted in English; the Spanish equivalents come from the string resources.

---

## E0 Foundations

### LB-001 Scaffold the KMP project
Refs: spec §8.2, NFR-PLAT-02, NFR-PLAT-03, ADR-0001, ADR-0004, ADR-0007, OQ-14

Done when:
- `./gradlew build` succeeds on a clean clone with JDK 21 and the Android SDK, and no other local setup.
- The modules match `architecture.md` §3.1: shared `core`, `transit`, `favourites`, `feature/*` and `umbrella`; Android `app`, `core/*` and `feature/*`; the iOS app target with the `LogroBusUI` package.
- Konsist and Gradle checks fail the build when:
  - a feature module depends on another feature module (on either platform);
  - a domain module depends on a data module, on Ktor or SQLDelight, or on a platform API;
  - a shared feature module imports another feature's packages.
- Versions are declared only in `gradle/libs.versions.toml`; build scripts contain no version literals. Every module has the `ciUnitTest` alias task.
- The Android `applicationId` and the iOS bundle id are both `com.ruizurraca.logrobus`. Android `minSdk` is 26, the iOS deployment target is 18.0, and the iOS device family is iPhone only (`TARGETED_DEVICE_FAMILY = 1`).
- `iosApp` builds in Xcode 26 or later without CocoaPods. The feature targets consume `Shared` as an XCFramework binary target, or the fallback (feature folders in the app target) is recorded in `architecture.md` §5.2.
- Both apps launch on an emulator and an iPhone simulator without crashing.

### LB-002 CI pipeline
Refs: NFR-MNT-01, NFR-MNT-02, NFR-MNT-05, NFR-SEC-04, NFR-SEC-05

Done when:
- While the repository is private, every workflow job is skipped. Once it is public (LB-415), every pull request runs the atomic Linux jobs: per-module unit tests (`ciUnitTest`), `:android:app:assembleDebug`, ktlint, detekt, and Kover verification (at least 80 % line coverage on `data` and `domain`).
- Pull requests that touch shared or iOS code run the macOS job: framework build, iOS build and tests, and `swiftlint --strict`.
- The dependency scan runs on every pull request and fails on known critical vulnerabilities.
- A deliberately failing test, a lint violation and a coverage drop each make their job fail (verified once with throwaway branches).
- Every CI step calls a script or a Gradle task that also runs locally (NFR-MNT-05).
- No secret, keystore or `local.properties` is committed; a secret-pattern scan runs in CI.

Verified on 2026-10-25, and the story is done:
- **Probes:** PRs #14–#16 were closed unmerged.
  - A regression failed the shared, Android and iOS tests.
  - An indentation fault failed `ktlint`.
  - An untested object failed `Coverage` (45.5 % against 80 %).
  - Each one failed its gate.
- **Dependency scan:** it runs on every pull request that touches the Kotlin area, which holds every Gradle dependency, and Dependabot alerts cover `main` (`ci_cd.md` §6).
- **Moved to other stories (owner decision, 2026-10-25):**
  - the screenshot jobs, to LB-011;
  - the iOS ViewModel test job, to LB-101;
  - the nightly workflow, to LB-412.

### LB-003 Golden fixtures and recorder
Refs: spec §10.2, `api_spec.md` §9, NFR-PRIV-02

Done when:
- `scripts/record-fixtures.sh` captures every fixture listed in spec §10.2, plus `geocoder_candidates_logrono.json` and `geocoder_find.json`, and sends the project `User-Agent`.
- Captured JSON is pretty-printed with a stable key order, so re-recording unchanged data produces no diff.
- A fixture with at least one `"[object Object]"` `lineRef` exists. If the live capture lacks one, a hand-made copy prefixed `synthetic_` is added.
- The weekly CI job re-records the fixtures and fails with a readable diff when a field is added, removed or changes type.
- No unit or integration test calls the live source. A test-scope network guard fails any real HTTP request.

### LB-004 `:shared:core` common, config and location
Refs: spec §8.6, §8.9, FR-ERR-06, NFR-REL-05

Done when:
- `AppClock.now()` applies `serverOffset`, smoothed over the last 5 `Date` samples. With the device clock fixed 90 s behind the server, the instants returned are server-corrected.
- `RemoteConfig` returns the compiled default for every key in `api_spec.md` §8 when no backend is configured.
- Polling intervals under 10 000 ms are clamped to 10 000 ms.
- `AppError` covers `Network`, `Server(status)`, `Data` and `NotFound`, and `AppResult` is exhaustively matchable.
- Geo utilities are tested:
  - Haversine distance is within 0.5 % of a reference value.
  - Polylines round-trip at precision 5 and 6.
  - The bounding box expands correctly by 2 km and 5 km.

Part 2 (added on 2026-10-26, when it started), also done when:
- `Logger` has a no-op implementation and a console one. The console one prints the level, tag and message, and the types of any throwable's cause chain but never their messages (NFR-PRIV-02). It drops entries below its minimum level.
- `AppDispatchers` bundles the main, default and I/O dispatchers, so that code receives them by injection and a test can replace all three with one test dispatcher.
- `AppLifecycle` exposes whether the app is in the foreground as a `StateFlow`: `[Android]` from `ProcessLifecycleOwner` (started means foreground); `[iOS]` from the `UIApplication` enter-background and enter-foreground notifications.
- `LocationProvider` (FR-LOC-03, NFR-PRIV-02):
  - It reports the location permission as not granted, approximate or precise. Requesting it is left to the UI (LB-201).
  - Location updates start when its flow is collected and stop when the collection is cancelled.
  - Without permission, the flow completes without starting updates. It also completes when location is gone for good: the permission revoked (iOS) or the platform service failed (Android).
  - `[Android]` uses the Fused Location Provider; `[iOS]` uses Core Location.
  - A location fix never prints its coordinates.

### LB-005 `:shared:core:network` and DTOs
Refs: `api_spec.md` §1–4, DD-07, DD-15, NFR-SEC-01, NFR-REL-02, NFR-PRIV-02

Done when:
- Every request sends `User-Agent: LogronoBus/<version> (<platform> <os-version>)` and `Accept-Encoding: gzip`. Timeouts are 5 s to connect, 10 s for live requests and 30 s for static ones (asserted with `MockEngine`).
- `201` responses from API-07 and API-08 decode as success (`dd15_` tests). A non-2xx status maps to `Server(status)`; a non-JSON body or a missing envelope maps to `Data`.
- A collection containing one malformed item still yields every other item and reports exactly one dropped item.
- DTOs for API-01 to API-08 decode every LB-003 fixture (contract tests).
- Cleartext HTTP is impossible: `[Android]` `cleartextTrafficPermitted=false`; `[iOS]` default ATS with no exceptions.
- No log line contains request bodies or coordinates.

### LB-006 Normaliser and mappers
Refs: spec §5.3–5.6, DD-01 to DD-11, DD-15 to DD-18, NFR-MNT-01

Done when:
- Every listed DD rule has at least one passing test named with its prefix (`dd01_` … `dd18_`).
- The `dd05_` tests cover CET (UTC+1), CEST (UTC+2) and the last Sundays of March and October. A VM time within 30 min of `locationRecordedAtTime` is left uncorrected (risk R4).
- `4-PALACIO DE CONGRESOS-PRADOVIEJOO`, with the override `PRADOVIEJOO→PRADOVIEJO`, yields the display name "Palacio de Congresos - Pradoviejo" *(spec)*. `B3-LARDERO-EL CAMPILLO` yields kind `NIGHT`.
- Codes 401 and 402 in `et_line1_60min.json` resolve to opposite directions through the algorithm of spec §5.6 and are stored in `direction_code_cache`. An ambiguous code yields `direction = null`.
- Stops 383, 384 and 385 exist as synthetic provisional stops. Items outside the service bounding box expanded by 2 km are dropped and counted in the `DataQualityReport`.
- Line coverage of `data` and `domain` is at least 80 %.

### LB-007 `:shared:core:database`
Refs: spec §8.8, `architecture.md` §4.3, FR-STOP-01, NFR-I18N-03, NFR-PERF-03

Done when:
- The schema contains spec §8.8 plus `stop.normalized_name`, `favourite.missing_syncs`, `plan_recent` and `arrival_snapshot`.
- Search matches regardless of accents, `ñ` and case: "logrono" matches "Logroño", "AYUNTAMIENTO" matches "Ayuntamiento", and the query "38" matches stop 38 by id.
- A search over the full 254-stop dataset returns in under 100 ms on the reference devices (benchmark).
- Migrations are versioned. A migration test upgrades from the previous schema without losing rows in `favourite`, `place` or `plan_recent`.

### LB-008 `StaticSyncEngine`
Refs: FR-ERR-04, FR-ERR-05, NFR-REL-03, spec §8.5, FR-FAV-04, ADR-0008

Done when:
- On first use with an empty cache and network available, lines, stops and shapes are downloaded, normalised and stored in one transaction. With no network, the requesting screen gets a `Network` error, and a later Retry succeeds once connectivity returns.
- No seed data is bundled or imported in the MVP (ADR-0008).
- A payload with fewer than 5 lines or 100 stops is rejected, and the previous cache is kept intact.
- An unchanged payload (same SHA-256) writes nothing apart from `sync_state.last_success`.
- An exception injected mid-write rolls back the transaction; concurrent readers never see a partial dataset.
- A failed background refresh shows no UI error and is retried at the next trigger. The TTL for lines and stops is 24 h.
- `favourite.missing_syncs` increases with every sync in which a saved stop is absent, and resets when the stop reappears.

### LB-009 `PollingEngine`
Refs: spec §8.6, FR-ERR-03, NFR-PERF-07

Done when:
- Two collectors of the same `PollKey` produce one upstream request per interval.
- Fetch times fall within the interval ±1 s of jitter (virtual-time test).
- After repeated failures the retry delays are 2, 4, 8, 16 and then 30 s. The last good value stays available, with `lastError` set.
- Moving `AppLifecycle` to the background stops all requests; returning to the foreground triggers an immediate fetch.
- No request is made while no collector is subscribed.

### LB-010 DI and platform presentation conventions
Refs: ADR-0004, ADR-0007, `architecture.md` §5

Done when:
- `initKoin()` runs exactly once at startup on each platform, and a Koin graph verification test passes.
- `[Android]` Each screen's `XViewModel` lives in its `:android:feature:<x>` module, is obtained with `koinViewModel()`, survives configuration changes, and builds state only with the shared `UiState` types and mappers.
- `[iOS]` Each Swift `XViewModel` (`@MainActor @Observable`) gets use cases only from `SharedDependencies`, collects them with `for await` inside `.task {}`, and builds state with the shared mappers. Views switch over the state exhaustively with `onEnum(of:)`.
- `[iOS]` Leaving a screen cancels its observation task, and a test asserts that the upstream collection stops.
- `SharedDependencies.preview` provides scripted fakes, used by SwiftUI previews and Swift ViewModel tests.
- The Kotlin and SKIE versions in the catalog are a pair documented as compatible.

### LB-011 Design system foundations
Refs: NFR-MNT-04, NFR-PLAT-01, NFR-A11Y-06, OQ-18, `design_guidelines.md` §4–6

Done when:
- Brand colours, typography and shapes live in one token file per platform. The neutral placeholder theme (OQ-18) is defined there and nowhere else.
- `LineBadge` gives every one of the 13 current line colours an `onColor` with at least 4.5:1 contrast (a unit test plus screenshots).
- `EtaText` renders every case of spec §5.7 with tabular figures and the spoken labels of `design_guidelines.md` §6.3.
- `[Android]` `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` appears only in the `designsystem` package (static check).
- `[iOS]` `adaptiveGlass` and the glass button styles render Liquid Glass on iOS 26 and the material fallback on iOS 18 (snapshots on both).
- Every component has light, dark and 200 % font snapshots.
- CI runs the Android screenshot tests (`verifyRoborazziDebug`, a matrix per module) and the iOS snapshot tests in their own jobs, behind the Kotlin and iOS gates (moved from LB-002).

### LB-012 App shell
Refs: FR-NAV-01 to 05, FR-NAV-07, ADR-0002, ADR-0005

- **Given** a cold start without a deep link, **when** the app opens, **then** the Lines tab is selected and the tabs show the labels of `design_guidelines.md` §2.1, in this order: Líneas, Cerca de mí, Cómo llegar (es) and Lines, Near me, Directions (en).
- **Given** the user is two screens deep in Lines, **when** they switch to Near Me and back, **then** Lines shows the same screen. **When** they re-select Lines, **then** it pops to its root, and re-selecting it at the root scrolls to the top.
- **Given** any tab root, **when** the user taps the More action, **then** More opens and lists Settings and About data.
- **Given** `[Android]` a window of medium width or wider, **when** the shell renders, **then** a navigation rail replaces the bottom bar.
- **Given** TalkBack or VoiceOver, **when** focus reaches the tab bar, **then** each tab announces its label and selected state. `[manual: TalkBack, VoiceOver]`
- **Given** a deep link that targets another tab (LB-603, v1.0), **when** the app is launched from it, **then** that tab opens instead of Lines. This criterion is verified when LB-603 lands.

### LB-013 Map foundation
Refs: spec §7.4, OQ-10, NFR-PRIV-05, NFR-PERF-04

Done when:
- MapLibre renders on both platforms with the style from `map_style_url`, and changing the value switches the style without a new build.
- Dark mode uses a dark base style.
- The OSM attribution and "Data: Ayuntamiento de Logroño" are visible on every map.
- Without a user location, the initial camera frames the service bounding box (lat 42.4219 to 42.4925, lng -2.5206 to -2.4050) with 32 dp / pt of padding.
- A build-time check fails any release configuration that points at OSM's public tile servers.
- With all 254 stops shown, panning holds 60 fps on the reference devices.

### LB-014 ADRs for the spec §8.3 choices
Refs: NFR-MNT-03

Done when:
- `docs/adr/` has one ADR, written with the template, for each remaining spec §8.3 choice: Ktor, SQLDelight, Koin, kotlinx-datetime, Coil 3, MapLibre, the analytics and crash backend (once OQ-16 is decided) and Multiplatform Settings.
- Each ADR names at least one rejected alternative and the reason for rejecting it.
- `docs/adr/README.md` lists every ADR with its status.

### LB-015 Remote config backend
Refs: NFR-REL-05, OQ-16, spec §8.9

Done when:
- The backend decision is recorded in an ADR, and OQ-16 is marked resolved.
- The implementation sits behind `RemoteConfig`. With the backend absent or unreachable, the app is fully functional on the compiled defaults.
- Fetched values are cached and applied at the next app start. A malformed value falls back to its default and is logged.
- `name_overrides`, `night_service_text` and `fare_tier_texts` resolve in es and en.

---

### LB-016 Multi-agent harness and local quality gates
Refs: NFR-MNT-05, ADR-0006

Done when:
- `scripts/checks/run-all.sh` runs every gate from a shell, and each gate is proven with a negative case.
- The git hooks block non-conventional commit subjects, credential-like content, direct pushes to `main`, frozen-spec edits without an ADR, and broken doc links.
- Claude Code, Gemini CLI and Copilot load `AGENTS.md` through their adapters, and no rules are duplicated.
- The policy workflow's jobs are skipped while the repository is private.

### LB-017 Telemetry contracts
Refs: ADR-0009, NFR-OBS-03, NFR-PRIV-02, NFR-PRIV-03

Done when:
- `AnalyticsEvent` covers the NFR-OBS-03 MVP events. A Konsist rule fails if any event property is a free-form `String`, or a coordinate (`LatLng`, `Double` latitude or longitude).
- `ConsentGatedAnalyticsTracker` and `ConsentGatedCrashReporter` forward nothing until consent is granted (unit tests).
- Release builds bind the no-op implementations, and debug builds bind the logging ones. No vendor SDK appears in any shared module's dependencies.
- `[Android]` and `[iOS]` each have a documented injection point (Koin start-up parameter) that accepts a native implementation, proven with a test double.

## E1 Lines

### LB-101 Lines list
Refs: FR-LINE-01, FR-LINE-02, DD-08, NFR-A11Y-02

- **Given** `lines_2026-10-22.json`, **when** Lines opens, **then** "Day lines" lists 1, 2, 3, 4, 5, 6, 7, 9, 10, 11 and "Night lines (Búho)" lists B1, B2, B3, in that order, and there is no line 8.
- **Given** a line whose `status` is not 1, **when** the list renders, **then** its row shows a status chip.
- **Given** lines 4 and B1, which share a colour, **when** both rows render, **then** each badge shows its own code and the screen reader says "Line 4" and "Line B1", never a colour.
- **Given** 200 % font, **when** the list renders, **then** no line code is truncated, and display names wrap rather than clip.
- **Given** a first launch with no network, **when** Lines opens, **then** the error state shows the network reason and Retry, and Retry loads the list once connectivity returns (ADR-0008).
- **Given** the first Swift ViewModel, `LinesViewModel`, **when** a pull request changes it, **then** CI runs its tests in the `iOS ViewModel and snapshot tests` job, behind the iOS gate (moved from LB-002).

### LB-102 Lines search
Refs: FR-LINE-03, FR-LINE-16, NFR-I18N-03

- **Given** the query "pradoviejo", **when** it is typed, **then** lines 4 and 9 match.
- **Given** the query "yague", without the accent, **when** it is typed, **then** line 2 (Yagüe - Varea) matches.
- **Given** the stop name "Iglesia de Lardero", **when** it is typed, **then** every line whose patterns include that stop is listed, including 1 and B3.
- **Given** an active query, **when** results show, **then** the favourites carousel is hidden and the Day / Night grouping is kept. **When** the query is cleared, **then** the carousel returns.
- **Given** a query with no matches, **when** results update, **then** an empty state is shown with a single "Clear search" action.

### LB-103 Favourite lines
Refs: FR-LINE-09, FR-LINE-12 to 15, FR-FAV-01

- **Given** no favourite lines, **when** Lines opens, **then** "Your favourite lines" shows a single hint card.
- **Given** the user taps the favourite toggle of line 3, **when** it changes, **then** a line 3 card appears immediately at the end of the carousel, and the toggle's label changes from "Add line 3 to favourites" to "Remove line 3 from favourites".
- **Given** line 10 is a favourite, **when** its card renders, **then** it shows the badge, "El Arco - Hospital San Pedro" and both headsigns, and tapping it opens line 10 detail.
- **Given** the user adds line 3 to favourites from line detail, **when** they go back to Lines, **then** the carousel includes line 3.
- **Given** the app is restarted, **when** Lines opens, **then** the favourites and their order are preserved.

### LB-104 Line detail
Refs: FR-LINE-04, FR-NAV-07, DD-16

- **Given** line 10, **when** detail opens, **then** the map shows both shapes, with the selected direction emphasised and the other at 40 % opacity.
- **Given** the direction toggle, **when** it renders, **then** each option reads "Towards <headsign>", where the headsign is the last stop of that pattern.
- **Given** INBOUND is selected, **when** the stop list renders, **then** it shows the INBOUND pattern's stops in sequence order.
- **Given** a stop row, **when** it is tapped, **then** stop detail is pushed onto the Lines back stack.
- **Given** a screen reader, **when** focus reaches the toggle, **then** both options and the selected one are announced. `[manual: TalkBack, VoiceOver]`

### LB-105 Next arrival per stop in line detail
Refs: FR-LINE-05, spec §5.7, DD-09

- **Given** API-05 reports line 1 at stop 38 in 4 min, **when** line 1 detail shows that direction, **then** stop 38's row shows "4 min".
- **Given** a stop with no arrival within 30 min, **when** its row renders, **then** it shows "–".
- **Given** an arrival with `predictionInaccurate: true`, **when** it renders, **then** it shows a "~" prefix, and its spoken label starts with "approximately".
- **Given** line detail is visible, **when** time passes, **then** API-05 is requested with `previewMinutes=30` every 20 s, and requests stop when the screen is left.

### LB-106 Live vehicles on the line map
Refs: FR-LINE-06, DD-04, DD-06

- **Given** API-04 returns two consecutive positions for vehicle 9693, **when** it is drawn, **then** its marker points along the bearing between them.
- **Given** a vehicle with `directionRef: "Ida"`, **when** INBOUND is selected, **then** the vehicle is hidden. **When** OUTBOUND is selected, **then** it is shown.
- **Given** a vehicle on line 1 with `directionRef ""`, **when** either direction is selected, **then** it is shown with a neutral marker *(spec)*.
- **Given** a vehicle with `lineRef "[object Object]"`, **when** any line map renders, **then** that vehicle is not shown.
- **Given** line detail is left, **when** the screen disappears, **then** API-04 polling stops.

### LB-107 Vehicle animation and grace period
Refs: FR-LINE-07

- **Given** two reports 10 s apart, **when** the second arrives, **then** the marker glides along the pattern shape at the `BUS_TICK_INTERVAL_MS` rate (1 s) instead of jumping.
- **Given** no VM report for a vehicle for 46 s, **then** its marker is removed *(spec)*.
- **Given** reduced motion is enabled, **when** a new report arrives, **then** the marker moves without animation.

### LB-108 Vehicle sheet
Refs: FR-LINE-08, DD-03, DD-05

- **Given** a vehicle is tapped, **when** the sheet opens, **then** it shows the line badge, headsign, next stop name, delay (for example "+4 min late") and report age (for example "12 s ago").
- **Given** `nextStopRef` is empty, **when** the sheet renders, **then** the next-stop row is hidden.
- **Given** a VM next-stop time ending in `Z` that is more than 30 min after `locationRecordedAtTime`, during CEST, **when** the sheet renders, **then** it shows the time corrected to Madrid.
- **Given** a vehicle number is shown, **when** its source is checked, **then** it is the VM fleet number and never an ET vehicle id.

### LB-109 Provisional stops
Refs: FR-LINE-11, DD-10, DD-11

- **Given** a pattern that contains stop 383, **when** the stop list renders, **then** the row reads "Provisional Templarios" (trimmed), with a "Provisional" tag, and the map uses the provisional marker.
- **Given** a screen reader, **when** focus reaches that row, **then** it announces "Provisional stop". `[manual: TalkBack, VoiceOver]`

### LB-110 Edit favourite lines
Refs: FR-FAV-02, FR-FAV-05

- **Given** three favourite lines, **when** the user opens Edit and drags the third to the first position, **then** the carousel order updates and persists across restarts.
- **Given** edit mode, **when** a line is removed, **then** it leaves the carousel and the toggle on its row becomes unselected.
- **Given** 20 favourite lines, **when** the user tries to add a 21st, **then** a limit message is shown and nothing is added.
- **Given** a screen reader, **when** reordering, **then** "Move up" and "Move down" accessibility actions work without dragging. `[manual: TalkBack, VoiceOver]`

---

## E2 Near Me

### LB-201 Location permission and provider
Refs: FR-LOC-01 to 03, FR-NEAR-06, NFR-PRIV-02

- **Given** a fresh install, **when** the user finishes onboarding and opens Near Me, **then** no permission prompt appears until they tap "Turn on location" or recenter.
- **Given** a permission request, **when** the system prompt appears, **then** only foreground location is requested and approximate location is accepted: `[Android]` coarse, with fine optional; `[iOS]` When In Use.
- **Given** permission is denied, **when** Near Me renders, **then** the map frames the service area, "Near you" becomes the row "Turn on location to see nearby stops", and saved stops, pins and stop detail keep working.
- **Given** the user leaves Near Me and no other visible screen needs location, **when** the tab changes, **then** location updates stop (verified with a provider fake).
- **Given** a location fix, **when** logs and analytics are inspected, **then** they contain no coordinates.

### LB-202 Near Me map
Refs: FR-NEAR-01, FR-NEAR-08, FR-STOP-10, spec §7.4

- **Given** permission is granted and the user is inside the service area, **when** Near Me opens, **then** the camera centres on the user at zoom 16.
- **Given** no permission, or the user is outside the service area, **when** Near Me opens, **then** the camera frames the service bounding box.
- **Given** a zoom below 15, **when** the map renders, **then** stops are clustered. **Given** a zoom above 16, **then** individual markers with line badges are shown.
- **Given** no permission, **when** the user taps recenter, **then** the permission flow starts. **Given** permission, **then** the camera moves to the user.
- **Given** dark mode, **when** Near Me renders, **then** the map uses the dark base style.

### LB-203 Bottom sheet and "Near you"
Refs: FR-NEAR-02, FR-NEAR-03, FR-HOME-03, FR-STOP-11, NFR-A11Y-04

- **Given** Near Me opens, **when** the sheet appears, **then** it starts at peek and cannot be dismissed, and the map stays pannable at peek and medium.
- **Given** the user is at 42.4650, -2.4450, **when** "Near you" loads, **then** it lists at most 5 stops within 800 m, sorted by straight-line distance and re-sorted by walking distance once API-08 answers.
- **Given** more than 40 stops within range, **when** API-08 is called, **then** it receives only the 40 nearest.
- **Given** the user moves less than 50 m, **then** API-08 is not called again. **Given** they move more than 50 m, **then** it is.
- **Given** a screen reader, **when** the user explores the sheet, **then** every nearby stop can be reached and opened without touching the map. `[manual: TalkBack, VoiceOver]`

### LB-204 "My stops" with live arrivals
Refs: FR-NEAR-03, FR-HOME-02, FR-HOME-07, NFR-PERF-02

- **Given** two saved stops and network, **when** the user taps Near Me right after a cold start, **then** ETAs for both stops are visible within 3 s at p75 *(spec, adapted)*.
- **Given** a saved stop with five upcoming arrivals across lines, **when** its row renders, **then** it shows the next 3, sorted by expected time.
- **Given** an arrival with `cancellation: true`, **when** "My stops" renders, **then** it is not counted among the 3 shown *(spec)*.
- **Given** no saved stops, **when** the sheet renders, **then** "My stops" reads "Save a stop to see its next buses here", with a "Find a stop" action that focuses the Near Me search.
- **Given** the arrivals request for a saved stop fails, **when** the sheet renders, **then** that stop keeps its previous arrivals with their freshness state, or shows an inline Retry if it has none (FR-ERR-03).

### LB-205 Stop detail
Refs: FR-NEAR-04, FR-NAV-07, FR-STOP-03 to 05, spec §5.7, DD-09, DD-16

- **Given** stop 38, served by lines 1 and 33, **when** stop detail opens, **then** API-06 is called with `lines=1,33&previewMinutes=60` *(spec)*.
- **Given** the user taps a stop pin in Near Me, **then** the pin enlarges, the sheet moves to medium and shows stop detail. **When** the stop is closed, **then** the sheet returns to the lists.
- **Given** arrivals in 30 s, 4 min and 75 min, **when** the list renders, **then** they show "Now", "4 min" and "HH:mm". An arrival more than 60 s in the past is removed.
- **Given** an arrival with `cancellation: true`, **when** it renders, **then** it is struck through with the label "Cancelled" *(spec)*.
- **Given** the last fetch was 50 s ago, **then** "Updated 50 s ago" is shown in the warning colour. **Given** more than 120 s, **then** ETAs switch to clock times with "Tap to refresh".
- **Given** stop detail is opened from line detail, **then** the same content and actions appear as a pushed screen.

### LB-206 Empty arrivals and night service
Refs: FR-STOP-06, OQ-08, NFR-REL-05

- **Given** API-06 returns no arrivals for a stop served by day lines, **when** stop detail renders, **then** it shows "No buses in the next 60 minutes".
- **Given** a stop served only by night lines (31 to 33), **when** the list is empty, **then** the message also shows `night_service_text` in the current language.
- **Given** remote config is unavailable, **when** the night-service text is needed, **then** the compiled default is shown.

### LB-207 Line filter and grouping
Refs: FR-STOP-04, FR-STOP-07

- **Given** a stop served by lines 1 and 10, **when** the line 10 chip is selected, **then** only line 10 arrivals are shown, and the chip is announced as selected.
- **Given** "Group by line" is turned on, **when** the list renders, **then** arrivals are grouped under line headers, each group sorted by expected time. The default is chronological.
- **Given** a filter that leaves no arrivals, **when** the list renders, **then** an empty message is shown with a "Clear filter" action.

### LB-208 Save a stop and edit saved stops
Refs: FR-STOP-08, FR-FAV-01, FR-FAV-02, FR-FAV-05

- **Given** stop detail, **when** the user taps Save and enters the alias "Casa", **then** the stop appears in "My stops" labelled "Casa".
- **Given** the alias field, **when** more than 30 characters are typed, **then** input stops at 30.
- **Given** Edit in "My stops", **when** the user reorders, renames or deletes, **then** the changes persist across restarts.
- **Given** 20 saved stops, **when** the user saves a 21st, **then** a limit message is shown and nothing is added.

### LB-209 Stop search
Refs: FR-NEAR-05, FR-STOP-01, FR-STOP-02, DD-16, NFR-PERF-03

- **Given** the query "Ayuntamiento", **when** results show, **then** the stops sharing that name are listed separately, each with its line badges and "towards <headsign>" subtitles.
- **Given** the query "38", **when** results show, **then** stop 38 is listed.
- **Given** any query, **when** it is typed, **then** local results appear within 100 ms.
- **Given** a result is selected, **when** search closes, **then** the map centres on the stop and the sheet shows its detail.
- **Given** a query with no matches, **when** results update, **then** an empty message is shown.

### LB-210 Directions to a stop and show on map
Refs: FR-STOP-09, FR-PLAN-11, FR-PLAN-14

- **Given** stop detail for stop 38, **when** the user taps "Directions to this stop", **then** How to Get There opens with stop 38 as the destination and the origin set as FR-PLAN-11 requires.
- **Given** stop detail pushed from Lines, **when** the user taps "Show on map", **then** Near Me opens with that stop selected.
- **Given** How to Get There already shows results, **when** "Directions to this stop" is used, **then** the destination is replaced and the old results are cleared.

### LB-211 Buses layer
Refs: FR-NEAR-07, FR-LIVE-01 to 03, DD-06

- **Given** the Buses layer is on and Near Me is visible, **then** API-03 is requested every 10 s. Polling stops when the layer is turned off, the tab changes or the app goes to the background.
- **Given** a vehicle on line 3, **when** it is drawn, **then** its marker uses the line 3 colour and shows "3".
- **Given** a vehicle with `lineRef "[object Object]"`, **when** it is drawn, **then** it is grey and has no code.
- **Given** the app is restarted, **when** Near Me opens, **then** the Buses layer is in its last state.
- **Given** `feature_live_map` is false, **when** the layer control renders, **then** the Buses option is hidden.

### LB-212 Routes layer
Refs: FR-NEAR-07, FR-LIVE-05, NFR-PERF-04

- **Given** the Routes layer is turned on, **when** the map renders, **then** every line shape is drawn in its line colour with a white casing.
- **Given** the layer is turned off, **when** the app restarts, **then** no shapes are drawn.
- **Given** all routes and all stops are shown, **when** the user pans, **then** the map holds 60 fps on the reference devices.

### LB-213 Live refresh cadence
Refs: FR-NEAR-09, FR-HOME-04, FR-HOME-06

- **Given** Near Me is visible, **when** time passes, **then** saved-stop arrivals refresh every 20 s.
- **Given** the app returns to the foreground on Near Me, **then** arrivals are fetched immediately.
- **Given** the user pulls to refresh or uses the refresh action, **then** all live data in the sheet is force-refreshed, and a refreshing indicator shows while the content stays interactive.
- **Given** another tab is visible, **when** time passes, **then** Near Me makes no live requests.

---

## E3 How to Get There

### LB-301 Origin and destination inputs
Refs: FR-PLAN-01, FR-PLAN-11

- **Given** location permission, **when** How to Get There opens, **then** the origin shows "My location" and the destination is empty.
- **Given** no location permission, **when** How to Get There opens, **then** both fields are empty.
- **Given** a stop, a saved place, an address or a point picked on the map with a long press, **when** it is chosen for either field, **then** the field shows its name, or the coordinates for a map point.
- **Given** a screen reader, **when** focus reaches either field, **then** its role and current value are announced. `[manual: TalkBack, VoiceOver]`

### LB-302 CartoCiudad geocoder and place search
Refs: FR-PLAN-02, FR-PLAN-12, `api_spec.md` §5

- **Given** the user types "ga", **then** no geocoder request is sent. **Given** they type "gan" and pause for 300 ms, **then** exactly one request is sent.
- **Given** fast typing, **when** the 300 ms debounce elapses, **then** only the last query is sent.
- **Given** results, **when** they render, **then** they are grouped as saved places, then stops, then addresses, with at most 8 addresses.
- **Given** a candidate without usable coordinates, **when** it is selected, **then** GEO-02 resolves it before use.
- **Given** the geocoder fails, **when** results render, **then** stops and saved places still appear, and the addresses group shows an inline error.
- Done when: `geocoder_candidates_logrono.json` and `geocoder_find.json` are captured, and the parameters are confirmed in `api_spec.md` §5.

### LB-303 Leave now / Depart at
Refs: FR-PLAN-03

- **Given** "Leave now", **when** a plan is requested, **then** `departureAt` is `AppClock.now()` in UTC.
- **Given** "Depart at" 08:19 on 22 Oct 2026 (CEST), **when** a plan is requested, **then** `departureAt` is `2026-10-22T06:19:00.000Z`.
- **Given** "Depart at" 08:19 on 15 Jan 2027 (CET), **when** a plan is requested, **then** `departureAt` is `2027-01-15T07:19:00.000Z`.

### LB-304 Swap origin and destination
Refs: FR-PLAN-08, FR-PLAN-14

- **Given** origin A and destination B, **when** swap is tapped once, **then** the origin is B, the destination is A, and the existing results are cleared.
- **Given** a screen reader, **when** focus reaches the swap button, **then** it announces "Swap origin and destination". `[manual: TalkBack, VoiceOver]`

### LB-305 Service-area check
Refs: FR-PLAN-10

- **Given** a destination 10 km outside the service bounding box, **when** a plan is requested, **then** "This location is outside the service area" appears inline and no API-07 request is sent.
- **Given** a point 4 km outside the box, **when** a plan is requested, **then** API-07 is called.

### LB-306 Plan request and itinerary cards
Refs: FR-PLAN-04, FR-PLAN-14, DD-08

- **Given** origin 42.4650, -2.4450 and destination 42.4560, -2.4300 at 08:19, **when** a plan is requested, **then** at least one itinerary with a bus leg on line 10 or 3 is shown *(spec)*. CI checks this against `route_ok.json`; the nightly Maestro run checks it against the live source.
- **Given** a plan request, **when** API-07 is called, **then** `maxResults` is 5, and the cards are sorted by arrival time.
- **Given** a card, **when** it renders, **then** it shows the departure and arrival times, the total duration, the line badges in leg order and the total walking distance.
- **Given** results are on screen, **when** the destination changes, **then** the old cards disappear before the new results arrive.
- **Given** the request fails with a network error, **when** the screen updates, **then** an error state with Retry is shown.

### LB-307 No-route messages
Refs: FR-PLAN-07, DD-17

- **Given** `routes: []` and `rejection.reason = "walk-too-far-destination"`, **when** the response arrives, **then** the message reads "The destination is more than 600 m from any stop." *(spec)*
- **Given** a non-empty `routes` together with a `rejection`, **when** the response arrives, **then** the routes are shown and the rejection is not.
- **Given** an unknown reason, **when** the response arrives, **then** "No route could be calculated." is shown.
- **Given** any rejection message, **when** it is shown, **then** a "Change time" action is offered.

### LB-308 Itinerary detail
Refs: FR-PLAN-05, FR-NAV-07, DD-18

- **Given** an itinerary with walk, bus and walk legs, **when** detail opens, **then** the map draws walking legs dashed and the bus leg in the line colour.
- **Given** a bus leg with 7 intermediate stops, **when** the step renders, **then** it reads "Ride 7 stops" and expands to list them.
- **Given** a `toStop.id` of 613, which is not in the stop catalogue, **when** the step renders, **then** the stop is resolved by name plus the nearest polyline point, or otherwise shown as plain text without a link.
- **Given** a boarding stop in the steps, **when** it is tapped, **then** stop detail is pushed.
- **Given** 200 % font, **when** the steps render, **then** they reflow without truncating times or line codes.

### LB-309 Real-time overlay on the first bus leg
Refs: FR-PLAN-06, DD-09

- **Given** a first bus leg departing in 20 min, **when** detail is visible, **then** the planned time is shown together with the live ETA from API-06 at the boarding stop, refreshed every 20 s.
- **Given** a first bus leg departing in 90 min, **when** detail opens, **then** no live overlay is requested.
- **Given** a low-confidence live ETA, **when** it renders, **then** it shows "~".

### LB-310 Recent searches
Refs: FR-PLAN-09, FR-PLAN-13

- **Given** 11 completed searches, **when** the planner root renders, **then** the latest 10 are listed, newest first.
- **Given** a recent search, **when** it is tapped, **then** origin and destination are filled and a plan is requested.
- **Given** a plan is in progress, **when** the screen renders, **then** recents are hidden in favour of the results.

### LB-311 Saved places
Refs: FR-FAV-01, FR-FAV-05, FR-PLAN-13

- **Given** the user saves an address as "Home", **when** the planner root renders, **then** a "Home" chip appears there and in place search.
- **Given** a saved-place chip, **when** it is tapped, **then** it fills the destination field.
- **Given** a saved place, **when** it is renamed or deleted, **then** the change persists across restarts.
- **Given** 20 saved places, **when** the user saves a 21st, **then** a limit message is shown and nothing is added.

---

## E4 MVP hardening

### LB-401 Network error handling
Refs: FR-ERR-03, FR-ERR-04, FR-ERR-05, ADR-0008

- **Given** a live request fails while its screen is visible, **when** retries run, **then** the delays are 2, 4 and 8 s, up to 30 s, and the previous data stays visible with its freshness state.
- **Given** a data source fails with no previous data, **when** the screen renders, **then** an error state shows the reason (network, server or data) and Retry.
- **Given** connectivity has returned, **when** Retry is tapped, **then** the screen loads normally.
- **Given** a background static refresh fails, **then** no error is shown, and the previous cache stays in use.

### LB-402 Standard UX states
Refs: spec §7.3, spec §10.6

- **Given** any data-driven MVP screen, **when** screenshot tests run, **then** they cover loading, fresh, stale, expired, refreshing, empty and error (the offline state is v1.0), in light, dark and 200 % font, on both platforms.
- **Given** a first load, **when** data is pending, **then** skeletons shaped like the final layout are shown, and no lone spinner lasts more than 300 ms.
- **Given** a refresh while content is shown, **when** it runs, **then** a thin progress indicator appears and the content stays interactive.
- **Given** items dropped during validation, **when** the screen renders, **then** the remaining content is shown with no error message, and the drop is counted in the `DataQualityReport`.

### LB-403 Settings
Refs: FR-SET-01, FR-NAV-04

- **Given** More → Settings, **when** it opens, **then** it offers language (System, Español, English), theme (System, Light, Dark), ETA format, data refresh status and Clear cache.
- **Given** English is selected, **when** the UI updates, **then** every UI string is in English without a restart, and line and stop names are unchanged.
- **Given** the ETA format is set to clock time, **when** arrivals render, **then** every ETA shows as HH:mm.
- **Given** Clear cache is used, **when** it completes, **then** live and image caches are emptied, while favourites, places, recents and static data are kept.

### LB-404 About data
Refs: FR-SET-03, NFR-PRIV-05

- **Given** More → About data, **when** it opens, **then** it shows the data source, the last successful sync time of each dataset and the disclaimer that predictions are provided by the operator.
- **Given** the latest sync failed, **when** the page renders, **then** it still shows the time of the last *successful* sync.
- **Given** the attributions section, **when** it renders, **then** it lists "Data: Ayuntamiento de Logroño", OpenStreetMap and CartoCiudad / IGN.

### LB-405 Localisation
Refs: NFR-I18N-01 to 03

- **Given** the device language is Spanish or English, **when** the app runs, **then** every UI string is in that language. Any other device language falls back to Spanish.
- **Given** the device time zone is America/New_York, **when** times render, **then** they are in Europe/Madrid.
- **Given** a count of 1 and a count of 2 minutes, **when** they render in either language, **then** the plural forms come from platform resources.
- Done when: a lint or test check fails on any hard-coded user-facing string in UI code.

### LB-406 Accessibility pass
Refs: NFR-A11Y-01 to 06, NFR-PLAT-01

- **Given** an arrival of line 2 in 4 minutes, **when** a screen reader focuses it, **then** it reads "Line 2, in 4 minutes". `[manual: TalkBack, VoiceOver]`
- **Given** live updates while focus is on an arrival row, **when** data refreshes, **then** focus does not move and at most one polite announcement is made every 30 s.
- **Given** 200 % font, **when** every MVP screen is captured, **then** no ETA or line code is truncated.
- **Given** every interactive element, **when** checked with Accessibility Scanner and Accessibility Inspector, **then** no touch target is smaller than 48 dp or 44 pt.
- **Given** `[iOS]` Reduce Transparency and Increase Contrast, **when** custom glass surfaces render on iOS 26 or later, **then** they stay legible. `[manual: VoiceOver]`

### LB-407 Favourites persistence
Refs: FR-FAV-03, FR-FAV-04

- **Given** favourites, places and recents exist, **when** the app updates to a new version, **then** all of them are preserved.
- **Given** a backup restore onto a new device (`[Android]` Auto Backup, `[iOS]` iCloud or device backup), **when** the app opens, **then** favourites and places are restored.
- **Given** a saved stop that has been missing from API-02 for 7 consecutive daily syncs, **when** "My stops" renders, **then** it shows "No longer served" and stays until the user deletes it.
- **Given** that stop reappears in a later sync, **when** "My stops" renders, **then** the "No longer served" label is gone.

### LB-408 Server clock offset
Refs: FR-ERR-06, spec §8.6

- **Given** the device clock is 3 min ahead of the server `Date` header, **when** an arrival expected in 5 min of server time renders, **then** it shows "5 min", not "2 min".
- **Given** no `Date` sample yet, **when** ETAs are computed, **then** the device clock is used, and the first sample corrects them on the next render.

### LB-409 Consent, analytics and crash reporting
Refs: NFR-PRIV-03, NFR-OBS-03, OQ-16, ADR-0009. Blocked until the vendor is chosen; the contracts are LB-017.

- **Given** first launch, **when** the app starts, **then** a consent dialog appears before any analytics or crash data is collected, and declining sends nothing.
- **Given** Settings, **when** it opens, **then** analytics and crash reporting have separate toggles, and changes take effect immediately.
- **Given** consent was given, **when** events are inspected in the debug view, **then** only the NFR-OBS-03 events are sent, with no free text or coordinates.

### LB-410 Onboarding
Refs: FR-NAV-06, SC-18, FR-LOC-01

- **Given** the first launch, **when** the app opens, **then** three cards (value, favourites, location rationale) appear before the Lines tab.
- **Given** any onboarding card, **when** Skip is tapped, **then** Lines opens, and onboarding never shows again.
- **Given** the location rationale card, **when** it is shown, **then** no system permission prompt is triggered.
- **Given** the second launch, **when** the app opens, **then** it goes straight to Lines.

### LB-411 Performance budgets
Refs: NFR-PERF-01 to 06, NFR-PERF-02 (adapted)

Done when:
- Cold start to first meaningful paint is under 1.5 s at p75 on a Pixel 6a and an iPhone 12 (Macrobenchmark, XCTest metrics).
- Cold start plus a Near Me tap to the first ETA is under 3 s at p75, on a network conditioned to 4G.
- Local stop search takes under 100 ms for any query (benchmark).
- The map holds 60 fps with all stops and 60 vehicles on a mid-range device.
- Download size is under 25 MB on Android (per-device AAB) and under 40 MB on iOS (App Store size report).
- A 10-minute session with the live map open uses under 2 MB of data.

### LB-412 Maestro end-to-end flows
Refs: spec §10.1, `architecture.md` §8

Done when:
- Five Maestro flows cover the journeys in `architecture.md` §8:
  - saved-stop ETAs after a cold start;
  - the "Ayuntamiento" search;
  - line 10, switching direction and tapping a live bus;
  - a plan from the current location to an address;
  - losing and restoring connectivity on stop detail (network error, then Retry).
- The flows run nightly on both platforms against the live source, and report results without blocking merges.
- The flows locate elements by stable accessibility identifiers, never by screen coordinates.
- `nightly.yml` runs the flows. It is created here, unless an Android instrumented test needed it first (moved from LB-002).


### LB-413 Apply the Figma designs
Refs: OQ-18, NFR-MNT-04

- **Given** the Figma file for the MVP, **when** it is applied, **then** only the token files, the design-system components and the app icons change; no feature code changes colours or typography directly.
- **Given** the new theme, **when** the screenshot suites run, **then** every reference image is re-recorded and reviewed, and every `LineBadge` still reaches 4.5:1 contrast.
- **Given** the Figma file, **then** `design_guidelines.md` links to it as the visual source of truth.

### LB-414 Privacy labels and disclosures
Refs: NFR-PRIV-01 to 05, `security_privacy.md` §5–6

Done when:
- The privacy policy is published in es and en at a stable https URL, and linked from About data and the store listings.
- The Google Data safety form and the Apple privacy nutrition label match the actual data flows, including the telemetry vendor if one is active.
- The checklist in `security_privacy.md` has been reviewed against the final MVP build, and the owner has approved the answers.

### LB-415 Repository public, `main` protected
Refs: `ci_cd.md` §1, OQ-20, OQ-21

Done when:
- The pre-public checklist in `ci_cd.md` §1.1 is complete, and the licence is decided.
- The repository is public, and `scripts/github/protect-main.sh` has been applied. A test direct push to `main` is rejected by GitHub.
- The policy workflow's jobs run (they are no longer skipped) on a test pull request, and they are the required checks.

---

## Traceability gaps

Status after the baseline review of 2026-10-22:

| Gap | Resolution |
| --- | --- |
| FR-FAV-05 for stops and places | Resolved: LB-208 and LB-311 now cite FR-FAV-05 in `backlog.md` |
| NFR-MNT-05 had no owning story | Resolved: LB-016 (multi-agent harness and local quality gates) |
| NFR-PLAT-03, iPad decision | Resolved: iPhone only for the MVP (OQ-19) |
| FR-NAV-02 deep-link exception | Accepted: verified when LB-603 lands (v1.0), as noted in `requirements.md` |
| FR-SET-01 notification toggles | Accepted: excluded from the MVP (`requirements.md` §3.6) |
| NFR-REL-01 crash-free rate | Accepted: store consoles until OQ-16 (`requirements.md` §3.6) |
