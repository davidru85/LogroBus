# Design guidelines

| Field | Value |
| --- | --- |
| Status | Draft |
| Last updated | 2026-10-22 |
| Based on | Spec §7; `requirements.md` (FR-NAV, FR-LINE-12 to 16, FR-NEAR, FR-PLAN-11 to 14); ADR-0002, ADR-0003, ADR-0005 |

**Figma designs for the MVP will be delivered near the end of the MVP (LB-413).** Until then, this document's wireframes and the neutral placeholder theme are the reference. When the Figma file arrives, it becomes the visual source of truth and is linked here.

This document covers how LogroBus looks and behaves: the three sections screen by screen, the platform design languages (Material 3 Expressive on Android, Liquid Glass on iOS), the visual language shared by both, UX states, accessibility and copy. Each platform should feel fully native, while the information and behaviour stay identical.

## 1. Principles

1. **"When is my bus?" comes first.** Live arrivals for a saved or nearby stop are one tap from launch (G1).
2. **Honest predictions.** Every ETA shows how fresh and how confident it is. Never present a guess as certain (G2, DD-09).
3. **Colour is never the only signal.** A line is always its code plus its colour, because two pairs of lines share colours (DD-08).
4. **Every map has a list.** Anything done on a map can also be done in a list (NFR-A11Y-04).
5. **Native first.** Use each platform's system components and idioms before building custom ones.

## 2. Information architecture

### 2.1 Tabs

| # | Section | Label es / en | Android icon (Material Symbols) | iOS icon (SF Symbols) |
| --- | --- | --- | --- | --- |
| 1 | Lines | Líneas / Lines | `directions_bus` | `bus` |
| 2 | Near Me | Cerca de mí / Near me | `near_me` | `location.circle` |
| 3 | How to Get There | Cómo llegar / Directions | `directions` | `arrow.triangle.turn.up.right.diamond` |

- The English label for tab 3 is "Directions", because the tab bar needs a short label; the screen title reads "How to get there". This copy is a proposal.
- The app always opens on Lines (OQ-15). Deep links are the only exception: they open their target tab. Saved-stop ETAs stay one tap away in Near Me, which meets G1.
- Each tab keeps its own back stack. Re-selecting the active tab pops it to its root, or scrolls to the top if it is already at the root.
- A toolbar action on each tab root opens **More**: Settings and About data in the MVP, plus News, Fares, Recharge points and Info in v1.0. Use a settings-style icon (`settings` / `gearshape`).

### 2.2 Where the spec screens went (ADR-0002)

| Spec screen | New home |
| --- | --- |
| SC-01 Home | Dissolved. Favourite lines → Lines carousel. Favourite stops and nearby stops → the Near Me sheet. Notice banner → top of Lines (v1.0). Featured news → More → News (v1.0) |
| SC-02 Global search | Split. Lines search → Lines tab. Stop search → the Near Me search field. Address search → How to Get There |
| SC-03, SC-04 Lines, Line detail | Lines tab |
| SC-05 Stop detail | The Near Me sheet, and a pushed screen from Lines and itineraries (same component) |
| SC-06 Map | Near Me tab (stops; buses and routes as optional layers; recharge points layer in v1.0) |
| SC-07 Vehicle sheet | Sheet over the Line detail map and the Near Me map |
| SC-08 to SC-10 Plan, itinerary list and detail | How to Get There tab |
| SC-11 to SC-14, SC-17 News, Fares, Recharge points, Info | More (v1.0) |
| SC-15 Manage favourites | Inline: reorder favourite lines from the carousel's "Edit"; manage saved stops from the Near Me sheet's "Edit" |
| SC-16 Settings | More → Settings (MVP) |
| SC-18 Onboarding | Before the first tab: 3 cards (value, favourites, location rationale) |

## 3. The three sections

### 3.1 Lines

