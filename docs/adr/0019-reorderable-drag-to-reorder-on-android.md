# ADR-0019: Reorderable for drag to reorder on Android

| Field | Value |
| --- | --- |
| Status | Accepted (2026-11-03) |
| Date | 2026-11-03 |
| Deciders | Project owner (2026-11-03, when LB-110 started). Written up by the agent for LB-110 |
| Related | FR-FAV-02, NFR-A11Y, LB-110, LB-208, ADR-0005, `design_guidelines.md` §3.1, `engineering_workflow.md` §9 |

## Context

FR-FAV-02 asks for favourites to be reordered by drag and drop. LB-110 does it for the favourite lines, in an edit sheet with a vertical list (owner decision, 2026-11-03), and LB-208 will do it for saved stops.

- **iOS** has it built in: a `List` in edit mode with `.onMove`.
- **Compose has no reorderable list.** A drag needs the dragged item to follow the finger, the others to make room, the list to scroll when the finger nears its edge, and the move to survive the list recycling items. Writing that on `LazyListState` and the pointer APIs is several hundred lines with subtle edge cases.

## Decision

- **Reorderable** (`sh.calvin.reorderable:reorderable`, 3.1.0, Apache-2.0), in `:android:feature:lines`, the only module that reorders so far. Its version lives in `gradle/libs.versions.toml`.
- A drag starts from a handle at the end of each row (`draggableHandle`), so a drag never fights the sheet's own swipe or a scroll.
- **The order is saved when the drag ends**, not at every step, so the database sees one write per drag.
- **Screen readers never need the drag** (NFR-A11Y): each row also has "Move up" and "Move down" accessibility actions, which are ours, not the library's.

## Consequences

- **Small and focused.** It is a Compose-only library, with no network access and no telemetry. It brings the Compose artifacts it builds on, which the Compose BOM resolves to the versions the app already uses.
- **Moving with Compose.** It is built against an older Compose than the app's. A Compose upgrade that breaks it would show in the edit sheet's interaction tests, and the fix is an upgrade of the library or a pin.
- **Licence:** Apache-2.0 is on the allow-list (`engineering_workflow.md` §9).

## Alternatives considered

- **Our own drag on `LazyListState`.** Rejected: more code to own and test (auto-scroll, cancelled drags, recycled items) for no gain in behaviour.
- **"Move up" and "Move down" buttons only.** Rejected: FR-FAV-02 asks for drag and drop. The actions stay, for screen readers.
- **Long press anywhere on the row.** Rejected: a handle shows that the row moves, and leaves a tap and a long press on the row free.
