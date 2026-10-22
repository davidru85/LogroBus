# Backlog

| Field | Value |
| --- | --- |
| Last updated | 2026-10-22 |
| Status values | `todo`, `in-progress`, `blocked`, `done`, `dropped` |
| Priority | Spec priority: P0 = launch blocker, P1 = launch target, P2 = post-launch |

Stories are numbered `LB-<epic><nn>`. Each one cites the requirements it implements, so read those spec sections before starting. When you pick up a story, set it to `in-progress`; when it meets the definition of done (`AGENTS.md` §11), set it to `done` and log it in `project_log.md`.

## Milestones

| Milestone | Goal | Epics | Exit criteria |
| --- | --- | --- | --- |
| M0 Foundations | A KMP skeleton that builds on both platforms, with the data layer proven against fixtures | E0 | CI green; contract and DD tests pass; both apps show three empty tabs |
| M1 Lines | Lines section complete | E1 | Lines list, favourites carousel, search and line detail with live buses on both platforms |
| M2 Near Me | Near Me section complete | E2 | Map, sheet, saved and nearby stops, stop detail with live arrivals; G1 met on test devices |
| M3 How to Get There | Trip planner complete | E3 | Plan from a location or address to any point; itinerary detail |
| M4 MVP hardening | Internal alpha, then closed beta | E4 | Offline, settings, accessibility, i18n and performance NFRs met; Maestro journeys green |
| M5 v1.0 | Public release | E5, E6, E7 | Remaining P0/P1 modules (news, fares, recharge points, widget, notifications); legal and store readiness |
| v2 | Accounts and ticketing | none | Spec §10.5 v2 row; operator agreement (OQ-11, OQ-12) |

## E0 Foundations (M0)

| Id | Story | Refs | Priority | Status |
| --- | --- | --- | --- | --- |
| LB-001 | Scaffold the KMP project: version catalog, `build-logic` convention plugins, the shared modules, `androidApp`, and an `iosApp` Xcode project linked to the `Shared` framework. Initialise CI-ready Gradle wrapper | Spec §8.2; ADR-0001, 0004 | P0 | todo |
| LB-002 | CI: Linux job (shared tests, Android build, ktlint, detekt, Kover), macOS job (framework, iOS build and tests, SwiftLint), dependency scanning | NFR-MNT-02, NFR-SEC-05 | P0 | todo |
| LB-003 | `scripts/record-fixtures.sh` and the initial golden fixtures in `fixtures/`, plus the weekly re-record job | Spec §10.2 | P0 | todo |
| LB-004 | `:shared:core`: `AppClock` with server offset, `Logger`, dispatchers, `AppResult` and `AppError`, `RemoteConfig` with compiled defaults, `AppLifecycle`, geo utilities | Spec §8.6, §8.9 | P0 | todo |
| LB-005 | `:shared:network`: `HttpClient` factory (headers, timeouts, 2xx rule, `Date` sampling), lenient `Json`, per-item decoding, DTOs and services for API-01 to API-08 | `api_spec.md` §1–4 | P0 | todo |
| LB-006 | Normaliser and mappers for lines, stops, VM, ET and the planner, with one test per DD rule (DD-01 to DD-11, DD-15 to DD-18) | Spec §5.3–5.6 | P0 | todo |
| LB-007 | `:shared:database`: SQLDelight schema (spec §8.8 plus the additions in `architecture.md` §4.3), FTS stop search with accent folding | FR-STOP-01, NFR-I18N-03 | P0 | todo |
| LB-008 | `StaticSyncEngine`: seed import, TTLs, content hash, validation thresholds, atomic replace; `scripts/update-seed.sh` | FR-ERR-01, FR-ERR-04, NFR-REL-03 | P0 | todo |
| LB-009 | `PollingEngine`: keyed de-duplication, intervals with a floor, jitter, backoff, pause and resume on app lifecycle | Spec §8.6, FR-ERR-03, NFR-PERF-07 | P0 | todo |
| LB-010 | DI and interop: Koin graph in `:shared:umbrella`, SKIE, the `ViewModels` factory, the iOS `@Observable` bridge pattern | ADR-0004 | P0 | todo |
| LB-011 | Design system foundations. Android: `MaterialExpressiveTheme`, tokens, `LineBadge`, `EtaText`, expressive wrappers. iOS: tokens, `LineBadge`, `EtaText`, `adaptiveGlass` helpers | `design_guidelines.md` §4–6 | P0 | todo |
| LB-012 | App shell: three tabs with per-tab back stacks, More toolbar entry, onboarding placeholder, on both platforms | ADR-0002, ADR-0005 | P0 | todo |
| LB-013 | Map foundation: MapLibre on both platforms, light and dark styles from `map_style_url`, attribution, development tile source | Spec §7.4, OQ-10 | P0 | todo |
| LB-014 | Write ADRs for the remaining spec §8.3 choices (Ktor, SQLDelight, Koin, kotlinx-datetime, Coil, MapLibre, Firebase, Multiplatform Settings) | NFR-MNT-03 | P1 | todo |
| LB-015 | Remote config backend: decide (OQ-16) and implement behind the `RemoteConfig` interface | Spec §8.9 | P1 | blocked (OQ-16) |

