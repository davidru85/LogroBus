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
| OQ-16 | Vendors for analytics, crash reporting and remote config | Project owner | LB-409, LB-015 | partly decided (2026-10-22) | Likely Firebase Analytics and Crashlytics, not final. Vendor-neutral contracts are built now (LB-017, ADR-0009), with no-op implementations until the decision. Remote config keeps its compiled defaults (NFR-REL-05) |
| OQ-17 | Placement of favourite stops in the three-section model | Project owner | LB-204 | resolved (2026-10-22) | "My stops" section in the Near Me sheet, with live arrivals; favourite lines go in the Lines carousel (ADR-0002) |
| OQ-18 | Brand colours, typography and app icon | Project owner | LB-413 | resolved (2026-10-22) | They come with the Figma designs delivered near the end of the MVP. Until then, a neutral placeholder theme defined as tokens (NFR-MNT-04) |
| OQ-19 | iPad support: iPhone-only, or a universal app? | Project owner | LB-001 (Xcode device family) | resolved (2026-10-22) | The MVP does not support iPad: iPhone only (NFR-PLAT-03). Android stays usable on tablets and foldables (FR-NAV-05). Revisit after the MVP |
| OQ-20 | Repository visibility and branch protection | Project owner | LB-415 | resolved (2026-10-22) | Private for now, with local gates authoritative. It becomes public before the MVP is completed; `main` is then protected on GitHub and CI runs |
| OQ-21 | Project licence (proprietary or open source, and which one)? | Project owner | LB-415 (before going public) | open | All rights reserved. Dependency licence allow-list in `engineering_workflow.md` §9 |
| OQ-22 | Domain for https deep links (spec §6.15), the privacy policy and store listings | Project owner | LB-414 (privacy policy URL), LB-603 | open | `logronobus://` scheme only until a domain exists |