```
┌─────────────────────────────────────┐
│ Líneas                          ⚙   │  large title / top app bar
│ ┌─────────────────────────────────┐ │
│ │ 🔍 Buscar línea o parada        │ │  search (FR-LINE-03)
│ └─────────────────────────────────┘ │
│ ⚠ Desvío en línea 3 · Ver aviso     │  notice banner (v1.0, FR-NEWS-06)
│                                     │
│ Tus líneas favoritas        Editar  │
│ ┌──────┐ ┌──────┐ ┌──────┐ ┌───     │  carousel (horizontal)
│ │ [3]  │ │ [10] │ │ [B1] │ │        │
│ │Campi…│ │El Ar…│ │Varea…│ │        │
│ └──────┘ └──────┘ └──────┘ └───     │
│                                     │
│ Líneas diurnas                      │  FR-LINE-01
│ [1]  Lardero - Hospital San Pedro ☆ │
│ [2]  Yagüe - Varea                ★ │
│ …                                   │
│ Líneas nocturnas (Búho)             │
│ [B1] Varea - La Cava              ☆ │
└─────────────────────────────────────┘
```

**Favourite lines carousel**
- A card shows the line badge, the display name in 2 lines (ellipsised), and both headsigns ("→ Hospital San Pedro · → Lardero") in secondary text. Tapping a card opens Line detail.
- Order: the user's manual order (FR-FAV-02), adjusted through "Edit" (drag to reorder, remove).
- Empty state: one compact hint card, "Marca ☆ en una línea para tenerla aquí" ("Tap ☆ on a line to pin it here"). The section never disappears entirely, so the feature stays discoverable.
- With more than 20 favourites, adding another shows the limit message (FR-FAV-05).

**Full list**
- Two groups, Day lines then Night lines (Búho), each sorted numerically by code: 1, 2, 3 … 11, then B1, B2, B3. The list is data-driven; there is no line 8.
- A row shows the badge, the display name, a status chip when the line is not operational (FR-LINE-02), and a trailing favourite toggle with an accessible label ("Añadir línea 3 a favoritas").
- Search filters by code, terminus or any stop name, ignoring accents and case. Results keep the grouping, and the carousel hides while a query is active.

**Line detail**

```
┌─────────────────────────────────────┐
│ ←  [10] El Arco - Hospital S. P.  ☆ │
│ ┌─────────────────────────────────┐ │
│ │  map: both shapes, selected     │ │  collapsible (FR-LINE-04)
│ │  direction emphasised, live 🚌  │ │  FR-LINE-06
│ └─────────────────────────────────┘ │
│ ( Hacia Artesanos | Hacia M. de Falla ) │  direction toggle with headsigns
│ ● Manuel de Falla              4 min│  stop timeline + next ETA (FR-LINE-05)
│ │                                   │
│ ● Avenida de Burgos           ~7 min│  "~" = low confidence (DD-09)
│ ● Provisional Templarios [Provisional]  –  │  FR-LINE-11
└─────────────────────────────────────┘
```

- Tapping a stop opens Stop detail, and tapping a bus opens the Vehicle sheet (FR-LINE-08).
- Vehicles whose direction is unknown show a neutral marker in both direction views.

### 3.2 Near Me

```
┌─────────────────────────────────────┐
│ ┌────────────────────────┐   ⚙      │  floating search (glass / M3 search bar)
│ │ 🔍 Buscar parada       │          │
│ └────────────────────────┘          │
│             MAP                     │
│        ●        ◉ you               │  stop pins, clustered below z15
│    ●      ●                    [◎]  │  floating controls: recenter,
│                                [≋]  │  layers (Paradas, Autobuses, Rutas)
│ ╭─────────────────────────────────╮ │
│ │ ───                             │ │  bottom sheet, peek detent
│ │ Mis paradas                     │ │  saved stops + next 3 arrivals each
│ │  Gran Vía 12  [1] 3' [10] 8'    │ │  (FR-HOME-02, adapted)
│ │ Cerca de ti                     │ │  5 closest within 800 m
│ │  Avenida de Portugal · 210 m    │ │  (FR-HOME-03, FR-STOP-11)
│ ╰─────────────────────────────────╯ │
└─────────────────────────────────────┘
```