## E1 Lines (M1)

| Id | Story | Refs | Priority | Status |
| --- | --- | --- | --- | --- |
| LB-101 | Lines list grouped Day / Night (Búho) and sorted by code; rows with badge, display name and status chip | FR-LINE-01, FR-LINE-02 | P0 | todo |
| LB-102 | Search bar filtering by code, terminus or stop name, ignoring accents and case | FR-LINE-03 | P0 | todo |
| LB-103 | Favourite lines: star toggle on rows and in detail; "Your favourite lines" carousel at the top; empty hint | FR-LINE-09, FR-FAV-01 | P0 | todo |
| LB-104 | Line detail: map with both shapes, direction toggle labelled with headsigns, ordered stop list; tap a stop to open Stop detail | FR-LINE-04 | P0 | todo |
| LB-105 | Next predicted arrival per stop in line detail (API-05), or a dash | FR-LINE-05 | P1 | todo |
| LB-106 | Live vehicles on the line map with bearing, filtered by direction when known; neutral marker when unknown | FR-LINE-06 | P0 | todo |
| LB-107 | Vehicle animation along the shape between polls; removal after the grace period | FR-LINE-07 | P1 | todo |
| LB-108 | Vehicle sheet: line, headsign, next stop, delay, report age | FR-LINE-08 | P1 | todo |
| LB-109 | Provisional stops: "Provisional" tag and distinct marker | FR-LINE-11, DD-10 | P1 | todo |
| LB-110 | Edit favourite lines: reorder by drag, remove; limit of 20 | FR-FAV-02, FR-FAV-05 | P1 | todo |

## E2 Near Me (M2)

