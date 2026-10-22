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
- **Navigation:** Jetpack Navigation 3. The app holds one back stack of `@Serializable` `NavKey`s per tab. Switching tabs keeps each tab's stack, and re-selecting a tab pops it to its root.
- **Adaptive layout:** `NavigationSuiteScaffold` shows a navigation bar on compact widths and a navigation rail on medium and expanded widths.
- **Maps:** MapLibre Native through `maplibre-compose`.

## Consequences

- Android looks current, and the API churn of experimental components is confined to one package.
- Navigation 3's app-owned back stacks map directly onto the per-tab requirement, and they are easy to test.
- Deep links need a small custom parser that builds the target tab's back stack. Navigation 3 has no deep-link DSL.

## Alternatives considered

- **Navigation Compose 2.x with type-safe routes.** Mature, but multiple back stacks need `saveState` and `restoreState` handling. Acceptable fallback.
- **Plain Material 3 without the expressive APIs.** Contradicts the brief.
- **Dynamic colour.** It could clash with the data-driven line colours and weaken brand recognition. Revisit after the MVP.