**Sheet behaviour**

| Detent | Content |
| --- | --- |
| Peek (about 25 % of the screen) | Section headers, plus the first saved stop with its next arrivals |
| Medium (about 50 %) | Saved stops and nearby stops lists, or the selected stop's arrivals |
| Large | The full list, or full stop detail with filter chips and actions |

- The sheet is always present and cannot be dismissed, and the map behind it stays interactive at the peek and medium detents.
- Tapping a pin selects the stop: the pin grows (spec §7.4) and the sheet moves to medium, showing stop detail. Closing the stop returns the sheet to the lists.
- **Stop detail** in the sheet shows:
  - The name, with headsigns to tell apart stops that share it (DD-16), and the served line badges.
  - The arrivals list (spec §5.7) with "Actualizado hace N s" ("Updated N s ago").
  - Line filter chips and a "Group by line" toggle (FR-STOP-04, FR-STOP-07).
  - Actions: Save stop with an alias (FR-STOP-08) and Directions to this stop (FR-STOP-09), which switches to How to Get There with the destination set.
- **Without location permission**, the map shows the whole service area. The "Cerca de ti" section becomes one row, "Activa la ubicación para ver paradas cercanas" ("Turn on location to see nearby stops"), which opens the permission flow. Saved stops and pins keep working (FR-LOC-02).
- **No saved stops:** "Guarda una parada para ver aquí sus próximos autobuses" ("Save a stop to see its next buses here"), with a "Buscar parada" ("Find a stop") action (FR-HOME-07, adapted).
- **Layers:** Stops (always on), Buses (live vehicles, FR-LIVE-01), Routes ("Show all routes", FR-LIVE-05), and in v1.0 Recharge points. A layer's state persists across sessions.

### 3.3 How to Get There

```
┌─────────────────────────────────────┐
│ Cómo llegar                     ⚙   │
│ ┌─────────────────────────────┐ ⇅   │  swap (FR-PLAN-08)
│ │ ◉ Mi ubicación              │     │  origin
│ │ ◎ ¿A dónde vas?             │     │  destination
│ └─────────────────────────────┘     │
│ ( Salir ahora ▾ )                   │  Leave now / Depart at (FR-PLAN-03)
│ Casa   Trabajo   + Lugar            │  saved places chips
│ Recientes                           │  last 10 (FR-PLAN-09)
│  Gran Vía → Hospital San Pedro      │
└─────────────────────────────────────┘

Results                                   Itinerary detail
┌─────────────────────────────────┐       ┌─────────────────────────────────┐
│ 08:22 → 08:41 · 19 min          │       │ map: walk dashed, bus in line   │
│ 🚶 4' › [10] › 🚶 3'  · 450 m a pie│       │ colour (FR-PLAN-05)             │
│ Sale en 3 min desde Gran Vía    │       ├─────────────────────────────────┤
├─────────────────────────────────┤       │ 🚶 Camina 4 min (300 m) a Gran Vía│
│ 08:25 → 08:44 · 19 min          │       │ [10] Sube hacia Artesanos       │
│ 🚶 6' › [3] › [1] › 🚶 2'          │       │      08:22 · en directo 08:24 ~ │  FR-PLAN-06
└─────────────────────────────────┘       │      7 paradas ▾                │
                                          │ ⬇ Bájate en Hospital San Pedro  │
                                          │ 🚶 Camina 3 min hasta el destino │
                                          └─────────────────────────────────┘
```