| Id | Story | Refs | Priority | Status |
| --- | --- | --- | --- | --- |
| LB-201 | Just-in-time foreground location permission; `LocationProvider` on both platforms; updates stop when no visible screen needs them | FR-LOC-01 to 03 | P0 | todo |
| LB-202 | Near Me map: user location, initial camera rules, stop pins with clustering, selected-stop styling | FR-STOP-10, spec §7.4 | P0 | todo |
| LB-203 | Persistent bottom sheet with detents; "Near you" list of the 5 closest stops within 800 m, sorted by walking distance (API-08, at most 40 candidates) | FR-HOME-03, FR-STOP-11, NFR-A11Y-04 | P0 | todo |
| LB-204 | "My stops" section: saved stops with their next 3 arrivals; empty state with "Find a stop" | FR-HOME-02, FR-HOME-07 (adapted) | P0 | todo |
| LB-205 | Stop detail (sheet in Near Me, pushed screen elsewhere): header with headsigns, served lines, arrivals from API-06 with `previewMinutes = 60`, ETA rules, "Updated N s ago", 20 s refresh | FR-STOP-03 to 05, spec §5.7 | P0 | todo |
| LB-206 | Empty arrivals message, and the night-service text from remote config for night-only stops | FR-STOP-06, OQ-08 | P0 | todo |
| LB-207 | Line filter chips and "Group by line" in stop detail | FR-STOP-04, FR-STOP-07 | P1 | todo |
| LB-208 | Save a stop with an alias (up to 30 characters); edit saved stops (reorder, rename, delete) | FR-STOP-08, FR-FAV-01, FR-FAV-02 | P0 | todo |
| LB-209 | Stop search by name or id under 100 ms, with results told apart by line badges and headsigns | FR-STOP-01, FR-STOP-02, DD-16 | P0 | todo |
| LB-210 | "Directions to this stop" switches to How to Get There with the destination set; "Show on map" from pushed stop detail | FR-STOP-09 | P1 | todo |
| LB-211 | "Buses" layer: all live vehicles (API-03), coloured by line, grey for an unknown line; polling only while visible | FR-LIVE-01 to 03 | P1 | todo |
| LB-212 | "Routes" layer: all line shapes | FR-LIVE-05 | P1 | todo |
| LB-213 | Live refresh every 20 s while visible, immediately on returning to the foreground, and on pull or refresh | FR-HOME-04, FR-HOME-06 | P0 | todo |

## E3 How to Get There (M3)

| Id | Story | Refs | Priority | Status |
| --- | --- | --- | --- | --- |
| LB-301 | Origin and destination inputs: current location, saved place, stop, map long-press, address | FR-PLAN-01 | P0 | todo |
| LB-302 | CartoCiudad geocoder: capture fixtures, confirm parameters, debounce 300 ms, at least 3 characters, at most 8 results, La Rioja filter | FR-PLAN-02, `api_spec.md` §5 | P0 | todo |
| LB-303 | "Leave now" / "Depart at" selector, sent as UTC `departureAt` | FR-PLAN-03 | P0 | todo |
| LB-304 | One-tap swap of origin and destination | FR-PLAN-08 | P0 | todo |
| LB-305 | Client-side check against the service area expanded by 5 km | FR-PLAN-10 | P1 | todo |
| LB-306 | Plan request (API-07, `maxResults = 5`) and itinerary cards sorted by arrival time | FR-PLAN-04 | P0 | todo |
| LB-307 | No-route messages mapped from `rejection.reason`, with criteria values | FR-PLAN-07, DD-17 | P0 | todo |
| LB-308 | Itinerary detail: map (walk dashed, bus in line colour) and step list with expandable intermediate stops; unresolved stops shown by name | FR-PLAN-05, DD-18 | P0 | todo |
| LB-309 | Real-time ETA overlay for the first bus leg departing within 60 min | FR-PLAN-06 | P1 | todo |
| LB-310 | Recent searches (last 10) | FR-PLAN-09 | P1 | todo |
| LB-311 | Saved places (Home, Work, custom): create from search or map, rename, delete | FR-FAV-01 | P0 | todo |

## E4 MVP hardening (M4)

| Id | Story | Refs | Priority | Status |
| --- | --- | --- | --- | --- |
| LB-401 | Offline banner and cached content on every screen; offline arrivals shown as expired clock times | FR-ERR-02, FR-ERR-05 | P0 | todo |
| LB-402 | Standard UX states on every data-driven screen, with screenshot tests (light, dark, 200 % font) | Spec §7.3, §10.6 | P0 | todo |
| LB-403 | Settings: language, theme, ETA format, data refresh status, clear cache | FR-SET-01 | P0 | todo |
| LB-404 | About data: data source, last sync per dataset, prediction disclaimer, attributions | FR-SET-03, NFR-PRIV-05 | P0 | todo |
| LB-405 | Localisation es and en, plurals, Madrid time display, locale formatting | NFR-I18N-01 to 03 | P0 | todo |
| LB-406 | Accessibility pass: spoken labels, 200 % font, contrast, polite announcements, list equivalents, glass legibility | NFR-A11Y-01 to 06 | P0 | todo |
| LB-407 | Favourites included in OS backups; "No longer served" after 7 missing syncs | FR-FAV-03, FR-FAV-04 | P0 | todo |
| LB-408 | ETAs use the server clock offset when the device clock is off by more than 60 s | FR-ERR-06 | P1 | todo |
| LB-409 | Consent dialog, Crashlytics and Analytics with the NFR-OBS-03 events only; Settings toggles | NFR-PRIV-03, NFR-OBS-03 | P1 | todo |
| LB-410 | Onboarding: 3 cards (value, favourites, location rationale) | SC-18, FR-LOC-01 | P1 | todo |
| LB-411 | Performance budgets: cold start, first ETA, search latency, map fps, download size | NFR-PERF-01 to 06 | P0 | todo |
| LB-412 | Maestro flows for the five critical journeys (`architecture.md` §8) | Spec §10.1 | P1 | todo |

