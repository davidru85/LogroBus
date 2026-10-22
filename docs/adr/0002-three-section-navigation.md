# ADR-0002: Three-section navigation: Lines, Near Me, How to Get There

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-10-22 |
| Deciders | Project owner |
| Related | Spec §7.1, §7.2, §6.1 (FR-HOME), §6.15; OQ-15, OQ-17; `design_guidelines.md` §2–3 |

## Context

Spec §7.1 defines a bottom bar with five destinations: Home, Lines, Map, Plan and More. The project owner chose a "Live Mobility and Planning" structure for the MVP instead, with three clearly defined sections:

1. **Bus lines**, with the user's favourite lines at the top (a horizontal carousel or fixed section), then the full list sorted by code, with a search bar.
2. **Near Me**, an interactive map with the user's location and nearby stop pins. Tapping a pin opens a bottom sheet with the wait times for the next buses.
3. **How to Get There**, a trip planner with origin and destination search, recommended routes combining lines, walking times and step-by-step directions.

## Decision

The app has exactly three top-level tabs: Lines, Near Me and How to Get There. The spec screens are redistributed as follows:

- **Home is dissolved.** Favourite lines become the Lines carousel. Favourite stops with live arrivals, and nearby stops, become sections of the Near Me bottom sheet ("My stops", "Near you"), which also serves as the list equivalent of the map (NFR-A11Y-04). Placing favourite stops in Near Me is a proposal that the owner still has to confirm (OQ-17).
- **Map becomes Near Me.** Buses and routes are optional layers; recharge points are added as a layer in v1.0.
- **Plan becomes How to Get There.**
- **Stop detail** is one component, shown as a sheet in Near Me and as a pushed screen from Lines and itineraries.
- **The More content** (Settings and About data in the MVP; News, Fares, Recharge points and Info in v1.0) is reached from a toolbar action on each tab root and never becomes a tab.
- **Deep links** are re-targeted to the new structure (`architecture.md` §6).

Every functional requirement of the spec keeps its meaning. Only where it is presented changes. The full mapping is in `design_guidelines.md` §2.2.

## Consequences

- The navigation is simpler and task-focused: browse, now, plan.
- The use case `GetHomeDashboard` (spec §8.4) is replaced by `ObserveSavedStopArrivals` and `GetNearbyStops`.
- G1 (live ETAs one tap from launch) depends on the launch tab, which OQ-15 decides. With the proposed default, saved-stop ETAs are one tap away in Near Me.
- Secondary content is less discoverable. This is acceptable for the MVP and should be revisited with usage data.
- FR-HOME-01 (the notice banner on Home) moves to the top of the Lines tab in v1.0.

## Alternatives considered

- **Keep the spec's five tabs.** Rejected by the owner, because it dilutes the MVP's focus.
- **Four tabs, with More as a tab.** Spends a tab slot on low-frequency content. Rejected.
