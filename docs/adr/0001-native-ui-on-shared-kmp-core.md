# ADR-0001: Native UI per platform on a shared KMP core

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-10-22 |
| Deciders | Project owner |
| Related | OQ-01, spec §2.5, §8.1, §8.2 |

## Context

The spec left OQ-01 open: native SwiftUI on iOS, or Compose Multiplatform for both platforms? The project owner's brief asks for Jetpack Compose with Material 3 Expressive on Android and SwiftUI with Liquid Glass on iOS. Both design languages are tightly bound to their platform toolkits. Liquid Glass in particular comes from SwiftUI and UIKit system components and APIs (`glassEffect`, glass button styles, system tab bars and sheets), which Compose Multiplatform cannot reproduce faithfully.

## Decision

- Share all non-UI code in Kotlin Multiplatform: domain, data, networking, persistence, normalisation, polling, and presentation state (ViewModels and `UiState`).
- Build the UI natively on each platform: Jetpack Compose with Material 3 Expressive on Android, and SwiftUI with Liquid Glass on iOS.
- Use Compose Multiplatform nowhere in v1.

## Consequences

- Each platform gets first-class access to its design language, accessibility features and system components.
- There are two UI codebases. Every screen is built twice, which is why ViewModels and display models are shared to keep the behaviour identical.
- iOS needs a Swift interop layer for the shared ViewModels (ADR-0004).
- Shared presentation emits semantic display models, and each platform localises them (`AGENTS.md` §5 rule 4).

## Alternatives considered

- **Compose Multiplatform for both.** One UI codebase, but iOS would look non-native and Liquid Glass would at best be approximated. Rejected, because it contradicts the brief.
- **Fully native apps with no KMP.** The data rules (18 data-defect rules, ETA logic, direction inference) would be duplicated, with a high risk of the platforms diverging. Rejected.