## E5 v1.0 content (M5)

| Id | Story | Refs | Priority | Status |
| --- | --- | --- | --- | --- |
| LB-501 | News list and detail, with the HTML sanitiser, native rendering, in-app browser and offline cache of 30 items | FR-NEWS-01 to 04, DD-13, NFR-SEC-02, 03 | P0 | todo |
| LB-502 | Service notices: derive `affectedLineIds`; banner on Lines; link from line detail | FR-NEWS-05, 06, FR-LINE-10, FR-HOME-01 | P1 | todo |
| LB-503 | Fares: categories, variant matrix, locale prices, validity, QR/EMV hidden, tier texts from config | FR-FARE-01 to 06 | P0 | todo |
| LB-504 | Recharge points: map layer and list, type filter, distance sort, directions, copy address, offline | FR-RCH-01 to 04 | P1 | todo |
| LB-505 | Info: CMS footer pages, social links, contact, open-source licences | FR-SET-02 | P1 | todo |
| LB-506 | Featured news (under 14 days old) at the top of More → News | FR-HOME-05 | P1 | todo |
| LB-507 | Share a news item | FR-NEWS-07, OQ-07 | P2 | todo |

## E6 Platform extras (M5)

| Id | Story | Refs | Priority | Status |
| --- | --- | --- | --- | --- |
| LB-601 | Home-screen widget showing the next 3 arrivals of a chosen saved stop (Glance, WidgetKit) | FR-WID-01 to 03 | P1 | todo |
| LB-602 | Local notifications for new notices on favourite lines and stops (WorkManager, BGAppRefreshTask) | FR-NOTIF-01 to 03, 05 | P1 | todo |
| LB-603 | Deep links re-targeted to the three tabs; unknown ids show a "Not found" toast | Spec §6.15, ADR-0002 | P1 | todo |
| LB-604 | Persisted line filter on the Buses layer | FR-LIVE-04 | P2 | todo |
| LB-605 | "Report a problem" email draft with an anonymised `DataQualityReport` | FR-SET-04 | P2 | todo |
| LB-606 | Arrival alert (foreground service, Live Activity) | FR-NOTIF-04 | P2 | todo |

## E7 Release readiness (M5)

| Id | Story | Refs | Priority | Status |
| --- | --- | --- | --- | --- |
| LB-701 | Privacy policy, Google Data safety, Apple privacy label | NFR-PRIV-01 to 04 | P0 | todo |
| LB-702 | Production vector tile provider and map style licensing | OQ-10, R6 | P0 | blocked (OQ-10) |
| LB-703 | Data reuse permission from the operator (owner task) | OQ-11, R7, NFR-PRIV-05 | P0 | blocked (OQ-11) |
| LB-704 | Store listings, screenshots, app icons, final identifiers | OQ-14 | P0 | todo |
| LB-705 | Observability dashboards and the parse-failure alert | NFR-OBS-01, 02 | P1 | todo |
| LB-706 | Evaluate the project proxy (edge cache, coalescing, versioned contract) | Spec §8.10, R1, R2 | P2 | todo |
