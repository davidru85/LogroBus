# ADR-0003: iOS deployment target and Liquid Glass adoption

| Field | Value |
| --- | --- |
| Status | Accepted (owner confirmed on 2026-10-22, OQ-13) |
| Date | 2026-10-22 |
| Deciders | Project owner, tech lead |
| Related | Spec §2.5, OQ-13, ADR-0001, `design_guidelines.md` §5 |

## Context

- Spec §2.5 sets iOS 16 as the minimum. The brief asks for SwiftUI with Liquid Glass.
- Liquid Glass ships with iOS 26: system components adopt it automatically when built with the iOS 26 SDK, and the custom APIs (`glassEffect`, `GlassEffectContainer`, `.glass` button styles, `tabBarMinimizeBehavior`) are available only on iOS 26 and later.
- iOS 26 dropped the iPhone XS and XR. iOS 18 still supports them, the same hardware as iOS 17.
- Modern SwiftUI APIs the app wants are iOS 17 or 18 only: `@Observable`, the `Tab` type in `TabView`, `ContentUnavailableView` and the newer scroll APIs.

## Decision

- **Deployment target: iOS 18.0.** Build with Xcode 26 or later, against the iOS 26 SDK or newer.
- **On iOS 26 and later,** the app uses Liquid Glass fully: system components plus custom glass on the floating map controls and the floating search field.
- **On iOS 18 to 25,** the same UI renders with the classic system appearance. The custom glass surfaces fall back to `.regularMaterial` backgrounds with the same shapes.
- Feature code never calls the glass APIs directly. It uses the `DesignSystem/` helpers `adaptiveGlass(in:)`, `AdaptiveGlassButtonStyle` and `AdaptiveGlassProminentButtonStyle`, which contain the `if #available(iOS 26, *)` branches.

## Consequences

- The app reaches the same devices as an iOS 17 floor, with modern SwiftUI APIs.
- Custom glass work is wrapped in one place, so raising the floor to iOS 26 later means deleting the fallback branches.
- Screenshot tests and manual accessibility passes must cover both the iOS 26+ appearance and the fallback appearance.

## Alternatives considered

- **iOS 26 as the floor.** No fallback code and the purest Liquid Glass, but it excludes the iPhone XS and XR and users who have not upgraded. This remains a valid choice if the owner prefers simplicity over reach.
- **iOS 16, as in the spec.** Loses `@Observable` and the `Tab` API, and needs more back-deployment shims. Rejected.
