# ADR-0005: Android UI stack: Material 3 Expressive, Navigation 3, adaptive navigation

| Field | Value |
| --- | --- |
| Status | Proposed |
| Date | 2026-10-22 |
| Deciders | Tech lead |
| Related | ADR-0001, ADR-0002, `design_guidelines.md` §4, `architecture.md` §5.1 |

## Context

The brief asks for Kotlin, Jetpack Compose and Material 3 Expressive. The app has three tabs, and each tab needs its own back stack (ADR-0002). Some expressive components (button groups, floating toolbars, loading indicators, expressive motion and typography) are still marked experimental in Compose Material 3.

## Decision

- **Theme:** `MaterialExpressiveTheme` with a fixed brand colour scheme for light and dark, `MotionScheme.expressive()`, expressive typography and the shape scale. Dynamic colour is not used in v1.
- **Experimental APIs:** `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` appears only in the wrappers of the `designsystem` package. Feature code uses the wrappers. The Compose Material 3 version is pinned in the version catalog and upgraded deliberately.
  - *Update, 2026-10-28 (LB-011):* the pin is 1.5.0-beta02, over the Compose BOM's 1.4.0, in which the expressive components and emphasized type are internal. It moves to 1.5.0 when that is stable. An architecture test keeps the opt-in inside the design system.
- **Navigation:** Jetpack Navigation 3. The app holds one back stack of `@Serializable` `NavKey`s per tab. Switching tabs keeps each tab's stack, and re-selecting a tab pops it to its root.
  - *Update, 2026-10-30 (LB-012):* Navigation 3 1.2.0, with `lifecycle-viewmodel-navigation3` at the lifecycle version. Each tab decorates its entries with its own saveable-state and ViewModel decorators, and each content key carries the tab, so a screen on two stacks never shares state. `NavDisplay` shows the start tab's entries under the selected tab's, so back from another tab's root returns to Lines with the predictive animation. Screens cross-fade on the motion scheme's effects spring rather than navigation3-ui's 700 ms fade. `rememberNavBackStack` uses Android's reflective serializer; a configured `SerializersModule` is needed only if navigation moves to shared code.
- **Adaptive layout:** `NavigationSuiteScaffold` shows a navigation bar on compact widths and a navigation rail on medium and expanded widths.
  - *Update, 2026-10-30 (LB-012):* `material3-adaptive-navigation-suite` shares Material 3's 1.5.0-beta02 pin. The shell chooses by width alone, as FR-NAV-05 says: a short bar on compact widths and a collapsed wide rail from the medium width up. Material's recommended type would keep a bar on a short window, such as a phone in landscape, or on a tabletop posture.
- **Maps:** MapLibre Native through `maplibre-compose`.

## Consequences

- Android looks current, and the API churn of experimental components is confined to one package.
- Navigation 3's app-owned back stacks map directly onto the per-tab requirement, and they are easy to test.
- Deep links need a small custom parser that builds the target tab's back stack. Navigation 3 has no deep-link DSL.

## Alternatives considered

- **Navigation Compose 2.x with type-safe routes.** Mature, but multiple back stacks need `saveState` and `restoreState` handling. Acceptable fallback.
- **Plain Material 3 without the expressive APIs.** Contradicts the brief.
- **Dynamic colour.** It could clash with the data-driven line colours and weaken brand recognition. Revisit after the MVP.
