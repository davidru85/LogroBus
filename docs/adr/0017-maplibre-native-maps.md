# ADR-0017: MapLibre Native for maps on both platforms

| Field | Value |
| --- | --- |
| Status | Proposed |
| Date | 2026-10-26 |
| Deciders | Project owner (spec §8.3 baseline, 2026-10-22). Written up by the agent for LB-014; awaiting acceptance |
| Related | NFR-MNT-03, OQ-10, spec §7.4, §8.3, `architecture.md` §5.1–5.2, LB-013, LB-702, risk R6 |

## Context

Maps are central to the app:
- the line detail shows shapes, stops and live buses;
- Near Me is a full-screen map with stop pins.

Line colours are data (DD-08), so both platforms must draw the same shapes and markers in the same colours. The project has no budget for per-request map fees.

Choosing the tile provider and its licence is a separate, open question (OQ-10). The style is switchable through `map_style_url` (`api_spec.md` §8). Release builds must never use OpenStreetMap's public raster tiles (risk R6).

## Decision

- **Android:** MapLibre Native, through `maplibre-compose`, in `:android:core:map`.
- **iOS:** MapLibre Native iOS (Swift package), wrapped in `MapUI` (`architecture.md` §5.1–5.2).
- **Style:** both platforms load a vector style from `map_style_url` and show the provider's attribution.
- **Tile provider:** a separate decision.
  - Development: OQ-10, LB-013.
  - Release: LB-702.
  - Until the owner decides, the compiled `map_style_url` default is empty (LB-004).
- **No dependency yet.** MapLibre enters the version catalog and the Swift package with LB-013.

## Consequences

- **The same renderer and style** on both platforms, so line colours, shapes and markers match.
- **No per-request fees** for map rendering. Tile hosting still has costs and terms, decided in OQ-10.
- **More work than the platform maps.** MapLibre's Compose and SwiftUI integrations need more hand-written code than Google Maps or MapKit, especially for accessibility. Every map task keeps a list equivalent (NFR-A11Y).

## Alternatives considered

- **Google Maps SDK on Android and MapKit on iOS.** Rejected:
  - two renderers and two styling systems mean different colours and shapes on each platform;
  - Google Maps needs an API key with billing;
  - MapKit's style control is limited.
- **Mapbox Maps SDK.** Rejected: in 2020 Mapbox moved its native SDKs to a proprietary licence with usage-based pricing.
- **A web map** (Leaflet or MapLibre GL JS) **in a WebView.** Rejected: weaker performance, weaker accessibility and a non-native feel. It would also bring a JavaScript-enabled WebView into the app, which the project already avoids for CMS content (AGENTS.md §10).
