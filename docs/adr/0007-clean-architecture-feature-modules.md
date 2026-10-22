# ADR-0007: Clean architecture with feature modules and platform-specific presentation

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-10-22 |
| Deciders | Project owner |
| Related | ADR-0001 (presentation sharing superseded), ADR-0004 (ViewModel bridge superseded), `architecture.md` |

## Context

ADR-0001 shared presentation *state machines* (ViewModels) in Kotlin Multiplatform, and ADR-0004 bridged them into SwiftUI. The owner has decided on a different split:
- a clean-architecture approach;
- modularisation by **feature**;
- a **platform-specific presentation layer** (Android and iOS);
- **presentation-layer models** shared through KMP.

## Decision

1. **Clean architecture.** Dependencies point inwards: presentation → domain ← data. The domain has no framework, I/O or platform dependencies.
2. **Feature modules.** Each user-facing feature is a module on each side:
   - shared: `:shared:feature:<name>`;
   - Android: `:android:feature:<name>`;
   - iOS: a `Feature<Name>` Swift package target.
   The features are `lines`, `stops`, `nearme`, `planner` and `settings`. **Feature modules never depend on each other.** Cross-feature navigation is wired by the app shell on each platform.
3. **Shared business capabilities.** Capabilities used by several features live in their own modules, split into domain and data so Gradle enforces the direction:
   - `:shared:transit:domain` and `:shared:transit:data` (lines, stops, arrivals, vehicles);
   - `:shared:favourites:domain` and `:shared:favourites:data` (favourites, saved places, recents).
   Technical concerns live in `:shared:core:*`.
4. **Layering inside a shared feature module.** Packages `domain` (use cases), `data` (feature-only data sources, if any) and `presentation` (models). Konsist architecture tests enforce the boundaries (`:tools:architecture-tests`).
5. **Shared presentation models.** KMP provides per feature:
   - the `UiState` types, UI models (plain types only), event and effect types;
   - **pure, stateless mappers** that turn domain results into UI models;
   - `EtaDisplay`, freshness and error-reason mapping, so spec §5.7 behaves identically on both platforms.
6. **Platform-specific presentation logic:**
   - Android ViewModels (`androidx.lifecycle.ViewModel`) live in `:android:feature:<name>`.
   - iOS ViewModels (`@MainActor @Observable` Swift classes) live in `Feature<Name>`.
   Both call shared use cases, apply the shared mappers, own lifecycle and subscriptions, handle events and drive navigation.

## Consequences

- Each platform has idiomatic, independently testable ViewModels. iOS no longer wraps Kotlin ViewModels.
- Orchestration (which use case to call on which event) is written twice. To keep it thin:
  - use cases return presentation-ready domain results;
  - mappers carry the formatting rules;
  - both platforms' ViewModels are tested against the same acceptance criteria.
- More Gradle modules mean longer KMP configuration and builds. Splitting each shared feature into separate domain, data and presentation modules was rejected for this reason; package boundaries plus Konsist are used instead.
- iOS feature targets consume the Kotlin framework as an XCFramework binary target of a local Swift package. This needs validating in LB-001; the fallback is feature folders inside the app target.
- ADR-0001's clause "presentation state (ViewModels and `UiState`) is shared" is superseded. ADR-0004 keeps the umbrella framework and SKIE, but drops the ViewModel bridge.

## Alternatives considered

- **Shared ViewModels (ADR-0001 and ADR-0004 as written).** Less duplication, but it contradicts the owner's decision on a platform-specific presentation layer.
- **Layer modules without feature modules.** Simpler, but features would not be isolated.
- **Three modules per shared feature (domain, data, presentation).** Strongest enforcement, but too many KMP modules for the build-time budget.
