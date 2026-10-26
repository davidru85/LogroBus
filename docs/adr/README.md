# Architecture decision records

Each significant or hard-to-reverse decision gets one short record: what was decided, why, and what it costs. An accepted ADR is never edited in substance. To change a decision, write a new ADR that supersedes the old one and update the old one's status.

NFR-MNT-03 requires an ADR for every technology choice in spec §8.3. ADR-0012 to ADR-0018 cover them (LB-014). The one still missing is the analytics and crash-reporting backend, which waits for OQ-16 (ADR-0009 already sets its vendor-neutral contracts).

## Index

| ADR | Title | Status | Date |
| --- | --- | --- | --- |
| [0001](0001-native-ui-on-shared-kmp-core.md) | Native UI per platform on a shared KMP core | Accepted (presentation clause superseded by 0007) | 2026-10-22 |
| [0002](0002-three-section-navigation.md) | Three-section navigation: Lines, Near Me, How to Get There | Accepted | 2026-10-22 |
| [0003](0003-ios-deployment-target-and-liquid-glass.md) | iOS deployment target and Liquid Glass adoption | Accepted | 2026-10-22 |
| [0004](0004-swift-interop-umbrella-framework-and-skie.md) | Swift interop: umbrella framework and SKIE | Proposed (amended by 0007 and 0010) | 2026-10-22 |
| [0005](0005-android-ui-stack.md) | Android UI stack: Material 3 Expressive, Navigation 3, adaptive navigation | Proposed | 2026-10-22 |
| [0006](0006-multi-agent-harness.md) | Agent-agnostic harness with thin per-agent adapters | Accepted | 2026-10-22 |
| [0007](0007-clean-architecture-feature-modules.md) | Clean architecture with feature modules and platform-specific presentation | Accepted | 2026-10-22 |
| [0008](0008-offline-mode-deferred.md) | Offline mode deferred to v1.0; cache-backed static data in the MVP | Accepted | 2026-10-22 |
| [0009](0009-telemetry-contracts.md) | Telemetry contracts now, vendor later | Accepted | 2026-10-22 |
| [0010](0010-ios-project-generation-and-framework-linking.md) | iOS project generation and framework linking | Accepted (amends ADR-0004's integration bullet) | 2026-10-23 |
| [0011](0011-static-analysis-tooling.md) | Static analysis tooling: ktlint CLI, detekt 2.0 alpha and Kover | Accepted | 2026-10-24 |
| [0012](0012-ktor-client-and-kotlinx-serialization.md) | Ktor client and kotlinx.serialization for networking | Proposed | 2026-10-26 |
| [0013](0013-sqldelight-local-database.md) | SQLDelight for the local database | Proposed | 2026-10-26 |
| [0014](0014-koin-dependency-injection.md) | Koin for dependency injection | Proposed | 2026-10-26 |
| [0015](0015-kotlinx-datetime-and-stdlib-time.md) | kotlin.time for instants, kotlinx-datetime for the Madrid time zone | Proposed | 2026-10-26 |
| [0016](0016-image-loading-coil-on-android.md) | Image loading with Coil 3 on Android and AsyncImage on iOS (refines spec §8.3) | Proposed | 2026-10-26 |
| [0017](0017-maplibre-native-maps.md) | MapLibre Native for maps on both platforms | Proposed | 2026-10-26 |
| [0018](0018-multiplatform-settings.md) | Multiplatform Settings for small persisted preferences | Proposed | 2026-10-26 |

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
