# Architecture decision records

Each significant or hard-to-reverse decision gets one short record: what was decided, why, and what it costs. An accepted ADR is never edited in substance. To change a decision, write a new ADR that supersedes the old one and update the old one's status.

NFR-MNT-03 requires an ADR for every technology choice in spec §8.3. The choices not yet covered here are tracked as LB-014.

## Index

| ADR | Title | Status | Date |
| --- | --- | --- | --- |
| [0001](0001-native-ui-on-shared-kmp-core.md) | Native UI per platform on a shared KMP core | Accepted | 2026-10-22 |
| [0002](0002-three-section-navigation.md) | Three-section navigation: Lines, Near Me, How to Get There | Accepted | 2026-10-22 |
| [0003](0003-ios-deployment-target-and-liquid-glass.md) | iOS deployment target and Liquid Glass adoption | Proposed | 2026-10-22 |
| [0004](0004-swift-interop-umbrella-framework-and-skie.md) | Swift interop: umbrella framework and SKIE | Proposed | 2026-10-22 |
| [0005](0005-android-ui-stack.md) | Android UI stack: Material 3 Expressive, Navigation 3, adaptive navigation | Proposed | 2026-10-22 |

Status values: `Proposed`, `Accepted`, `Superseded by ADR-nnnn`, `Deprecated`.

## Template

Copy this into `NNNN-kebab-case-title.md`:

```markdown
# ADR-NNNN: Title

| Field | Value |
| --- | --- |
| Status | Proposed |
| Date | YYYY-MM-DD |
| Deciders | … |
| Related | FR-…, NFR-…, OQ-…, ADR-… |

## Context
What forces are at play, and what problem needs a decision?

## Decision
What we will do, stated in one or two paragraphs.

## Consequences
What becomes easier, what becomes harder, and what follow-up work is needed.

## Alternatives considered
Each rejected option, with the reason it was rejected.
```
