# Requirements baseline

| Field | Value |
| --- | --- |
| Status | Baseline 1.0 for the MVP (milestones M0–M4) |
| Last updated | 2026-10-22 |
| Based on | Spec v0.1 §6 and §9; ADR-0002, ADR-0003; owner decisions OQ-13 to OQ-17 |
| Acceptance criteria | [`acceptance_criteria.md`](acceptance_criteria.md) |

This document freezes the scope of the MVP. It defines the requirements that the spec does not cover because they came after it: three-section navigation, the favourite-lines carousel, the Near Me sheet, planner behaviour, and platform constraints. It also states how the spec's own requirements are adapted to the new structure.

The spec remains the source for every requirement it already defines (`FR-*`, `NFR-*`). This document only adds new ids, never renumbers existing ones, and records each adaptation explicitly.

## 1. Scope baseline

| Module | MVP (M0–M4) | v1.0 (M5) | v2 |
| --- | --- | --- | --- |
| NAV (new) | FR-NAV-01 to 07 | none | none |
| LINE | FR-LINE-01 to 09, 11 to 16 | FR-LINE-10 (needs notices) | none |
| STOP | FR-STOP-01 to 11 | none | none |
| NEAR (new) | FR-NEAR-01 to 09 | Recharge-points layer (FR-RCH-01) | none |
| LIVE | FR-LIVE-01 to 03, 05 | FR-LIVE-04 | none |
| PLAN | FR-PLAN-01 to 14 | none | none |
| HOME (adapted, §3.5) | FR-HOME-02, 03, 04, 06, 07 | FR-HOME-01, 05 | none |
| FAV | FR-FAV-01 to 05 | none | none |
| ERR | FR-ERR-03 to 06 | FR-ERR-01, 02 (offline mode, ADR-0008) | none |
| Offline mode (G3, NFR-REL-04) | none: static data is cached, but offline use is not guaranteed | All (LB-607) | none |
| SET | FR-SET-01, 03 | FR-SET-02, 04 | none |
| LOC | FR-LOC-01 to 03 | none | none |
| NEWS, FARE, RCH, WID | none | All | none |
| NOTIF | none | FR-NOTIF-01 to 03, 05 | FR-NOTIF-04 if it slips from v1.0 (P2) |
| Deep links (spec §6.15) | none | All, re-targeted (`architecture.md` §6) | none |
| Accounts, TSC cards, payments, QR | none | none | All (spec §1.2) |

**Non-functional requirements:**
- All of spec §9 applies to the MVP, except:
  - NFR-SEC-02 and NFR-SEC-03, which concern CMS content and apply from v1.0;
  - NFR-REL-04, which applies from v1.0 with the offline mode (ADR-0008).
- NFR-PRIV-04 (store privacy declarations) is completed as the final MVP step (LB-414).
- NFR-PRIV-03 and NFR-OBS-03 apply once an analytics backend exists, which is deferred under OQ-16.
- The additions in §4 also apply to the MVP.

## 2. Priorities and keywords

- MUST, SHOULD and MAY follow RFC 2119, as in the spec.
- P0 means an MVP blocker, P1 an MVP target that may move to v1.0 with the owner's approval, and P2 post-launch.
- Each requirement maps to one or more backlog stories. The `Story` column below gives the primary one.

## 3. New and adapted functional requirements

### 3.1 NAV: navigation and app shell

| Id | Requirement | Priority | Story |
| --- | --- | --- | --- |
| FR-NAV-01 | The app MUST have exactly three top-level destinations, in this order: Lines, Near Me, How to Get There. | P0 | LB-012 |
| FR-NAV-02 | The app MUST always open on Lines. The only exception is a launch from a deep link, which opens the link's target. The exception takes effect when deep links ship (v1.0, LB-603). | P0 | LB-012 |
| FR-NAV-03 | Each tab MUST keep its own back stack. Switching tabs preserves every stack. Re-selecting the active tab pops it to its root, or scrolls to the top if it is already at the root. | P0 | LB-012 |
| FR-NAV-04 | Each tab root MUST offer a toolbar action that opens More. In the MVP, More contains Settings and About data. | P0 | LB-012 |
| FR-NAV-05 | On Android, the top-level navigation MUST adapt to the window size: a navigation bar on compact widths, and a navigation rail on medium and expanded widths. | P1 | LB-012 |
| FR-NAV-06 | A skippable onboarding of three cards (value, favourites, location rationale) MUST be shown once, before the first tab, on first launch. It MUST NOT request any permission itself. | P1 | LB-410 |
| FR-NAV-07 | Stop detail MUST be one screen definition with identical content and actions everywhere. It is shown as a sheet in Near Me and as a pushed screen from Lines and from itinerary detail. | P0 | LB-205 |

