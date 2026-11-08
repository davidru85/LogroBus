# Security and privacy

| Field | Value |
| --- | --- |
| Status | Draft; store declarations are prepared as the final MVP step (LB-414) |
| Last updated | 2026-11-08 |
| Based on | Spec NFR-SEC-01 to 05, NFR-PRIV-01 to 05 |

LogroBus has no accounts, no project-owned servers and no advertising. This document records what data the app handles, which permissions it uses, the security controls, and the draft store declarations. **It is not legal advice; the owner validates the privacy policy and the store answers before release.**

## 1. Principles

1. **Data minimisation.** Collect nothing that a feature does not need, and keep everything on the device.
2. **Foreground only.** The app never accesses location in the background.
3. **No third-party SDKs that profile users.** Analytics or crash reporting, if added (OQ-16), is consent-gated and carries no personal data, coordinates or free text.
4. **Treat the source as untrusted input.** Every payload is validated (spec §5.4), and HTML is sanitised.

## 2. Data inventory

| Data | Origin | Stored | Leaves the device | Recipient | Purpose |
| --- | --- | --- | --- | --- | --- |
| Precise or approximate location | OS, when the user is on Near Me or plans from "My location": Google Play services' Fused Location Provider on Android, Core Location on iOS | Memory only | Yes, as coordinates in API-07 and API-08 requests | Logroño transport BFF | Nearby stops, walking distances, trip planning |
| Coordinates of map points and saved places used in a plan | User | Saved places: on-device database | Yes, in API-07 requests | Logroño transport BFF | Trip planning |
| Address search text | User | No | Yes, in geocoder requests | CartoCiudad (IGN) | Address suggestions |
| Favourites, saved places, recent searches, settings | User | On-device database and settings. The settings (LB-403) are the theme and the ETA format, as enum names, in a DataStore file on Android and the app's user defaults on iOS (ADR-0018); the app's language is kept by the OS on iOS and from Android 13, and by AppCompat in a file of the app's below Android 13 (ADR-0020). "Clear cache" empties only the app's cache directories | Only through the user's own OS backup (Android Auto Backup and transfers to a new device, iCloud and device backups). Android's backup rules keep only the database, the settings file and, below Android 13, AppCompat's language file; the database also caches the public lines and stops (LB-407) | The user's Google or Apple account | Persistence (FR-FAV-03) |
| Public operator data committed as test fixtures: lines, stops, live vehicles, estimated calls, routes, walking distances, fares, CMS pages, CartoCiudad candidates and recharge points | Recorded from the public APIs into `fixtures/` by `scripts/record-fixtures.sh` (LB-003) | In the repository, as golden payloads for the contract tests | No: they never reach the app at runtime | none | Regression tests that decode the source's real shapes. `fixtures/recharge_points.json` lists about 100 recharge points by the shop's name and street address, which are the operator's public data for API-11; the owner chose to keep the capture rather than a shape-only copy (2026-11-08, [`acceptance_criteria.md`](acceptance_criteria.md) §LB-003, OQ-11 still open). No user data is in any fixture: the recorder sends no user input, and `vm_all_with_object_lineRef.json` and the ET captures hold no coordinates tied to a person |
| IP address and `User-Agent` | Inherent to HTTPS | No | Yes | BFF, CartoCiudad, map tile provider | Network transport |
| Analytics events and crash reports | Telemetry contracts (ADR-0009, LB-017): typed events and breadcrumbs with ids, counts, flags and enums only (a Konsist rule), consent-gated per kind and opt-in; crash reports carry error types and stacks, never messages (`RedactedThrowable`); no-op in release builds and console logging in debug builds until the vendor is chosen. The vendor SDKs must ship with automatic collection off and tied to consent (LB-409). `StopView` and `LineView`, with a vendor's install id and timestamps, could hint at someone's usual stops: the privacy declarations (LB-414) must cover them | none | Only with consent, once a vendor is active (likely Firebase Analytics and Crashlytics, OQ-16) | Vendor to be confirmed | Stability and product metrics |

The project collects **no** device identifiers, advertising ids, contacts, photos or account data.

## 3. Permissions

| Platform | Permission | When requested | Notes |
| --- | --- | --- | --- |
| Android | `INTERNET`, `ACCESS_NETWORK_STATE` | Granted at install | Connectivity-aware error messages |
| Android | `ACCESS_COARSE_LOCATION` + `ACCESS_FINE_LOCATION` | Just in time: first Near Me recenter, the "Near you" row, or "My location" in the planner | The user may grant approximate only, and every feature must work with it. **Never** `ACCESS_BACKGROUND_LOCATION` |
| Android | `POST_NOTIFICATIONS` | v1.0, only when the user opts in to notice alerts | none |
| iOS | `NSLocationWhenInUseUsageDescription` (es and en strings) | Just in time, as above | **Never** "Always" authorisation. The user may disable precise location |
| iOS | Notifications | v1.0, opt-in only | none |

**As built (LB-201 part 1):**
- **Android** declares `ACCESS_COARSE_LOCATION` and `ACCESS_FINE_LOCATION` in the app's manifest. A test reads the merged manifest and finds these two location permissions, and no other: no `ACCESS_BACKGROUND_LOCATION`.
- **iOS** sets `NSLocationWhenInUseUsageDescription`: in Spanish in the build settings, and in Spanish and English in `InfoPlist.xcstrings`. A test finds both languages, the two Spanish texts equal, and no "Always" purpose string and no `location` background mode.
- **The prompt comes only from Near Me's "Turn on location" row**, so far; the recenter control comes with LB-202, and any prompt from How to Get There's "My location" with LB-301 part 2 or LB-306. Opening Near Me asks for nothing.
  - Android asks for both permissions at once, so the user may grant approximate location only.
  - Once the system no longer asks, the row opens the app's page in the system settings instead: on Android after two denials, and on iOS after one. An interrupted request opens nothing.