- Origin and destination accept: current location, a saved place, a stop, a point picked by long-pressing the map, or an address (FR-PLAN-01). Origin defaults to "Mi ubicación" ("My location") when permission is granted.
- Place search is one list, in this order: saved places, then stops, then addresses (CartoCiudad, at least 3 characters, debounced 300 ms, at most 8 results).
- A results card shows the departure and arrival times, total duration, line badges in leg order with walk segments between them, and the total walking distance. Cards are sorted by arrival time (FR-PLAN-04).
- Itinerary detail is a map with a step list. Steps with intermediate stops expand. Planned times are shown alongside the live ETA of the first bus leg when it departs within 60 min.
- When there are no routes, show the message mapped from `rejection.reason`, including its criteria values (FR-PLAN-07), plus a "Cambiar hora" ("Change time") action.
- A point outside the service area triggers the inline error "Esta ubicación está fuera del área de servicio" ("This location is outside the service area") before any request is sent (FR-PLAN-10).

## 4. Android: Material 3 Expressive

**Theme**
- Wrap the app in `MaterialExpressiveTheme` with a fixed brand `ColorScheme` for light and dark, `MotionScheme.expressive()`, the expressive typography and the shape scale.
- **Until OQ-18 is decided, the brand palette is a neutral placeholder** (greys plus one accessible accent). It is defined as tokens in one place (NFR-MNT-04) and replaced by the Figma designs in LB-413, near the end of the MVP.
- Dynamic colour is not used in v1, so brand surfaces stay predictable next to the data-driven line colours.
- Expressive APIs are experimental. Opt in only inside `designsystem/` wrappers, such as `LbButtonGroup` and `LbLoadingIndicator`.

| Need | Component |
| --- | --- |
| Top-level navigation | `NavigationSuiteScaffold`: a short navigation bar on phones, a navigation rail on tablets and foldables |
| Tab roots | `LargeFlexibleTopAppBar` / `TopAppBar` collapsing on scroll, with the More action |
| Lines search | `SearchBar` with `rememberSearchBarState()`; it expands to full screen on focus |
| Favourite lines | `HorizontalMultiBrowseCarousel` (or `HorizontalUncontainedCarousel` for fixed-width cards) |
| Direction toggle, layer switcher | Connected `ButtonGroup` with `ToggleButton`s |
| Map controls | `HorizontalFloatingToolbar` / `VerticalFloatingToolbar` over the map; a medium FAB for "recenter" |
| Near Me sheet | `BottomSheetScaffold` with a partially expanded peek and `skipHiddenState = true` |
| Refreshing | `LoadingIndicator` inside `PullToRefreshBox`, or a thin `LinearWavyProgressIndicator` under the app bar |
| First load | Skeletons shaped like the final layout (spec §7.3), never a lone spinner |
| Filter chips | `FilterChip` rows; line chips carry the `LineBadge` as the leading icon |
| Itinerary steps | `ListItem`s with a custom leg rail; expandable intermediate stops with expressive motion |

- **Motion:** spatial transitions (sheet detents, carousel, direction toggle) use the expressive spatial springs, and colour or opacity changes use the effects springs. When system animations are off ("Remove animations"), use the reduced-motion fallbacks.
- **Shape:** cards use 12 dp corners (spec §7.5). Expressive shape morphing is fine for buttons and FABs. **Line badges never morph**; their 6 dp rectangle is an identity element.
- **Typography:** ETAs use `titleMediumEmphasized` with tabular figures (`fontFeatureSettings = "tnum"`). Display styles are reserved for section titles.

## 5. iOS: Liquid Glass

Liquid Glass is the navigation and control layer that floats above content. Content itself (lists, cards, ETAs, line badges, the map) stays solid and legible underneath.

**System first.** These pick up Liquid Glass automatically when built with the iOS 26 SDK and run on iOS 26 or later:

