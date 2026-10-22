# Open questions

| Field | Value |
| --- | --- |
| Last updated | 2026-10-22 |
| Status values | `open`, `proposed` (a default is in use until confirmed), `resolved` |

OQ-01 to OQ-12 come from spec §10.4; OQ-13 onwards were raised after the spec. If you are working on something that depends on an `open` question, use its documented fallback. If there is none, ask the owner. When a question is resolved, record the answer and where it was applied, and log it in `project_log.md`.

| Id | Question | Owner | Blocks | Status | Default or answer |
| --- | --- | --- | --- | --- | --- |
| OQ-01 | Native SwiftUI on iOS, or Compose Multiplatform for both? | Tech lead | Sprint 1 | resolved | Native SwiftUI with Liquid Glass on iOS, Compose with M3 Expressive on Android, shared KMP core (ADR-0001) |
| OQ-02 | Meaning of `status` values other than `1` for lines and stops | Operator | none | open | Treat `1` as operational and anything else as not operational, with a status chip |
| OQ-03 | Why do some planner ids (603, 609, 613) and VM next-stop ids (632) not exist in API-02? Are there two stop id spaces? | Operator | none | open | DD-18 fallback: resolve by name plus nearest point, otherwise show the name only |
| OQ-04 | Is a GTFS or GTFS-RT feed available? Can planned timetables be published? | Operator | Timetable feature | open | No timetables in v1 (DD-14) |
| OQ-05 | Is a staging environment available? | Operator | none | open | Test against fixtures; production only from the recording job and Maestro |
| OQ-06 | Official mapping of ET `directionRef` codes to Ida and Vuelta | Operator | none | open | Inference algorithm of spec §5.6 with a learned cache |
| OQ-07 | Stable public URL for news items, for share links | Operator | LB-507 | open | Share the news section URL |
| OQ-08 | Night service calendar (Búho days and hours) as data | Operator | LB-206 text | open | `night_service_text` from remote config |
| OQ-09 | Definition of the FN20 and FN50 tiers and their eligibility texts | Operator | LB-503 | open | `fare_tier_texts` from remote config |
| OQ-10 | Vector tile provider and map style licensing | Product | LB-013 (development), LB-702 (release) | open | Development: a free OSM-based vector style (for example OpenFreeMap, terms to be verified), switchable through `map_style_url`. Never OSM's public raster tiles in release builds |
| OQ-11 | Terms of reuse for the API data; is a formal agreement needed? | Product, legal | Release | open | Show the attributions; no public release without an answer |
| OQ-12 | Will v2 ticketing use the existing Keycloak client, or a dedicated mobile client id? | Operator | v2 | open | none |
| OQ-13 | iOS deployment target: the spec says iOS 16, but Liquid Glass needs iOS 26 | Project owner | LB-001 | resolved (2026-10-22) | iOS 18.0, with Liquid Glass on 26+ and a material fallback on 18–25 (ADR-0003, accepted) |
| OQ-14 | App identifiers: store display name, Android `applicationId`, iOS bundle id, Kotlin package root, URL scheme | Project owner | LB-001 | resolved (2026-10-22) | `applicationId`, bundle id and Kotlin package root: `com.ruizurraca.logrobus`. Display name "LogroBus" and scheme `logronobus://` (spec §6.15) kept as defaults. The https deep-link domain of spec §6.15 is still to be chosen before LB-603 |
| OQ-15 | Which tab opens at launch? | Project owner | LB-012 | resolved (2026-10-22) | Always Lines; deep links open their target tab. G1 is met with one tap to Near Me |
| OQ-16 | Remote config backend: Firebase Remote Config, or a static JSON hosted by the project? | Project owner | LB-015 | open | Compiled defaults until decided (the app must work without remote config) |
| OQ-17 | Placement of favourite stops in the three-section model | Project owner | LB-204 | resolved (2026-10-22) | "My stops" section in the Near Me sheet, with live arrivals; favourite lines go in the Lines carousel (ADR-0002) |