- **How to Get There (LB-301 part 1) reads the foreground permission only:** its origin is "My location" with the permission and empty without it. It asks for nothing and starts no location updates (`ReadInitialOriginTest.lb301_readingTheOrigin_startsNoLocationUpdates`, `ShellUITests.test_lb301_…`). The fix is read when a plan is requested (LB-306).
- **Location runs only while Near Me shows,** and stops when another tab is selected or the app leaves the foreground (FR-LOC-03): on Android 5 s after the screen stops collecting, and on iOS when the screen's task is cancelled. The fixes stay in memory, and nothing logs or sends them yet; the nearby stops (LB-203) will send them in API-08 requests.

## 4. Security controls

| Area | Control | Requirement |
| --- | --- | --- |
| Transport | HTTPS only. Android network security config with `cleartextTrafficPermitted="false"`; iOS App Transport Security defaults with no exceptions. Both are in place and tested (LB-005). No certificate pinning (spec §8.7) | NFR-SEC-01 |
| HTTP client | No cookies and no response cache: OkHttp keeps neither by default, and the iOS NSURLSession is configured without them. A failed call is logged by method, path and error type, a dropped item by type and index, never with a query, a body or an error message (LB-005) | Spec §8.7, NFR-PRIV-02 |
| Source payloads | Lenient decoding plus validation: ids, coordinates within the service bounding box, string length caps. Invalid items are dropped and logged | Spec §5.4 |
| CMS HTML (v1.0) | Allow-list sanitiser in shared code; no WebView with JavaScript; only `https` links are opened | NFR-SEC-02, 03 |
| Deep links | Ids must be numeric and positive, coordinates within range and inside the service area plus 5 km, and unknown parameters are ignored. Invalid links open the parent screen | Spec §6.15 |
| Secrets | None in the binary or the repository. `scripts/checks/check-secrets.sh` runs in the pre-commit hook. Signing material lives outside the repository (`release_process.md` §3) | NFR-SEC-04 |
| Dependencies | Dependabot alerts (enabled 2026-10-24) and update PRs. The `Dependency review` CI job fails a pull request that adds a dependency with a known critical vulnerability (`ci_cd.md` §6). Together, the review and the alerts are the NFR-SEC-05 equivalent of OWASP Dependency-Check, which is not used (owner decision, 2026-10-25). Licence allow-list (`engineering_workflow.md` §9) | NFR-SEC-05 |
| Logging | No coordinates, no search text and no personal data in logs. Release builds log only errors: the app shell passes its build type to the shared graph (LB-010) | NFR-PRIV-02 |
| Backups | Only the user's own data goes into the OS backups (LB-407, `architecture.md` §4.3): on Android, rules that include the database, the settings file and, below Android 13, AppCompat's language file, and nothing else; on iOS, Application Support and the user defaults. Android's cloud backup does not require client-side encryption (`disableIfNoEncryptionCapabilities` is not set): on a device without a screen lock, Google encrypts the backup on its servers but not end to end, and it would then hold the saved places' coordinates once the planner saves them (LB-311). Requiring it would leave a user without a lock with no backup at all, so a new phone would lose their favourites. For the owner to confirm before the store declarations (LB-414) | FR-FAV-03, NFR-PRIV |
| AI agents | Agents run with project permission rules that deny reading signing material and secrets (`agent_harness.md` §4). No production credentials exist in developer environments | none |

### 4.1 Threats considered

| Threat | Mitigation |
| --- | --- |
| Malicious or broken CMS content (script injection) | Sanitiser with a strict allow-list; native rendering only |
| Tampered or malformed source data | Per-item validation; atomic sync with sanity thresholds; previous dataset kept |
| Crafted deep links | Strict parsing and range checks |
| Network interception | HTTPS with platform trust; no cleartext |
| Device theft | No sensitive data stored; OS sandbox; backups contain only the database (favourites, places, recents and the public lines and stops it caches) and the settings, the language included |
| Source outage or blocking | Error states with Retry and a cached static dataset in the MVP; full offline mode in v1.0 (ADR-0008); identifying `User-Agent`; optional proxy (spec §8.10) |
| Supply-chain compromise | Pinned versions; Dependabot; reviewed upgrades; small dependency set |

## 5. Store declarations (drafts, to finalise in LB-414, the final MVP step)

**Google Play Data safety (draft):**
- Location, approximate and precise: sent off the device to provide app functionality (trip planning and nearby stops), processed ephemerally, not used for advertising, not sold. Whether it counts as "shared" with the transport operator's service must be confirmed against Google's definitions.
- App activity and app info and performance: none, while OQ-16 is deferred.
- The Android app includes Google Play services Location (LB-004). Check Google's Data safety guidance for Play services SDKs before declaring.
- Data is encrypted in transit. Users cannot request deletion because nothing is stored server-side; this must be stated.

**Apple privacy nutrition label (draft):**
- Likely "Data Not Collected": location and search text are only sent to third-party services in real time to fulfil a request, and are not retained by the project. Confirm against Apple's definition before submission.
- This changes if crash reporting or analytics is added (OQ-16).

## 6. Privacy policy outline (v1.0)

The policy is published in es and en at a stable https URL; the domain is still to be chosen (see the deep-link domain item in `open_questions.md`). It covers:
- Who is responsible.
- What data is processed and why (§2).
- The third parties involved: Logroño transport service, CartoCiudad (IGN), the map tile provider (OQ-10), and the OS backup providers.
- That nothing is stored server-side, plus how to exercise rights under GDPR and LOPDGDD.
- Contact details.