| Need | Component |
| --- | --- |
| Top-level navigation | `TabView` with `Tab(…)`, and `.tabBarMinimizeBehavior(.onScrollDown)` on scrolling tabs (iOS 26) |
| Tab roots | `NavigationStack` with large titles; toolbar items for More |
| Lines search | `.searchable(text:placement:)` on the Lines stack |
| Near Me sheet | `.sheet` with `.presentationDetents([.height(peek), .medium, .large])`, `.presentationBackgroundInteraction(.enabled(upThrough: .medium))` and `.interactiveDismissDisabled()`. Don't set `.presentationBackground`, so the system glass stays |
| Direction toggle | `Picker(.segmented)` |
| Favourite lines | `ScrollView(.horizontal)` + `LazyHStack` with `.scrollTargetBehavior(.viewAligned)` |
| Refreshing | `.refreshable`; skeletons via `.redacted(reason: .placeholder)` for the first load |

The MVP is **iPhone only** (NFR-PLAT-03): no iPad layouts.

**Custom glass** is used only on floating map controls and the floating search field:
- Recenter and layers buttons use `.buttonStyle(.glass)`. The single primary action, "Buscar rutas" ("Find routes") in the planner, uses `.buttonStyle(.glassProminent)` with the brand tint (a placeholder accent until OQ-18).
- Neighbouring floating controls sit in a `GlassEffectContainer(spacing:)` so their shapes blend. Use `glassEffectID(_:in:)` to morph between related states, for example the layer button expanding into the layer menu.
- Never stack glass on glass, never put glass on list rows, cards or line badges, and never tint more than one element per screen.
- Feature code calls only the `DesignSystem/` helpers:
  - `.adaptiveGlass(in:)` applies `.glassEffect(.regular.interactive(), in:)` on iOS 26 and later, and a `.regularMaterial` background with the same shape on iOS 18–25 (ADR-0003).
  - `AdaptiveGlassButtonStyle` and `AdaptiveGlassProminentButtonStyle` wrap `.glass` and `.glassProminent` the same way.
- **Accessibility:** glass must stay legible with Reduce Transparency, Increase Contrast and both appearances. Verify every custom glass surface in these modes before calling a story done.

## 6. Shared visual language

### 6.1 Tokens (spec §7.5 plus additions)

| Token | Value |
| --- | --- |
| Corner radius, cards | 12 dp / pt |
| Corner radius, sheets | Platform default |
| Spacing scale | 4, 8, 12, 16, 24, 32 |
| Screen margins | 16 |
| Minimum touch target | 48 × 48 dp (Android), 44 × 44 pt (iOS) |
| Line badge | At least 32 × 24, radius 6, bold label, fill = line colour, text = `onColor` (≥ 4.5:1) |
| ETA text | Title medium, emphasized or semibold, tabular figures |
| Secondary ETA text (delay, early, scheduled) | Body small, secondary colour |
| Freshness, stale | Warning colour, "Actualizado hace N s" ("Updated N s ago") |

### 6.2 Line badge

`LineBadge(code, color, onColor, size = Regular | Small | Large)` is the only way a line is shown, in lists, chips, map markers, itinerary legs and notifications. Its accessibility label is "Línea 3" / "Line B1", never the colour name.

### 6.3 ETA presentation (spec §5.7)

| Case | Visual | Spoken (en) |
| --- | --- | --- |
| Under 60 s | "Ahora" / "Now" with a subtle pulse (static when reduced motion is on) | "Line 2, arriving now" |
| 1 to 59 min | "N min" | "Line 2, in N minutes" |
| 60 min or more | "HH:mm" (Europe/Madrid) | "Line 2, at HH mm" |
| Low confidence | "~" prefix + info icon | "approximately …" |
| Scheduled only | Clock time in the secondary colour + "Programado" ("Scheduled") | "scheduled at HH mm" |
| Delay ≥ 120 s / early ≥ 60 s | Secondary "+N min tarde" / "N min antes" ("N min late" / "N min early") | "N minutes late" / "N minutes early" |
| Cancelled | Row struck through + "Cancelado" ("Cancelled") | "Line 2, cancelled" |
| Expired data (over 120 s) | All ETAs as clock times + "Toca para actualizar" ("Tap to refresh") | none |