### 3.2 LINE additions: favourite lines

| Id | Requirement | Priority | Story |
| --- | --- | --- | --- |
| FR-LINE-12 | The Lines tab MUST show a "Your favourite lines" section above the full list: a horizontal carousel of cards in the user's manual order (FR-FAV-02). | P0 | LB-103 |
| FR-LINE-13 | A favourite line card MUST show the line badge, the display name (at most two lines) and both headsigns. Tapping the card opens line detail. | P0 | LB-103 |
| FR-LINE-14 | When there are no favourite lines, the section MUST show one hint card explaining how to add one. The section never disappears. | P0 | LB-103 |
| FR-LINE-15 | Every line row MUST have a favourite toggle with an accessible label, for example "Add line 3 to favourites". Changes appear in the carousel immediately. | P0 | LB-103 |
| FR-LINE-16 | While a search query is active, the favourites section MUST be hidden, and the results MUST keep the Day / Night (Búho) grouping. | P1 | LB-102 |

### 3.3 NEAR: Near Me

| Id | Requirement | Priority | Story |
| --- | --- | --- | --- |
| FR-NEAR-01 | Near Me MUST show a full-screen map with stop pins and, when permitted, the user's location, plus a persistent bottom sheet that cannot be dismissed. | P0 | LB-202 |
| FR-NEAR-02 | The sheet MUST support three detents (peek, medium, large). The map MUST stay interactive at peek and medium. | P0 | LB-203 |
| FR-NEAR-03 | With no stop selected, the sheet MUST list "My stops" (saved stops with their next 3 arrivals, FR-HOME-02) above "Near you" (FR-HOME-03). | P0 | LB-204, LB-203 |
| FR-NEAR-04 | Tapping a stop pin or a stop row MUST select the stop: its pin is enlarged (spec §7.4), the sheet moves to medium, and the sheet shows stop detail (FR-STOP-03 to 09). Closing the stop returns the sheet to the lists. | P0 | LB-205 |
| FR-NEAR-05 | A search field floating over the map MUST open stop search (FR-STOP-01, 02). Selecting a result selects that stop as in FR-NEAR-04 and centres the map on it. | P0 | LB-209 |
| FR-NEAR-06 | Without location permission, the map MUST frame the service area. "Near you" MUST be replaced by one row that opens the permission flow (FR-LOC-01), and every other Near Me feature MUST keep working. | P0 | LB-201 |
| FR-NEAR-07 | A layer control MUST toggle the Buses layer (FR-LIVE-01 to 03) and the Routes layer (FR-LIVE-05). Stops are always shown. The layer state persists across sessions. | P1 | LB-211, LB-212 |
| FR-NEAR-08 | A recenter control MUST move the camera to the user's location, requesting permission first if needed (FR-LOC-01). | P0 | LB-202 |
| FR-NEAR-09 | Live data in the sheet MUST refresh as required by FR-HOME-04 and FR-STOP-05, and only while Near Me is the visible tab. | P0 | LB-213 |

### 3.4 PLAN additions

| Id | Requirement | Priority | Story |
| --- | --- | --- | --- |
| FR-PLAN-11 | The origin MUST default to the current location when location permission is granted. Otherwise both fields start empty. | P0 | LB-301 |
| FR-PLAN-12 | Place search MUST return results grouped in this order: saved places, stops, addresses (FR-PLAN-02). | P0 | LB-302 |
| FR-PLAN-13 | When no plan is in progress, the How to Get There root MUST show the saved places as chips and the recent searches (FR-PLAN-09). | P0 | LB-311, LB-310 |
| FR-PLAN-14 | Changing the origin, destination or time MUST invalidate the current results. Results computed for other inputs MUST never be shown. | P0 | LB-306 |

### 3.5 HOME: adaptation to the three-section model

ADR-0002 dissolved the Home screen of the spec. Each FR-HOME requirement keeps its meaning and moves as follows:

| Id | New location | Milestone |
| --- | --- | --- |
| FR-HOME-01 | Service notice banner at the top of Lines | v1.0 |
| FR-HOME-02 | "My stops" in the Near Me sheet (FR-NEAR-03) | MVP |
| FR-HOME-03 | "Near you" in the Near Me sheet (FR-NEAR-03) | MVP |
| FR-HOME-04 | Refresh cadence of the Near Me sheet (FR-NEAR-09) | MVP |
| FR-HOME-05 | Featured news at the top of More → News | v1.0 |
| FR-HOME-06 | Pull or refresh action in the Near Me sheet and in stop detail | MVP |
| FR-HOME-07 | Empty state of "My stops", with a "Find a stop" action that focuses the Near Me search (FR-NEAR-05) | MVP |

