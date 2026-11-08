# Acceptance criteria

| Field | Value |
| --- | --- |
| Status | Baseline 1.0 for the MVP (M0–M4) |
| Last updated | 2026-11-08 |
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

Done (2026-11-08). Evidence:
- **Recorder and endpoint coverage:** `scripts/record-fixtures.sh` is the only place that may call the live source. `--list` prints its 19 endpoints: the 16 fixtures of spec §10.2, both API-07 routes included, `sections.json` (API-12's fifth path, which the spec's list omits) and the two CartoCiudad captures. `FixtureRecorderScriptTest.lb003_theRecorderCoversEveryFixtureOfTheSpec` (`tools/architecture-tests`) compares the list with the spec's names, offline. A body above 16 MiB fails its endpoint with a message naming it and leaves `fixtures/` alone, capped with `--max-filesize` on the wire and checked again after decompression (`lb003_anOversizedBodyFailsTheEndpoint`).
- **Pacing and `User-Agent`:** one request per endpoint, at least a second apart, with the project `User-Agent` (`lb003_requestsAreAtLeastOneSecondApart`, `lb003_aCaptureSendsTheProjectUserAgent`, same file, against a local stub).
- **Canonical output:** `jq --sort-keys --indent 2` (`lb003_aCaptureIsPrettyPrintedWithSortedKeys`). A second `--check` run straight after the capture listed all 19 as `unchanged`, although the values and the sizes had moved: the shape is what is compared.
- **The defect as it arrives:** `fixtures/vm_all_with_object_lineRef.json` carries the source's own `"[object Object]"` value (DD-06) in 40 of its 57 activities, at `result.activities[].lineRef` and, with it, `publishedLineName`, so no hand-made copy prefixed `synthetic_` was needed.
- **The weekly job:** `.github/workflows/weekly-fixtures.yml`, Mondays 04:17 UTC and on demand, documented in [`ci_cd.md`](ci_cd.md) §5.1. `Fixtures` runs `--check`, and drift exits 1 through `scripts/ci/fixture-shape.sh`, which collapses an array to `[]`, reads an integer and a float as `number`, reads `null` and `unknown` as open types, so neither they nor anything beneath them is drift, reports the value when a path holds both `null` and a value, and reads an array's element path as `unknown` when it holds nothing, so a collection that became empty or was empty and now holds items is not drift in either direction (`lb003_aShapeDiffReportsAnAddedField`, `lb003_aShapeDiffReportsARemovedField`, `lb003_aShapeDiffReportsATypeChange`, `lb003_anIdenticalShapeReportsNoDiff`, `lb003_aNullAndAValueAreTheSameShape`, `lb003_anIntegerAndAFloatAreTheSameShape`, `lb003_anEmptyCollectionIsTheSameShapeAsAPopulatedOne`, `lb003_anAddedFieldOnANonEmptyCollectionIsStillDrift`, `lb003_checkModeFailsOnDriftAndPassesOnAnUnchangedShape`). On drift, `Contract drift` opens one issue per ISO week. The workflow is in no gate, so drift never blocks a merge.
- **No test reaches the source:** a JDK `InetAddressResolverProvider` in `:shared:core:testing`, on the test classpath of every module that uses the Android or the KMP library convention, the two test-support modules excepted, resolves loopback only and fails any other host name with `NETWORK GUARD`. `TestNetworkGuardTest` (3 cases: the service loader finds the guard, a real host fails with the marker, loopback still resolves) and `NetworkGuardProbeTest` in `:shared:core:network` (2 cases: the source's host is unresolvable, loopback still resolves) prove it. The guard covers JVM tests only: Kotlin/Native tests have no JVM resolver and stay on `MockEngine`. It stops host names only, since a numeric IP literal never reaches a resolver.
- **Robolectric follows from it.** Robolectric used to download its Android framework jar from the test JVM, which the guard would refuse, so the build resolves the jars through Gradle and hands them over with `robolectric.offline=true` and `robolectric.dependency.dir`, a miss being a failure rather than a request. A test never downloads that jar any more.
- **Owner decision (2026-11-08):** the 19 captures are committed with OQ-11 still open, and merging them is the owner's legal call.
- The workflow has not run on CI yet: this evidence is local, and nothing here claims a workflow result.

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

Part 1 (2026-10-27: `:shared:core:network`, before the DTOs), also done when:
- The engines' own timeouts follow the request timeout: OkHttp reads and writes, and NSURLSession's idle timeout, last 10 s for live requests and 30 s for static ones. NSURLSession has no separate connect timeout, so on iOS connecting is bounded by the request timeout.
- A transport failure, a timeout or any other failure to send, such as a malformed `api_base_url`, maps to `Network` for that call only. A call whose caller is cancelled is cancelled, not failed.
- A body that is only a word or a JSON scalar maps to `Data`: every source response is an object or an array.
- The `Date` header of every response, error statuses included, corrects `AppClock` (spec §8.6). A cached response counts from `Date` plus `Age`. A missing or unreadable `Date` or `Age`, or a time before 2026, leaves the clock alone.
- `[iOS]` NSURLSession keeps no response cache and no cookies; `[Android]` OkHttp has neither by default.
- A failed call is logged by method, path and error type; a dropped item by DTO name, index and error type. Neither log line contains a body or an error message.

Part 2, before the fixtures (2026-10-27), also done when:
- A GET sends its query parameters percent-encoded, except the commas in values, so a list goes out as the source was captured: `?lines=1,33&previewMinutes=60` (API-06). The failure log names the path without the query, and a path that carries its own query is rejected.
- The cleartext criterion above is tested: `[Android]` the manifest names a network security config whose only element, `base-config`, denies cleartext; `[iOS]` the built app's Info.plist has no `NSAppTransportSecurity` key.

### LB-006 Normaliser and mappers
Refs: spec §5.3–5.6, DD-01 to DD-11, DD-15 to DD-18, NFR-MNT-01

Done when:
- Every listed DD rule has at least one passing test named with its prefix (`dd01_` … `dd18_`).
- The `dd05_` tests cover CET (UTC+1), CEST (UTC+2) and the last Sundays of March and October. A VM time within 30 min of `locationRecordedAtTime` is left uncorrected (risk R4).
- `4-PALACIO DE CONGRESOS-PRADOVIEJOO`, with the override `PRADOVIEJOO→PRADOVIEJO`, yields the display name "Palacio de Congresos - Pradoviejo" *(spec)*. `B3-LARDERO-EL CAMPILLO` yields kind `NIGHT`.
- Codes 401 and 402 in `et_line1_60min.json` resolve to opposite directions through the algorithm of spec §5.6 and are stored in `direction_code_cache`. An ambiguous code yields `direction = null`.
- Stops 383, 384 and 385 exist as synthetic provisional stops. Items outside the service bounding box expanded by 2 km are dropped and counted in the `DataQualityReport`.
- Line coverage of `data` and `domain` is at least 80 %.

Part 1 (2026-10-31), the rules that need no DTO shapes, done with this evidence (on the Android host and the iOS simulator):
- DD-01, DD-02, DD-06 (`IdParserTest`): `dd01_aStopIdInEverySourceFormatIsTheSameNumber`, `dd01_aStopIdThatIsNotAWholeNumberIsNull`, `dd01_onlyTheAgencyPrefixOneIsStripped`, `dd02_aLineIdInEverySourceFormatIsTheSameNumber`, `dd06_theLiteralObjectObjectLineRefIsNull`.
- DD-04: `DirectionMapperTest` (`dd04_ascAndIdaAreOutbound`, `dd04_descAndVueltaAreInbound`, …) and `EtDirectionInferrerTest` (`dd04_eightyPercentForOnePatternAndUnderFiftyForTheOtherDecides`, `dd04_anOrderOneAboveOrBelowItsPositionStillFits`, `dd04_oneStopAtDifferentPlacesOnThePatternsIsDecidedByItsOrderButNotCached`, …). Codes 401 and 402 of `et_line1_60min.json`, and the cache, are part 2.
- DD-05 (`NextStopTimeTest`): `dd05_aMislabelledTimeIsReadAsMadridTime_inCet`, `…_inCest`, `dd05_aTimeWithinThirtyMinutesOfTheReportIsLeftAlone`, `dd05_onTheLastSundayOfMarchTimesOnEitherSideOfTheJumpAreCorrected`, `dd05_aTimeInTheHourSkippedInMarchIsReadNearestTheReport`, `dd05_onTheLastSundayOfOctoberATimeAcrossTheChangeIsCorrected`, `dd05_onTheLastSundayOfOctoberTheRepeatedHourIsReadNearestTheReport`.
- DD-07 (`SourceStringsTest`), DD-08 (`CssColorTest`, with `ContrastTest` from LB-011), DD-11 (`NameNormaliserTest`).
- The display name and the kind (`LineNameParserTest`): `lb006_pradoviejooShowsAsPalacioDeCongresosPradoviejo`, `lb006_aCodeStartingWithBIsANightLine`.
- The bounding box (`ServiceAreaTest`): `lb006_aPointMoreThanTwoKilometresBeyondTheStopsIsNot`. Dropping and counting items is part 2.
- DD-15 was done in LB-005 (`SourceClientTest.dd15_anyTwoHundredStatusIsASuccessAndPostsAnswer201`).

Part 2 is left: the mappers over LB-005's DTOs (lines, stops, VM, ET, planner) and the `StaticNormaliser` for LB-008; DD-03, DD-09, DD-10's synthetic stops 383 to 385, DD-16 to DD-18; the persisted `direction_code_cache` with codes 401 and 402; the `DataQualityReport`; all checked against LB-003's fixtures.

### LB-007 `:shared:core:database`
Refs: spec §8.8, `architecture.md` §4.3, FR-STOP-01, NFR-I18N-03, NFR-PERF-03

Done when:
- The schema contains spec §8.8 plus `stop.normalized_name`, `favourite.missing_syncs` and `plan_recent` (`architecture.md` §4.3). `arrival_snapshot` waits for offline mode in v1.0 (ADR-0008).
- Search matches regardless of accents, `ñ` and case: "logrono" matches "Logroño", "AYUNTAMIENTO" matches "Ayuntamiento", and the query "38" matches stop 38 by id.
- A search over the full 254-stop dataset returns in under 100 ms on the reference devices (benchmark). LB-007 measures it on the JVM and the iOS simulator; the reference devices are measured with the search screen (LB-209, LB-411).
- Migrations are versioned. A migration test upgrades from the previous schema without losing rows in `favourite`, `place` or `plan_recent`. Until the first release the schema is version 1 and has no previous schema, so this applies from the first schema change after it (ADR-0013).

Added from the reviews (2026-10-27), also done when:
- The search index follows every write to `stop`: insert, update, delete and `INSERT OR REPLACE`.
- The index and the query split names into the same words: a typographic apostrophe, a non-breaking space, a dash or a decomposed accent (NFD) never hides a match.
- Key columns are `NOT NULL`, a favourite line or stop can be saved once only, and a stop's lines and patterns have lookup indexes.
- Instant columns hold UTC epoch milliseconds and read as `kotlin.time.Instant` (AGENTS.md §5.5).

### LB-008 `StaticSyncEngine`
Refs: FR-ERR-04, FR-ERR-05, NFR-REL-03, spec §8.5, FR-FAV-04, ADR-0008

Done when:
- On first use with an empty cache and network available, lines, stops and shapes are downloaded, normalised and stored in one transaction. With no network, the requesting screen gets a `Network` error, and a later Retry succeeds once connectivity returns.
- No seed data is bundled or imported in the MVP (ADR-0008).
- A payload with fewer than 5 lines or 100 stops is rejected, and the previous cache is kept intact.
- An unchanged payload (same SHA-256) is neither normalised nor written: only `sync_state.last_success` and `favourite.missing_syncs` change. Amended in LB-008 part 1: FR-FAV-04 counts daily syncs, so a source that stops changing must still count a missing stop.
- An exception injected mid-write rolls back the transaction; concurrent readers never see a partial dataset.
- A failed background refresh shows no UI error and is retried at the next trigger. The TTL for lines and stops is 24 h.
- `favourite.missing_syncs` increases with every sync in which a saved stop is absent, and resets when the stop reappears.

Part 1 (2026-10-30), the engine with the source and the normaliser injected, done with this evidence (`StaticSyncEngineTest`, on the Android host and the iOS simulator, unless named otherwise):
- First use and Retry: `lb008_theFirstSyncStoresTheLinesPatternsShapesAndStopsItDownloads`, `lb008_storedStopsCanBeSearchedByName`, `lb008_withNoNetworkTheFirstSyncFailsWithNetworkAndStoresNothing`, `lb008_aRetrySucceedsOnceTheNetworkIsBack`.
- No seed: `lb008_aNewDatabaseHasNoLinesOrStops`, and `ArchitectureTest.lb008_noModuleBundlesSeedData` (mutation-checked) with its predicate's tests.
- Validation: `lb008_aPayloadWithFewerThanFiveLinesIsRejectedAndTheCacheKept`, `…OneHundredStops…`, `dd10_provisionalStopsDoNotCountTowardsTheHundred`, `lb008_aRejectedPayloadIsLoggedWithItsCounts`.
- Unchanged payload: `lb008_anUnchangedPayloadWritesNothingButTheTimeOfTheSync`, `lb008_whenOneDatasetChangesBothAreStored`; the hash, `ContentHashTest`.
- Atomic replace: `lb008_aWriteThatFailsHalfwayRollsBackAndKeepsTheCache`, `lb008_observersHearOfTheNewCatalogueOnceItIsWhole`, `lb008_whatLeavesThePayloadLeavesTheCacheAndTheSearch`. "Concurrent readers never see a partial dataset" rests on the one transaction those tests prove; part 2 adds no reader of its own.
- TTL and retries: `lb008_withinTwentyFourHoursOfTheLastSuccessNothingIsDownloaded`, `lb008_twentyFourHoursAfterTheLastSuccessTheSyncDownloadsAgain`, `lb008_aDatasetThatNeverSyncedMakesTheCacheStale`, `lb008_aFailedRefreshIsRetriedAtTheNextTrigger`.
- Missing syncs: `lb008_aSavedStopMissingFromASyncCountsOneMoreMissingSync`, `lb008_aSavedStopThatReappearsStartsAgainFromZero`, `lb008_anUnchangedPayloadStillCountsMissingStops`, `lb008_aFailedSyncCountsNoMissingSync`, `lb008_onlySavedStopsAreCounted`.
- Added from the part 1 reviews:
  - A changed normaliser fingerprint (a new normaliser, new DD-11 overrides) renormalises an unchanged payload (`lb008_aChangedNormaliserRenormalisesAnUnchangedPayload`).
  - A last success in the future makes the cache stale (`lb008_aLastSuccessInTheFutureIsStale`).
  - A saved stop that is only provisional (DD-10) counts as served (`dd10_aSavedStopThatIsOnlyProvisionalIsStillServed`), and a pattern stop with no stop rejects the catalogue (`dd10_aPatternStopWithNoStopIsRejectedAndTheCacheKept`).
  - A database that cannot be read, or a normaliser that throws, fails with `Data` and keeps the cache; two syncs at once share one download (`lb008_aDatabaseThatCannotBeReadFailsWithData`, `lb008_aNormaliserThatThrowsFailsWithDataAndKeepsTheCache`, `lb008_twoSyncsAtOnceShareOneDownload`).
  - A rejected or rolled-back sync counts no missing stop; the stored keys of directions and line kinds never change (`CatalogueKeysTest`).

Part 2 is left: the real source on `SourceClient` (API-01, API-02) and LB-006's normaliser with its fingerprint; the Koin `single`; the app-start trigger and the daily background job; and, at screen level, the `Network` error with Retry and the silent background refresh.

### LB-009 `PollingEngine`
Refs: spec §8.6, FR-ERR-03, NFR-PERF-07

Done when:
- Two collectors of the same `PollKey` produce one upstream request per interval.
- Fetch times fall within the interval ±1 s of jitter (virtual-time test).
- After repeated failures the retry delays are 2, 4, 8, 16 and then 30 s. The last good value stays available, with `lastError` set.
- Moving `AppLifecycle` to the background stops all requests; returning to the foreground triggers an immediate fetch, unless the last attempt is under 10 s old (`MIN_POLL_INTERVAL`, still fresh by spec §5.7).
- No request is made while no collector is subscribed.
- Added from the LB-009 reviews:
  - The jittered wait never drops below the 10 s floor, and waits at the floor still spread over 10 to 11 s.
  - A collector that leaves and comes back waits for the key's next scheduled fetch, and the backoff survives it.
  - A lone success between failures waits the interval without restarting the backoff; two successes in a row restart it.
  - A `fetch` that throws fails only its own key (`Data`; a timeout inside it gives `Network`) and is retried; the other keys keep polling.
  - A key without collectors for 120 s forgets its value, so a returning screen shows Loading rather than an expired value.

### LB-010 DI and platform presentation conventions
Refs: ADR-0004, ADR-0007, `architecture.md` §5

Done when:
- `initKoin()` runs exactly once at startup on each platform, and a Koin graph verification test passes.
- `[Android]` Each screen's `XViewModel` lives in its `:android:feature:<x>` module, is obtained with `koinViewModel()`, survives configuration changes, and builds state only with the shared `UiState` types and mappers.
- `[iOS]` Each Swift `XViewModel` (`@MainActor @Observable`) gets use cases only from `SharedDependencies`, collects them with `for await` inside `.task {}`, and builds state with the shared mappers. Views switch over the state exhaustively with `onEnum(of:)`.
- `[iOS]` Leaving a screen cancels its observation task, and a test asserts that the upstream collection stops.
- `SharedDependencies.preview` provides scripted fakes, used by SwiftUI previews and Swift ViewModel tests.
- The Kotlin and SKIE versions in the catalog are a pair documented as compatible.

Part 1 (2026-10-27, the graph of the shared core), also done when:
- The graph resolves every core service on the Android host and the iOS simulator, with one `ServerCorrectedClock` serving as the `AppClock`, and a second definition of any type fails at start.
- `[Android]` `LogroBusApplication` starts the graph at launch; `[iOS]` `LogroBusApp.init` does, and `SharedDependencies.isReady` says so.
- The app shells pass their version, for the `User-Agent`, and their build type: release builds log errors only, whatever the shared binary's build.
- Stopping the graph cancels its application scope and closes the database.
- SKIE exports `TopLevelDestination` as a Swift enum, so the tab labels switch over it without a default.

Part 2 (2026-10-28, the conventions on About data's last syncs), also done when:
- The first screen with a use case is the last-sync section of About data (LB-404, FR-SET-03), because LB-101 waits for LB-003's fixtures (OQ-11). It reads `sync_state` and needs no source data.
- What the screen shows is LB-404's criteria and LB-405's time-zone criterion, met early and tested under their ids (`lb404_…`, `lb405_…`).
- A failure to read is a typed `AppResult` failure, never an exception, and both screens show FR-ERR-05's Error state with its reason and Retry. Retry reads again.
- The first load draws the final layout with placeholders, not a spinner (spec §7.3).
- `[Android]` The ViewModel's upstream survives 5 s without collectors, which covers a configuration change, then stops. Each `ViewModelStoreOwner` gets its own ViewModel.
- `[Android]` The app's graph gives each screen its ViewModel, through `initKoin(uiModules)`.
- The graph gives Swift every function of `UseCases`, checked by reflection on the Android host, so a new use case needs no new test line.
- `SharedDependencies.shared.preview()` scripts synced, never-synced, loading and failing states, and counts the collections running. Before the graph starts, asking `SharedDependencies` for a use case fails with a message that points at `preview()`.
- `[iOS]` The ViewModel's init only stores its use cases, so SwiftUI may rebuild it cheaply.
- `[iOS]` The iosApp scheme runs the package's Swift ViewModel tests, so `scripts/ios/test.sh` does too, until LB-101's matrix job.

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

Part 1 (2026-10-28, shared and Android), also done when:
- `onColorFor` and `contrastRatio` (`:shared:core:common`) follow WCAG 2.2, and give each of spec §3's 13 line colours an `onColor` of at least 4.5:1. The data layer uses the same rule (DD-08, LB-006).
- `EtaUi` (`:shared:core:presentation`) carries every case of spec §5.7 as meaning, not words.
- `[Android]` Every text colour of the placeholder schemes reaches 4.5:1 against its container, and every surface is grey.
- `[Android]` `EtaText` shows and speaks every case, in Spanish and English, with plurals and "a la 01:05". The spoken label says what the screen shows, for every combination `EtaUi` allows. "Now" pulses, stays at 4.5:1 at its faintest, and holds still when animations are off.
- `[Android]` The warning colour of stale data is a token (`StatusColors`) that reaches 4.5:1 on every surface.
- `[Android]` The window theme follows dark mode, so the app never starts on a white window in dark mode.
- `[Android]` An architecture test keeps `ExperimentalMaterial3ExpressiveApi` inside the design system's package. Another requires the screenshot job to list every module with reference images.
- `[Android]` The app and About data draw on `LogroBusTheme`, in light and dark. About data's four states have screenshots.
- The expressive wrappers (`LbLoadingIndicator`, `LbButtonGroup`) are built with the first screen that needs each; no screen does yet.

Part 2 (2026-10-29, iOS), also done when:
- `[iOS]` `Tokens` are Android's palette, as ARGB values. Every text colour reaches 4.5:1, measured with the shared `contrastRatio`, and every surface is grey.
- `[iOS]` `LineBadge` says "Línea 3" / "Line 3". `EtaText` shows and speaks Android's 13 cases in Spanish and English, with "a la 01:05". "Now" stays at 4.5:1 at its faintest and holds still with Reduce Motion.
- `[iOS]` The components' words follow SwiftUI's environment locale.
- `[iOS]` `adaptiveGlass` and the glass button styles are snapshot as Liquid Glass and as the fallback. Each is captured in light, dark, Increase Contrast and 200 % text, and with the primary action disabled.
  - The fallback is forced on the iOS 26 simulator through `glassRendering`; real iOS 18 runs come with the nightly (LB-412).
  - Reduce Transparency, which no trait sets, is checked by hand.
- `[iOS]` The glass buttons are at least 44 × 44 pt. A line badge always holds its code, at 200 % too. An ETA in a narrow row overflows rather than truncates.
- `[iOS]` The snapshot tests run in the iOS build-and-tests job, through the iosApp scheme, rather than a job of their own, pending the owner's decision on the iOS CI budget. Since LB-101 (owner decision, 2026-11-02), `DesignSystemTests` runs in the iOS ViewModel and snapshot job of its own; the app-hosted `GlassSnapshotTests` stay in the app's job.

### LB-012 App shell
Refs: FR-NAV-01 to 05, FR-NAV-07, ADR-0002, ADR-0005

- **Given** a cold start without a deep link, **when** the app opens, **then** the Lines tab is selected and the tabs show the labels of `design_guidelines.md` §2.1, in this order: Líneas, Cerca de mí, Cómo llegar (es) and Lines, Near me, Directions (en).
- **Given** the user is two screens deep in Lines, **when** they switch to Near Me and back, **then** Lines shows the same screen. **When** they re-select Lines, **then** it pops to its root, and re-selecting it at the root scrolls to the top.
- **Given** any tab root, **when** the user taps the More action, **then** More opens and lists Settings and About data.
- **Given** `[Android]` a window of medium width or wider, **when** the shell renders, **then** a navigation rail replaces the bottom bar.
- **Given** TalkBack or VoiceOver, **when** focus reaches the tab bar, **then** each tab announces its label and selected state. `[manual: TalkBack, VoiceOver]`
- **Given** a deep link that targets another tab (LB-603, v1.0), **when** the app is launched from it, **then** that tab opens instead of Lines. This criterion is verified when LB-603 lands.

Done (2026-10-30), with tests `lb012_…`:
- Both platforms open on Lines, with the three tabs in order and the labels in Spanish and English (`AppShellTest`, `ShellUITests`, `LaunchTests`).
  - Each tab root's title is its label, except How to Get There, whose screen is titled "How to get there" (`plan.title`).
- "Two screens deep in Lines" is Lines, More and About data. Lines keeps that screen while another tab shows, and re-selecting Lines pops it to its root (`TabNavigationTest`, `ShellUITests`).
  - On iOS the system pops a re-selected tab by itself, after the router, so the UI test shows the behaviour rather than the router's part.
- Re-selecting a tab at its root asks the root to scroll to its top. The parts are tested: the requests (`TabNavigatorTest`, `TabRouterTests`) and the Android scrolling helper (`ScrollToTopOnReselectTest`).
  - On Android, a root with a list calls `ScrollToTopOnReselect`; on iOS, it watches the `scrollToTopRequests` environment value.
  - The placeholder roots do not scroll, so nothing tests the whole path yet. The first scrolling roots verify it end to end: see LB-101's criteria.
- Every tab root's More action opens More, which lists Settings and About data. Settings is a titled placeholder until LB-403.
- `[Android]` A window of medium width or wider shows a rail, whatever its height, and a compact one a bar (`AppShellTest`).
  - At 200 % font on a 320 dp phone, every bar label fits its tab. Rail labels keep their full size and wrap (`TabLabelTest`).
  - Each tab has the tab role and its selected state, the automated half of the TalkBack criterion. Screen titles are headings.
- `[Android]` Back walks back through the selected tab's stack, as does a screen's up button. At the root of Near Me or How to Get There, back returns to Lines; at the root of Lines, it leaves the app. The stacks survive the activity's recreation.
  - Screens change on the expressive motion scheme's effects spring.
  - A screen on two tabs keeps a separate state and ViewModel on each.
- Screenshots and snapshots, in light, dark and 200 % (Spanish):
  - on Android, the shell as a bar and as a rail, More and Settings;
  - on iOS, More and Settings.
- Not done here:
  - **Owner, on a device:** the TalkBack and VoiceOver passes.
  - **LB-603:** the deep-link criterion.
- No onboarding placeholder: FR-NAV-06 belongs to LB-410, and the shell opens straight on Lines. FR-NAV-07 (one stop detail screen) is LB-205's.

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

Done (2026-10-30), with tests `lb017_…`:
- `AnalyticsEvent` has the six MVP events of NFR-OBS-03: `AppOpen`, `StopView`, `LineView`, `PlanRequest`, `PlanResult` and `FavouriteAdd`. `widget_add` and `notice_open` come with their features in v1.0.
  - The Konsist rule allows only `Int`, `Long`, `Boolean` and telemetry enums in every type under `AnalyticsEvent` or `Breadcrumb`, at any depth and in the constructor or the body, the roots and intermediate interfaces included, so free text and coordinates fail whatever their type (`ArchitectureTest`, `ArchitectureRulesTest`). Mutation-checked with a `String` and a `Double` latitude, a free-text event under an intermediate sealed interface, and two types sharing a simple name.
  - A second rule pins the catalogue to the MVP events, at any depth.
  - `[iOS]` Swift cannot add events or breadcrumbs: the SwiftLint rule `swift_telemetry_payload` rejects any Swift type that adopts either protocol.
- The gates forward nothing before the user decides, after a refusal or a withdrawal, or on consent to the other kind of telemetry, and nothing dropped is sent later (`ConsentGatedAnalyticsTrackerTest`, `ConsentGatedCrashReporterTest`, on the Android host and the iOS simulator).
  - The crash gate forwards a `RedactedThrowable`: each error's type and stack frames, none of its messages (`ConsentGatedCrashReporterTest`); on Android, with the original stack trace (`RedactedStackTest`).
  - A vendor that throws never breaks the caller (`FailingTelemetryTest`).
- With a logger that prints every level, a release graph prints no telemetry and a debug graph prints it, only after consent (`TelemetryGraphTest`). The logging reporter prints an error's types, never its message (`LoggingTelemetryTest`).
- No module but `:android:app` may declare a vendor analytics or crash-reporting SDK, native or a Kotlin Multiplatform wrapper such as GitLive's Firebase or CrashKiOS: `ModuleRules` fails the build when one is declared (`ModuleRulesTest`), and detekt forbids their imports. `[iOS]` LogroBusUI's `Package.swift` declares none (`ArchitectureTest`); the app target may.
- The injection point is `initKoin(…, telemetry = PlatformTelemetry(analytics, crashReporter))` on Android and `doInitKoin(appVersion:debug:telemetry:)` on iOS, documented on both functions and at both call sites. A recording double passed there receives telemetry only after consent (`AndroidGraphTest`, `IosGraphTest`). Swift classes conforming to the exported protocols work behind the shared gates, receive redacted errors, and go in `PlatformTelemetry` (`TelemetryTests`, iOS).
- Until LB-409, consent lives in memory and starts undecided, so the debug logs show no telemetry in the running app. Events are checked in tests.

## E1 Lines

### LB-101 Lines list
Refs: FR-LINE-01, FR-LINE-02, DD-08, NFR-A11Y-02

- **Given** `lines_2026-10-22.json`, **when** Lines opens, **then** "Day lines" lists 1, 2, 3, 4, 5, 6, 7, 9, 10, 11 and "Night lines (Búho)" lists B1, B2, B3, in that order, and there is no line 8.
- **Given** a line whose `status` is not 1, **when** the list renders, **then** its row shows a status chip.
- **Given** lines 4 and B1, which share a colour, **when** both rows render, **then** each badge shows its own code and the screen reader says "Line 4" and "Line B1", never a colour.
- **Given** 200 % font, **when** the list renders, **then** no line code is truncated, and display names wrap rather than clip.
- **Given** a first launch with no network, **when** Lines opens, **then** the error state shows the network reason and Retry, and Retry loads the list once connectivity returns (ADR-0008).
- **Given** the first Swift ViewModel, `LinesViewModel`, **when** a pull request changes it, **then** CI runs its tests in the `iOS ViewModel and snapshot tests` job, behind the iOS gate (moved from LB-002).
- **Given** the list scrolled down, **when** the user re-selects the Lines tab, **then** the list scrolls to its top, end to end through the shell, on both platforms (FR-NAV-03, moved from LB-012).

Part 1 (2026-11-02), the list over a repository contract and sample lines, done with this evidence. The first criterion's fixture, `lines_2026-10-22.json`, waits for part 2 (OQ-11, LB-003); every other criterion is met with the sample lines, which have the network's codes and shared colours:
- Order and groups: `LinesUiMapperTest` (the codes of the fixture, shuffled, and 9 before 10), the Android screen test, and `LinesViewModelTests` on iOS.
- The status chip: the mapper, the Android screen test, and the screenshots of both platforms (sample line 11 is not operational).
- Lines 4 and B1: the Android screen test, and `ShellUITests` on iOS (VoiceOver's label is "Line 4, Casa Blanca - Yagüe").
- 200 % font: the screenshots at 200 % on Android and at `accessibility3` on iOS.
- First launch with no network: `ObserveLinesTest` (an empty cache waits for the sync, whose failure is the result; a failed refresh of a full cache stays silent; Retry syncs again), and the ViewModel tests of both platforms.
- The CI job: `iOS ViewModel and snapshot tests`, one per test target of `LogroBusUI`, behind the iOS gate (owner decision, 2026-11-02).
- Scroll to top: `TabNavigationTest` on Android and `ShellUITests` on iOS, both through the shell on the debug build's sample lines (owner decision, 2026-11-02: debug builds list sample lines until LB-008 part 2).

Part 2, also done when: the first criterion holds with `lines_2026-10-22.json` through LB-005's DTOs, LB-006's normaliser and LB-008's sync, and the sample lines are gone from debug builds.

### LB-102 Lines search
Refs: FR-LINE-03, FR-LINE-16, NFR-I18N-03

- **Given** the query "pradoviejo", **when** it is typed, **then** lines 4 and 9 match.
- **Given** the query "yague", without the accent, **when** it is typed, **then** line 2 (Yagüe - Varea) matches.
- **Given** the stop name "Iglesia de Lardero", **when** it is typed, **then** every line whose patterns include that stop is listed, including 1 and B3.
- **Given** an active query, **when** results show, **then** the favourites carousel is hidden and the Day / Night grouping is kept. **When** the query is cleared, **then** the carousel returns.
- **Given** a query with no matches, **when** results update, **then** an empty state is shown with a single "Clear search" action.

Part 1 (2026-11-03), the search by a line's code, name and headsigns, done with this evidence. A stop's name needs the stops, which LB-008 part 2 caches, so the third criterion is part 2 (owner decision, 2026-11-03):
- "pradoviejo": `LineQueryTest` (the lines whose names end at Pradoviejo, and no other). The sample lines' names are placeholders (OQ-11): the real ones come with LB-003's fixture, and if Pradoviejo is only a stop of lines 4 and 9, part 2's stop search finds them.
- "yague": `LineQueryTest` ("yague" and "YAGÜE" find "Yagüe - Varea"), with the stop search's folding (NFR-I18N-03).
- An active query: `LinesUiMapperTest` (the results in their groups, without the carousel; an empty or blank query brings it back), the ViewModel tests of both platforms (a key never loads the lines again; a Retry keeps the query), `LinesScreenTest` on Android (the field keeps its text as the results change), the screenshots of both platforms, and end to end `ShellUITests` on iOS ("varea" finds lines 2 and B1 without the favourites, and Clear search brings them back).
- No matches: `LinesUiMapperTest`, `LinesScreenTest` (one sentence and Clear search, which empties the field), the screenshots of both platforms, and `ShellUITests`.

Part 2, also done when: the third criterion holds, a line found by the stop names of its patterns, folded as the stop search folds them (`architecture.md` §4.3), once LB-008 part 2 caches the stops; and the first holds with the real line names.

### LB-103 Favourite lines
Refs: FR-LINE-09, FR-LINE-12 to 15, FR-FAV-01

- **Given** no favourite lines, **when** Lines opens, **then** "Your favourite lines" shows a single hint card.
- **Given** the user taps the favourite toggle of line 3, **when** it changes, **then** a line 3 card appears immediately at the end of the carousel, and the toggle's label changes from "Add line 3 to favourites" to "Remove line 3 from favourites".
- **Given** line 10 is a favourite, **when** its card renders, **then** it shows the badge, "El Arco - Hospital San Pedro" and both headsigns, and tapping it opens line 10 detail.
- **Given** the user adds line 3 to favourites from line detail, **when** they go back to Lines, **then** the carousel includes line 3.
- **Given** the app is restarted, **when** Lines opens, **then** the favourites and their order are preserved.

Done (2026-11-02) with this evidence. Line detail itself is LB-104: until then, a card or a row opens a minimal detail with the line's badge, its name and the favourite toggle (owner decision, 2026-11-02). Debug builds use the sample lines, whose headsigns are the two ends of their names:
- The hint with no favourites: `LinesUiMapperTest`, `LinesScreenTest` on Android, and the screenshots of both platforms (the list with no favourites).
- Toggling line 3: `SetFavouriteLineTest` and `FavouriteLinesRepositoryTest` (a new favourite goes last); the ViewModel tests of both platforms; `LinesScreenTest` ("Add line 4 to favourites", "Remove line 10 from favourites"); end to end, `TabNavigationTest` on Android and `ShellUITests` on iOS, where the card appears at the end of the carousel and the label changes.
- Line 10's card: `LinesUiMapperTest` (badge, name and both headsigns, in the user's order), `LinesRepositoryTest` (the headsigns of each direction, outbound first), `LinesScreenTest` (the headsigns spoken as "Towards …", and the card opens line 10), the screenshots with favourite lines, and both shells' UI tests (a card opens its line's detail).
- Adding from line detail: `LineDetailUiMapperTest`, `LineDetailScreenTest`, the ViewModel tests of both platforms, and end to end through both shells (a row opens line 3, its toolbar adds it, and the carousel has it back on Lines).
- A restart: `FavouriteLinesRepositoryTest` (a new repository over the same database) and `AndroidGraphTest` (the favourites survive stopping and starting the graph over the device's database).

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
- **Given** a favourite line that has left the catalogue, which since LB-103 stays saved with no card, **when** edit mode opens, **then** it is listed as no longer served and can be removed, and it counts towards the limit of 20 (from LB-103's data review).
- **Given** a screen reader, **when** reordering, **then** "Move up" and "Move down" accessibility actions work without dragging. `[manual: TalkBack, VoiceOver]`

Done (2026-11-03) with this evidence. "Edit", on the "Your favourite lines" heading, opens a modal sheet with every favourite line in the user's order (owner decision, 2026-11-03). Android drags with the Reorderable library (ADR-0019); iOS uses a system list in edit mode:
- Dragging the third line to the top: `FavouriteLinesEditTest.lb110_draggingTheThirdLineToTheTop_savesTheNewOrderOnceTheDragEnds` on Android, where the order is sent once, when the drag ends; `FavouriteLinesRepositoryTest` (a new order survives a restart, is one change, keeps the lines it leaves out after the others, and skips lines no longer saved), `ReorderFavouriteLinesTest`, and the ViewModel tests of both platforms (the carousel follows). On iOS the drag is the system list's own; it is checked by hand on a device.
- Removing a line: `FavouriteLinesEditTest` (the row sends the line's toggle off, and the sheet follows a favourite removed or added) and, end to end, `ShellUITests.test_lb110_…` on iOS, where line 3 leaves the sheet and the carousel and its star goes off.
- The limit of 20: `FavouriteLinesRepositoryTest` (a 21st line is not saved, a line already saved at the limit is not a limit, and saved stops do not count), `SetFavouriteLineTest` (the toggle says the limit was reached and counts no `favourite_add`), the ViewModel tests of both platforms (the message shows once), and `FavouriteLinesEditTest` (the snackbar of the list and of the line detail on Android). On iOS the alert is checked by hand.
- A line no longer served: `LinesUiMapperTest` (the sheet lists it in its place, also when it is the only favourite), `FavouriteLinesEditTest` (Edit shows for it alone, and it can be removed), the edit sheet's screenshots on both platforms. It counts towards the limit, because the repository counts every saved line.
- Screen readers: `FavouriteLinesEditTest.lb110_talkBackMovesALineUpOrDown_withoutDragging` on Android (the actions, and each row's place, "2 of 3", before and after a move), `FavouriteLinesListTests` on iOS (the moves behind "Move up" and "Move down"), and the focus after a removal and after the sheet closes (`FavouriteLinesEditTest`). TalkBack and VoiceOver themselves are still to be checked on a device.

---

## E2 Near Me

### LB-201 Location permission and provider
Refs: FR-LOC-01 to 03, FR-NEAR-06, NFR-PRIV-02

- **Given** a fresh install, **when** the user finishes onboarding and opens Near Me, **then** no permission prompt appears until they tap "Turn on location" or recenter.
- **Given** a permission request, **when** the system prompt appears, **then** only foreground location is requested and approximate location is accepted: `[Android]` coarse, with fine optional; `[iOS]` When In Use.
- **Given** permission is denied, **when** Near Me renders, **then** the map frames the service area, "Near you" becomes the row "Turn on location to see nearby stops", and saved stops, pins and stop detail keep working.
- **Given** the user leaves Near Me and no other visible screen needs location, **when** the tab changes, **then** location updates stop (verified with a provider fake).
- **Given** a location fix, **when** logs and analytics are inspected, **then** they contain no coordinates.

Part 1 (2026-11-05): the permission flow on Near Me's root, which is the "Near you" section alone until the map and the sheet land (owner decision, 2026-11-05). Evidence:
- **Nothing asks before the tap:** opening Near Me shows the row and asks for nothing (`NearMeRouteTest`, `NearMeViewModelTests`). Both shells open on it: `TabNavigationTest` on Android; on iOS, `ShellUITests.test_lb201_…` finds the row and no system alert, with location reset to never asked, and the accessibility audit waits for the row, in light and dark and at the largest text.
- **Foreground only, approximate accepted:**
  - Android asks for `ACCESS_COARSE_LOCATION` and `ACCESS_FINE_LOCATION` together. A grant of approximate location alone turns the row off. The merged manifest declares these two location permissions and no other (`LocationPermissionsTest`).
  - iOS asks for When In Use. The app has the purpose string in Spanish and English, and no "Always" string or location background mode (`AppBundleTests`).
  - Once the system no longer asks, the row opens the app's page in the system settings: on Android after two denials (Robolectric's rationale shadow), and on iOS after one.
  - On Android, an interrupted request (no grants at all) opens nothing, a second tap while the prompt shows asks nothing more, a rotation during the prompt keeps the answer's meaning, and a device without the settings screen does not crash (`NearMeRouteTest`).
- **Denied:** "Near you" is the row "Turn on location to see nearby stops" (`ObserveLocationAccessTest`, `NearMeUiMapperTest`, `NearMeScreenTest`, screenshots and snapshots in light, dark and large text). A revoked permission brings it back. A failed platform service with the permission granted is `Unavailable`, which shows no row.
- **Updates stop with the screen:**
  - `ObserveLocationAccessTest` runs a provider fake, whose updates run only while location access is collected.
  - Android's ViewModel stops collecting 5 s after Near Me stops collecting its state (`NearMeViewModelTest`).
  - On iOS, cancelling the screen's task stops the preview's updates (`NearMeViewModelTests`). The view runs that task only while it shows (`onAppear` to `onDisappear`) and the app is active; no automated test drives that gating, which part 2's device pass checks.
- **No coordinates in logs:** nothing logs or sends a fix yet: `ObserveLocationAccessTest` has a fix arrive and nothing come out. `LocationFix` and `GeoPoint` print no coordinates (LB-004).

Part 2, with the screens that use location:
- the map frames the service area without location, and the recenter control asks for the permission first (LB-202, FR-NEAR-08);
- saved stops, pins and stop detail keep working without location (LB-202 to LB-205);
- "Near you" lists the nearby stops once location is on (LB-203);
- the first prompt follows onboarding's location card (LB-410);
- "Near you" explains a granted location whose service fails (FR-LOC-02, LB-203);
- TalkBack and VoiceOver passes on a device, and a device pass of the prompts, approximate location, the settings fallback, and the iOS location arrow gone once another tab shows.

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
Refs: FR-NEAR-04, FR-NAV-07, FR-STOP-03 to 05, FR-ERR-06, spec §5.7, §8.6, DD-09, DD-16

- **Given** stop 38, served by lines 1 and 33, **when** stop detail opens, **then** API-06 is called with `lines=1,33&previewMinutes=60` *(spec)*.
- **Given** the device clock is 3 min ahead of the server's `Date`, **when** an arrival the server expects in 5 min renders, **then** it shows "5 min" on both platforms (FR-ERR-06, LB-408 part 2). Shared code works the ETA out from the graph's `AppClock` when the row renders, never from the platform's clock. Swift reaches that clock, or the computed ETAs, through `SharedDependencies`. A test runs it through the graph, with a mock engine and a device clock off by 3 min.
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

Part 1 (2026-11-06): How to Get There's root with its two fields, which open nothing until place search lands (owner decision, 2026-11-06). Evidence:
- **With the location permission, the origin is "My location":**
  - `ReadInitialOriginTest` covers approximate and precise location.
  - `PlannerUiMapperTest` and both platforms' ViewModel tests check the fields.
  - Android end to end with approximate location granted: `TabNavigationTest.lb301_withLocation_…`.
  - Screenshots and snapshots in light, dark and large text.
- **Without it, both fields are empty:** the same tests check this, plus:
  - `TabNavigationTest.lb301_withoutLocation_…` on Android;
  - on iOS, `ShellUITests.test_lb301_…`, with location reset to never asked.
  - The empty fields show their hints: "Where from?" and "Where to?".
- **The origin follows the permission:** it is read again each time the root shows.
  - `PlannerRouteTest`: back from the background on Android.
  - `PlannerViewModelTest` on Android and `PlannerViewModelTests` on iOS: shown again after a grant or a revocation.
  - `PlannerViewTests` on iOS: the view reads the origin by itself once it is in a window.
  - iOS also reads it again each time the scene becomes active. No automated test drives that; part 2's device pass checks it.
- **Opening the root asks for nothing and starts no location updates:**
  - `ReadInitialOriginTest.lb301_readingTheOrigin_startsNoLocationUpdates`;
  - `ShellUITests.test_lb301_…`, which finds no system alert.
- **A screen reader stops once on each field,** and hears its name, then its value: "Origin, My location".
  - Android: `PlannerScreenTest` checks the merged node's description.
  - iOS: an accessibility label and value; `ShellUITests` finds each field by them.
  - The accessibility audit walks the root at the default and the largest text, and in dark mode.
- **Fields are not buttons yet:** they open nothing, so they announce no button role (`PlannerScreenTest.lb301_theFieldsOpenNothingYet`).

Part 2, with the sources of a place:
- choosing a stop (LB-008 part 2), a saved place (LB-311), an address (LB-302) or a map point (LB-013) for either field, which then shows its name, or the coordinates of a map point;
- the fields become buttons that open place search, so their role is announced;
- the TalkBack and VoiceOver passes on a device, and a device pass of the origin on iOS after a change in Settings.

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
- **Given** the departure, **when** "Depart at" is chosen, **then** the date and time pickers open at the picked time, or at now for "Leave now" or a picked time gone by, on the Madrid clock, and offer no day before today. *(Added with the selector, 2026-11-06.)*
- **Given** a picked time, **when** the root shows again, **then** the time stays. *(Added with the selector, 2026-11-06.)*

Done (2026-11-06). Evidence:
- **`departureAt`:** `PlanDepartureTest` (shared, on the Android host and the iOS simulator). "Leave now" departs at the app clock's now; "Depart at" 08:19 on 22 Oct 2026 departs at `2026-10-22T06:19:00.000Z`, and on 15 Jan 2027 at `2027-01-15T07:19:00.000Z`. The last Sundays of March (a skipped time moves on by the hour) and October (a repeated time is the first, in summer time, unless that has gone by; then the second) are tested too.
- **When a plan is requested:** there is no plan request until LB-306, whose criteria now send `departureAt` from the departure. Moving that part there is an agent decision for the owner to confirm in LB-303's pull request, as LB-304's results part was.
- **The selector:**
  - Shared: `PlannerDepartureUiTest`, the text's day on the Madrid calendar in summer and winter time and on both clock-change Sundays (after midnight in Madrid, its today is UTC's tomorrow), and the pickers' start, now for a time gone by.
  - Both ViewModels' `lb303_` tests: the menu's events, the pickers opening at now on the app clock, a cancel, "Leave now" again, a picked time staying when the root shows again, and its day counted again then (on iOS, the view told of it).
  - Android: `PlannerDepartureTest` (the chip, a button; its text in Spanish and English, with "a la" in the hour 1; the menu; the date, then the time; cancel on either; no day before today), `PlannerRouteTest` through the ViewModel, and screenshots of the longest text. `DepartureDialogsScreenshotTest` captures the date's and the time's dialogs in light, dark and at 200 %, with `assertAccessible` on each, and checks the time's at 200 % on SDK 33's linear scaling too.
  - iOS: `ShellUITests.test_lb303_…` end to end (the menu, the system's picker, Done, then "Leave now"); `MadridClockTests`, the picker's conversions from New York's and Tokyo's zones, in summer and winter time; snapshots of the longest text in light, dark and large text, and of every text in Spanish and English.
- **Screen readers:** the chip and the row are buttons spoken by their text. `assertAccessible` checks the root and both dialogs; Xcode's audit checks the root and the "Departure time" sheet, at the default and the largest text and in dark mode. The pickers' own issues, Material's and the system's, are exempted by name on the owner's decision (2026-11-07, `design_guidelines.md` §3.3). How TalkBack and VoiceOver read the pickers is for the device pass `[manual: TalkBack, VoiceOver]`.

### LB-304 Swap origin and destination
Refs: FR-PLAN-08, FR-PLAN-14

- **Given** origin A and destination B, **when** swap is tapped once, **then** the origin is B, the destination is A, and the existing results are cleared.
- **Given** a screen reader, **when** focus reaches the swap button, **then** it announces "Swap origin and destination". `[manual: TalkBack, VoiceOver]`
- **Given** a swap that put "My location" in the destination, **when** the root shows again with the permission, **then** the swap stays. *(Added with the swap, 2026-11-06.)*
- **Given** "My location" in either field, **when** the root shows again without the permission, **then** that field is empty. *(Added with the swap, 2026-11-06.)*

Done (2026-11-06). Evidence:
- **One tap swaps the ends:**
  - Shared: `PlanPlacesTest` (a swap, and a second one that restores the trip) and `PlannerUiMapperTest` ("My location" shown as the destination). Until LB-301 part 2 brings other places, the only trip to swap is "My location" and an empty field; a swap of two chosen places is tested then.
  - Both platforms' ViewModel tests: `lb304_oneSwap_…` and `lb304_afterASwap_theRootShowingAgain_keepsIt`.
  - Android, through the route: `PlannerRouteTest.lb304_oneTapOnSwap_exchangesTheFields`.
  - iOS, end to end: `ShellUITests.test_lb304_withLocation_oneTapOnSwap_makesMyLocationTheDestination` turns location on from Near Me's row, answers the system's prompt, then taps the button by its spoken label.
  - Screenshots and snapshots of the swapped state, in light, dark and large text.
- **The swap stays when the root shows again, and "My location" leaves either field without the permission:** `PlanPlacesTest` (`lb304_afterASwap_…`, `lb304_withoutThePermission_myLocationLeavesTheDestinationToo`, and a revoke then a re-grant after a swap), and both ViewModels' `lb304_afterASwap_theRootShowingAgain_keepsIt`.
- **The existing results are cleared:** How to Get There has no results until LB-306, whose criterion now names a swap among the changes that clear them (FR-PLAN-14). Moving this part there is an agent decision for the owner to confirm in LB-304's pull request.
- **Screen readers:**
  - The button is a button spoken "Swap origin and destination": `PlannerScreenTest` on Android; on iOS the UI test finds it by that label. `assertAccessible` and Xcode's audit check its label and its touch target.
  - It is read after both fields. On Android the card is a traversal group (`PlannerScreenTest.lb304_talkBackReadsBothFields_beforeTheSwapButton`). On iOS the fields' sort priority sets the order, which no automated check reads.
  - TalkBack and VoiceOver themselves, the iOS order included, are still to be checked on a device.

### LB-305 Service-area check
Refs: FR-PLAN-10

- **Given** a destination 10 km outside the service bounding box, **when** a plan is requested, **then** "This location is outside the service area" appears inline and no API-07 request is sent.
- **Given** a point 4 km outside the box, **when** a plan is requested, **then** API-07 is called.

### LB-306 Plan request and itinerary cards
Refs: FR-PLAN-04, FR-PLAN-14, DD-08

- **Given** origin 42.4650, -2.4450 and destination 42.4560, -2.4300 at 08:19, **when** a plan is requested, **then** at least one itinerary with a bus leg on line 10 or 3 is shown *(spec)*. CI checks this against `route_ok.json`; the nightly Maestro run checks it against the live source.
- **Given** a plan request, **when** API-07 is called, **then** `maxResults` is 5, and the cards are sorted by arrival time.
- **Given** a card, **when** it renders, **then** it shows the departure and arrival times, the total duration, the line badges in leg order and the total walking distance.
- **Given** results are on screen, **when** the origin, the destination or the time changes (a swap included, LB-304; the time, LB-303), **then** the old cards disappear before the new results arrive.
- **Given** a departure, **when** API-07 is called, **then** `departureAt` is `PlanDeparture.departureAt(now)` on the app clock, in UTC with milliseconds, as `2026-10-22T06:19:00.000Z` (LB-303). *(Moved from LB-303, 2026-11-06.)*
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
- **Given** English is selected, **when** the UI updates, **then** every UI string is in English without a restart (Android; on iOS the language is chosen in the system's Settings, which relaunches the app: ADR-0020, owner decision, 2026-11-03), and line and stop names are unchanged. `[manual: switching the language on a device, both platforms]`
- **Given** the ETA format is set to clock time, **when** arrivals render, **then** every ETA shows as HH:mm.
- **Given** Clear cache is used, **when** it completes, **then** live and image caches are emptied, while favourites, places, recents and static data are kept.

Done (2026-11-03) with this evidence. Settings has three sections, Appearance, Arrivals and Data, with no new navigation (owner decision, 2026-11-03). Each platform keeps the app's language its own way (ADR-0020):
- Settings' offer: `SettingsScreenTest` on Android (the sections as headings, the language, theme, ETA format, data status and Clear cache rows, the choices in use selected, the events sent, the first load's layout); `ShellUITests.test_lb403_…` on iOS end to end (the language row, both pickers, a theme chosen and set back, Clear cache's alert); `SettingsUiMapperTest`; both platforms' ViewModel tests; the screenshots of the first load and the content on both platforms (light, dark, 200 %).
- **The language, without a restart on Android only** (owner decision, 2026-11-03): on Android the dialog offers the system language, Español and English, and `AppSettingsTest.lb403_aChosenLanguage_isAndroidsAppLanguage` sets it through AppCompat, which recreates the activity in it; a language changed in Android's own settings shows in Settings (`…_aLanguageChosenInAndroidsOwnSettings_showsInSettings`); the languages are declared to Android (`AppLanguagesTest`), and the app bundle keeps every language's strings. On iOS the row shows the language the app runs in and opens the app's page in the iPhone's Settings, where iOS keeps the per-app language and relaunches the app after a change. Every string comes from each platform's resources, and line and stop names are shown as delivered, so they do not change. Robolectric runs one Android and does not reconfigure the app for a new language, so the switch itself is checked on a device.
- The ETA format: `PreferencesStoreTest` and the platform stores' tests (kept and observed), and `EtaDisplayTest.lb403_clockTimeFormat_showsEveryEtaAsHHmm`: in the clock-time format every ETA, "Now" and minutes included, is the Madrid clock time (`…_clockTimes_areOnTheMadridClockInSummerTimeToo`). No screen renders arrivals yet; LB-205's build their `EtaUi` with `EtaDisplay.of`.
- Clear cache: `CacheDirectoryCachesTest` on Android (the cache directory emptied, the database and the settings file kept) and `CachesDirectoryCachesTest` on iOS (`Library/Caches` emptied, the database kept, the URL cache's folder left to it), `ClearCacheTest`, and both platforms' ViewModel tests (the message once, and a failure said). The MVP keeps no live data on disk (ADR-0008), and image caches will live in those directories.
- The theme, beyond the criteria: `AppThemeTest` on Android (each theme's night mode; from Android 12 the system's per-app night mode, with nothing read before the first frame; below it AppCompat, with the saved theme read once per process), `AppSettingsTest.lb403_choosingDarkInSettings_turnsTheAppDark_andSettingsStays`, and `AppAppearanceTests` on iOS (the first frame in the saved theme, the windows' style); the settings survive a restart of the graph (`AndroidGraphTest.lb403_…`) and a read that fails (`PreferencesStoreTest`).

### LB-404 About data
Refs: FR-SET-03, NFR-PRIV-05

- **Given** More → About data, **when** it opens, **then** it shows the data source, the last successful sync time of each dataset and the disclaimer that predictions are provided by the operator.
- **Given** the latest sync failed, **when** the page renders, **then** it still shows the time of the last *successful* sync.
- **Given** the attributions section, **when** it renders, **then** it lists "Data: Ayuntamiento de Logroño", OpenStreetMap and CartoCiudad / IGN.

Done early, with LB-010 part 2 (2026-10-28): the last successful sync of each dataset (tests `lb404_…`).
- `ObserveLastSyncs` lists every static dataset in order (spec §8.5: lines and stops), with its last successful sync or none.
- A row without a success counts as never synced, and rows of v1.0 datasets are ignored.
- Keeping an earlier success through a failed sync is LB-008's part (`architecture.md` §4.4).
- More → About data opens it since LB-012, with its title and an up button.

### LB-405 Localisation
Refs: NFR-I18N-01 to 03

- **Given** the device language is Spanish or English, **when** the app runs, **then** every UI string is in that language. Any other device language falls back to Spanish.
  - A device whose languages include Spanish or English gets the first of them; with neither, Spanish, on both platforms (ADR-0020). On Android the app's strings fall back from `values/`, and `MainActivity` attaches in the same language, so Material's and Android's strings, the plural rules and the date formats agree with them (`AppLocalesTest`, `lb405_…`). iOS falls back to the app's development language, Spanish, and the root view gives SwiftUI that language in the device's region, so the lookups that take a locale agree too (`AppLocaleTests`; `ShellUITests.test_lb405_…`, French and "French, then English", the package targets' words included). Below Android 13, once "System" is chosen in Settings during a session, Android's own words follow the device until the app restarts (ADR-0020).
  - Every string exists in both languages: Android lint's `MissingTranslation`, an error, checks `values-en/`; on iOS, `LocalisationTest.lb405_everyIosCatalogueEntryIsInSpanishAndEnglish` checks every catalogue entry, plural forms included, `lb405_everyKeyTheIosCodeUsesIsInItsTargetsCatalogue` the keys the code uses, arguments included, `lb405_packageTargetsLookTheirKeysUpInTheirOwnBundles` the bundle they are looked up in, and `lb405_everyIosCatalogueKeyIsInUse` that none is left over.
- **Given** the device time zone is America/New_York, **when** times render, **then** they are in Europe/Madrid.
  - Done early, with LB-010 part 2, for About data's times (tests `lb405_…`). `MadridDateTime` converts through the CET and CEST transitions, and each platform formats it in a fixed zone with its locale. Settings' data status uses the same formatting (LB-403), and an ETA's clock time is the Madrid clock's (`EtaDisplayTest`).
- **Given** a count of 1 and a count of 2 minutes, **when** they render in either language, **then** the plural forms come from platform resources.
  - Done early, with LB-011: the spoken ETAs and delays are plurals on both platforms, Android's `plurals` and the iOS catalogue's plural variations (`EtaTextTest.lb011_eachEtaCaseIsSpoken` and `…_inSpanish`, on both). They are tested at 1 and 4 minutes, and 4 is in the same plural category as 2 in both languages. The visible "1 min" and "2 min" use an abbreviation that has no plural in either language.
- Done when: a lint or test check fails on any hard-coded user-facing string in UI code.
  - `LocalisationTest.lb405_androidUiCodeHasNoHardcodedUserFacingText` and `lb405_iosUiCodeHasNoHardcodedUserFacingText`, in `:tools:architecture-tests`, with their rules unit-tested in `UiTextRulesTest` (`code_style.md` §1).

### LB-406 Accessibility pass
Refs: NFR-A11Y-01 to 06, NFR-PLAT-01

- **Given** an arrival of line 2 in 4 minutes, **when** a screen reader focuses it, **then** it reads "Line 2, in 4 minutes". `[manual: TalkBack, VoiceOver]`
- **Given** live updates while focus is on an arrival row, **when** data refreshes, **then** focus does not move and at most one polite announcement is made every 30 s.
- **Given** 200 % font, **when** every MVP screen is captured, **then** no ETA or line code is truncated.
- **Given** every interactive element, **when** checked with Accessibility Scanner and Accessibility Inspector, **then** no touch target is smaller than 48 dp or 44 pt.
- **Given** `[iOS]` Reduce Transparency and Increase Contrast, **when** custom glass surfaces render on iOS 26 or later, **then** they stay legible. `[manual: VoiceOver]`

Part 1 (2026-11-04): automated checks that every screen built so far runs, and future screens join (`testing_strategy.md` §4.7). Evidence:
- **Touch targets (the fourth criterion):**
  - Android: `assertAccessible` from `:android:core:testing`, on every screenshot case in light and at 200 % font (every state) of the shell and its rail, More, Lines, the line detail, the favourites editor, Settings, its language dialog and About data. It checks labelled controls and 48 dp touch areas as TalkBack and Accessibility Scanner get them, from the view's node info; `AccessibilityChecksTest` shows a small control in a window's corner, and two side by side, failing.
  - iOS: `AccessibilityAuditUITests`, with Xcode's audit (44 pt hit regions among its checks) on every screen and the Clear cache alert at the default and the largest text, and again in dark mode.
  - Accessibility Scanner and Accessibility Inspector by hand are left for the device check.
- **200 % font (the third criterion):**
  - Android: the same checks' cut-text rule on every screen at 200 %. `EtaTextTest.lb406_everyEtaCase_isWholeAt200PercentFont` (in Spanish and English) and `LineBadgeTest.lb406_everyLineCodeIsWholeAt200PercentFont` cover the ETAs and codes that no screen shows whole yet.
  - iOS: the audit's clipped-text check at the largest text, besides the snapshots at `accessibility3`.
  - A mutation that ellipsised the line names of Android's Lines list failed 9 cases.
- **Contrast (NFR-A11Y-01, NFR-A11Y-06):**
  - The iOS audit found the system's secondary grey and blue under 4.5:1 in Settings and About data. They are now the tokens' secondary text and accent (`design_guidelines.md` §5), in light and dark, and `TokensTests.lb406_…` checks both on the system's backgrounds, raised ones and Increase Contrast included. The snapshots take the app's tint, and SwiftLint forbids the system's greys.
  - Android's placeholder tokens were already checked (`TokensTest`, LB-011), and so were line badges, by NFR-A11Y-06's rule (LB-011).
- **Spoken labels of ETAs and line badges (NFR-A11Y-02):** met in LB-011 (`EtaTextTest`, `LineBadgeTest` and their iOS counterparts).

Part 2, done when:
- the first criterion holds on the arrival rows, with LB-205;
- the second holds on the live arrivals, with LB-205 and LB-213;
- every map has its list equivalent (NFR-A11Y-04), with LB-203, LB-104 and LB-308;
- the fifth holds on the first custom glass, the map's buttons (LB-202);
- TalkBack, VoiceOver, Accessibility Scanner and Accessibility Inspector have been run on a device.

### LB-407 Favourites persistence
Refs: FR-FAV-03, FR-FAV-04

- **Given** favourites, places and recents exist, **when** the app updates to a new version, **then** all of them are preserved.
- **Given** a backup restore onto a new device (`[Android]` Auto Backup, `[iOS]` iCloud or device backup), **when** the app opens, **then** favourites and places are restored.
- **Given** a saved stop that has been missing from API-02 for 7 consecutive daily syncs, **when** "My stops" renders, **then** it shows "No longer served" and stays until the user deletes it.
- **Given** that stop reappears in a later sync, **when** "My stops" renders, **then** the "No longer served" label is gone.
- A provisional stop made up from a pattern that API-02 lacks (DD-10), such as 384, is still served, so it never counts as missing (LB-008 part 1; for the owner to confirm).

Part 1 (2026-11-05): the backups, the first two criteria. Evidence:
- **Android (Auto Backup and a transfer to a new device):** the manifest gives data extraction rules from Android 12 and full backup content rules before it, which keep the same files: the database (favourites, places and recent searches, with the lines and stops it caches, ADR-0013), to whose include Android adds its journal and write-ahead log, the settings file (ADR-0018) and, below Android 13, AppCompat's language file (ADR-0020). Nothing else, so a map's tile cache cannot use up Auto Backup's 25 MB.
  - `BackupTest` saves a favourite line and the theme through the app's graph, copies the files each rule set backs up while the app runs, restores them to an app with no data, as on a new device, and opens the graph again: both are there, and a stand-in tile cache is not, for a cloud backup, a device transfer and a backup before Android 12. The language goes through AppCompat's own storage and comes back too. Dropping the database, the settings or the language from a rule set fails it. Under Robolectric the last commit is already in the database file when it is copied, so the journal is not exercised; Android adds it to the include itself.
  - On a Pixel 9 Pro emulator (API 37), with `bmgr` and the local transport: line 3 saved and a stand-in tile cache in `files/`, a backup, the app's data cleared, a restore. Only `databases/logrobus.db` and its journal came back, and the app opened with line 3 in "Your favourite lines".
- **iOS (iCloud and device backups):** they keep an app's Application Support, except a file or folder excluded from them, and its user defaults. The database is in Application Support and not excluded, and the settings are in the standard user defaults, so both were already backed up. `IosDatabaseTest.lb407_iosBacksTheDatabaseUp` saves a favourite and checks the file and its folder; marking either excluded fails it. A restore on a device is left for the device check.
- **Updates (the first criterion):** an update replaces the app's code and keeps its data on both platforms, so the database and the settings stay where they are. The app keeps them across a restart: `lb007_androidKeepsTheDatabaseInAFileAcrossLaunches`, `lb007_iosKeepsTheDatabaseInAFileAcrossLaunches`, `lb103_favouriteLinesAreKeptInTheDevicesDatabase_acrossARestart` and `lb403_settingsAreKeptOnTheDevice_acrossARestart`. After the first release, a schema change comes with `.sqm` migrations, which CI will verify once a release exists (ADR-0013). `[manual: install an update over a build with favourites, on a device]`

Part 2, done when:
- the third and fourth criteria hold in "My stops", with LB-204 and LB-208 (saved stops) and the daily sync of LB-008 part 2. `updateMissingSyncs` already counts the missing syncs after each successful sync (LB-008 part 1);
- the owner has confirmed the fifth;
- a restore has been run on an iPhone and an Android phone.

### LB-408 Server clock offset
Refs: FR-ERR-06, spec §8.6

- **Given** the device clock is 3 min ahead of the server `Date` header, **when** an arrival expected in 5 min of server time renders, **then** it shows "5 min", not "2 min".
- **Given** no `Date` sample yet, **when** ETAs are computed, **then** the device clock is used, and the first sample corrects them on the next render.

Part 1 (2026-11-05): the shared pieces, put together by hand. Evidence:
- **`ServerClockOffsetTest`** (`:shared:umbrella`, on the JVM and the iOS simulator) wires the real source client over a mock engine to a server-corrected clock on a device clock 3 min ahead, and counts an ETA from that clock with the ETA rule `EtaDisplay.of`.
  - Before any response, an arrival the server expects in 5 min shows 2 min, from the device clock.
  - After a response with the server's `Date`, the same arrival shows 5 min.
  - Taking out the client's `Date` sampling, or the offset in `ServerCorrectedClock.now`, fails both tests.
- **The pieces were built earlier,** in LB-004, LB-005, LB-010, LB-011 and LB-403:
  - LB-004: the clock and its smoothing over 5 samples (`AppClockTest`).
  - LB-005: the `Date` of every response (`SourceClientTest`).
  - LB-010: one clock bound as both `ServerCorrectedClock` and `AppClock` in the graph (`GraphAssertions`). That the graph's source client gets this instance (`Koin.kt`) is not under test yet; part 2's test runs through the graph.
  - LB-011: `EtaText`, which shows `Minutes(n)` as "n min" on both platforms (`EtaTextTest`, `EtaTextTests`).
  - LB-403: `EtaDisplay.of`, which takes the now to count from.
  - In Kotlin, detekt forbids direct calls to `Clock.System.now()` and `System.currentTimeMillis()` (AGENTS.md §5, rule 5). Swift has no such check yet.
- **The offset applies at any size,** as spec §8.6 has it, not only beyond FR-ERR-06's 60 s. Below 60 s it still matters: with the device clock 50 s ahead, an arrival 4 min 30 s away would show 3 min. `lb408_anOffsetUnder60SecondsCorrectsTheEtaToo` pins it, and a 60 s threshold in `ServerCorrectedClock.now` fails it.

Part 2, done with LB-205's rows (its criterion on the device clock):
- each ETA is worked out in shared code from the graph's `AppClock` when a row renders, so the first response corrects the next render;
- Swift reaches that clock, or the computed ETAs, through `SharedDependencies`;
- a test runs through the graph, with a mock engine and a device clock off by 3 min;
- a check keeps Swift from computing ETAs with `Date()`, such as a SwiftLint rule, if Swift gets the clock.

### LB-409 Consent, analytics and crash reporting
Refs: NFR-PRIV-03, NFR-OBS-03, OQ-16, ADR-0009. Blocked until the vendor is chosen; the contracts are LB-017.

- **Given** first launch, **when** the app starts, **then** a consent dialog appears before any analytics or crash data is collected, and declining sends nothing.
- **Given** Settings, **when** it opens, **then** analytics and crash reporting have separate toggles, and changes take effect immediately.
- **Given** consent was given, **when** events are inspected in the debug view, **then** only the NFR-OBS-03 events are sent, with no free text or coordinates.
- **Given** the vendor SDKs, **when** the app ships, **then** their automatic collection is off by default (for Firebase: `firebase_analytics_collection_enabled` and `firebase_crashlytics_collection_enabled` false in the Android manifest, `FIREBASE_ANALYTICS_COLLECTION_ENABLED` and `FirebaseCrashlyticsCollectionEnabled` NO in the iOS Info.plist), and each SDK's own collection, uncaught crashes and automatic events included, follows the user's consent and its withdrawal. The LB-017 gates cover only the calls made through the contracts.
- **Given** the persisted `ConsentStore` that replaces LB-017's `MemoryConsentStore`, **when** the dialog and Settings write it, **then** each decision is updated atomically, so two writers never lose one, and Swift reaches the store through `SharedDependencies`.

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