Settings → ETA format switches between minutes and clock time (FR-SET-01).

### 6.4 Map styling (spec §7.4)

- Initial camera: the user at zoom 16 if they are inside the service area, otherwise the service bounding box with 32 padding.
- Stops: a 12 white circle with a dark-neutral border. A selected stop is 20, with a badges callout. Clustered below z15; individual markers with badges above z16.
- Vehicles: a rounded rectangle in the line colour with the code in `onColor`, plus a heading chevron. Vehicles with an unknown line are grey and show no code.
- Shapes: a 5 stroke in the line colour with a 2 white casing. The non-selected direction is drawn at 40 % opacity. Walking legs are dashed and neutral.
- A dark base style is used in dark mode. OSM attribution and "Datos: Ayuntamiento de Logroño" ("Data: Logroño City Council") are always visible (NFR-PRIV-05).

## 7. UX states (spec §7.3)

| State | Android | iOS |
| --- | --- | --- |
| Loading (first) | Skeleton shapes | `.redacted(reason: .placeholder)` on sample content |
| Refreshing | `LoadingIndicator` / wavy linear indicator; content stays interactive | `.refreshable` spinner, or a toolbar `ProgressView` |
| Stale | "Actualizado hace N s" in the warning colour | Same |
| Expired | Clock-time ETAs + "Toca para actualizar" | Same |
| Empty | One sentence + one action, no illustration | `ContentUnavailableView` with one action |
| Offline (v1.0 only, ADR-0008; in the MVP a missing connection is the Error state with reason "network") | Non-blocking banner "Sin conexión. Mostrando datos guardados." ("You are offline. Showing saved data.") | Same, as a top inset banner |
| Error | Message, reason (network / server / data), "Reintentar" ("Retry") | `ContentUnavailableView` with a "Reintentar" button |
| Partial | Content shown; logged silently | Same |

## 8. Accessibility checklist (NFR-A11Y)

- [ ] Every ETA and line badge has the spoken label from §6.3.
- [ ] Live updates do not move focus, and polite announcements come at most once per 30 s for the focused item.
- [ ] Text at 200 % does not truncate ETAs or line codes; rows reflow vertically.
- [ ] Every map task has a list path: nearby stops in the sheet, stops on a line in the line detail list, itinerary steps.
- [ ] Touch targets meet the minimum size; the favourite toggles are not smaller than the target.
- [ ] Custom glass surfaces have been checked with Reduce Transparency and Increase Contrast; Android has been checked with high-contrast text.
- [ ] TalkBack and VoiceOver order follows the visual order; map pins have labels ("Parada Gran Vía 12, líneas 1 y 10").

## 9. Copy and localisation

- Spanish (es-ES) is the source language and English ships in v1. The tone is short, direct and friendly, using "tú" in Spanish.
- Line names, stop names and CMS content are shown as delivered (after overrides) and never translated.
- Dates, times and prices follow the device locale. Times always use `Europe/Madrid`.

| Key | es | en |
| --- | --- | --- |
| `tab.lines` | Líneas | Lines |
| `tab.near_me` | Cerca de mí | Near me |
| `tab.directions` | Cómo llegar | Directions |
| `lines.favourites.title` | Tus líneas favoritas | Your favourite lines |
| `lines.group.day` | Líneas diurnas | Day lines |
| `lines.group.night` | Líneas nocturnas (Búho) | Night lines (Búho) |
| `nearme.saved.title` | Mis paradas | My stops |
| `nearme.nearby.title` | Cerca de ti | Near you |
| `plan.title` | Cómo llegar | How to get there |
| `plan.origin.current` | Mi ubicación | My location |
| `plan.destination.hint` | ¿A dónde vas? | Where to? |
| `freshness.updated_ago` | Actualizado hace %d s | Updated %d s ago |
| `offline.banner` (v1.0) | Sin conexión. Mostrando datos guardados. | You are offline. Showing saved data. |