The spec's Home acceptance criterion ("ETAs for both favourite stops visible within 3 s of cold start") is measured from cold start plus one tap on Near Me (§4, NFR-PERF-02).

### 3.6 Other MVP adaptations

The MVP has no offline mode: without connectivity, screens show the network error state with Retry (FR-ERR-05), and live data retries with backoff (FR-ERR-03). See ADR-0008.

| Id | Adaptation in the MVP | Full scope |
| --- | --- | --- |
| FR-SET-01 | Settings omits the notification toggles, because the NOTIF module ships in v1.0 | v1.0 |
| FR-FAV-05 | The limit of 20 per type applies to lines, stops and places alike | MVP |
| NFR-REL-01 | The crash-free rate is read from the Play Console and App Store Connect until a crash-reporting backend exists (OQ-16) | Once OQ-16 is decided |
| NFR-PRIV-03, NFR-OBS-03 | Not applicable until an analytics backend exists (OQ-16) | Once OQ-16 is decided |

## 4. New and adapted non-functional requirements

| Id | Requirement |
| --- | --- |
| NFR-PLAT-01 | The iOS deployment target is 18.0. Liquid Glass is used on iOS 26 and later. The fallback appearance on iOS 18–25 MUST be fully functional and meet every NFR-A11Y requirement (ADR-0003). |
| NFR-PLAT-02 | Android `minSdk` is 26 (Android 8.0), and `targetSdk` is the latest stable SDK at the time of release. |
| NFR-PLAT-03 | Phones are the target form factor. iOS is **iPhone only** in the MVP: no iPad support (OQ-19). Android layouts MUST stay usable on tablets and foldables (FR-NAV-05). |
| NFR-OBS-04 | Analytics and crash reporting MUST be used only through the shared telemetry contracts (`AnalyticsTracker`, `CrashReporter`, `ConsentStore`). Events are typed and cannot carry free text or coordinates. Implementations are no-op until the vendor is chosen (ADR-0009, OQ-16). |
| NFR-PERF-02 (adapted) | Cold start to first ETA in Near Me, measured with an automated tap on Near Me as soon as the first frame is drawn: under 3 s at p75 over 4G. |
| NFR-REL-05 | The app MUST be fully functional with the remote-config defaults compiled into the binary, with no remote-config backend configured or reachable (OQ-16 is deferred). |
| NFR-MNT-04 | Brand colours, typography and shapes MUST be defined as design tokens in one place per platform, so the neutral placeholder theme can be replaced without touching feature code (OQ-18). |
| NFR-MNT-05 | Every quality gate (tests, lint, documentation checks) MUST be runnable from the command line through scripts or Gradle tasks. No gate may depend on a specific AI agent or IDE (ADR-0006). |

## 5. Delivery constraints

- **Visual design:** Figma designs for the MVP are delivered near the end of the MVP. Until then, screens follow `design_guidelines.md` with the neutral placeholder theme, and the Figma designs are applied in LB-413.
- **Privacy declarations** are prepared last (LB-414), once the data flows and the telemetry vendor are final.
- **The repository** is public since 2026-10-23, with `main` protected (LB-415). It was due before the MVP is completed; the owner brought it forward.
- **Architecture:** clean architecture with feature modules, a platform-specific presentation layer and shared presentation models (ADR-0007).

## 6. Change control

- This baseline changes only with the owner's approval. Each change gets a row in §7 and a `project_log.md` entry, plus an ADR if the change is architectural.
- A new requirement gets the next free id in its module and is never renumbered. A dropped requirement keeps its id, with its status shown as `dropped`.
- When a requirement changes, `acceptance_criteria.md` and `backlog.md` are updated in the same change.

## 7. Change log

| Date | Change | Approved by |
| --- | --- | --- |
| 2026-10-22 | Baseline 1.0: FR-NAV, FR-NEAR, FR-LINE-12 to 16, FR-PLAN-11 to 14, NFR-PLAT, NFR-REL-05, NFR-MNT-04/05; FR-HOME adaptation | Project owner |
| 2026-10-22 | Offline mode moved to v1.0 (G3, FR-ERR-01, FR-ERR-02, NFR-REL-04; ADR-0008); iPhone only (NFR-PLAT-03); telemetry contracts (NFR-OBS-04, ADR-0009); privacy declarations as the final MVP step (LB-414); repository public and `main` protected before MVP completion (LB-415); Figma designs applied near the end of the MVP (LB-413) | Project owner |
