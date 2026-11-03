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
- **As built (LB-012):**
  - More, and the screens it opens, are pushed onto the selected tab's stack, with a back button, so each tab keeps its own.
  - On Android, back at the root of Near Me or How to Get There returns to Lines, and at the root of Lines it leaves the app, as Material recommends for a start destination. iOS has no back at a tab root.
  - Tab icons are outlined, and filled while selected (Android).
  - In a navigation bar, a tab label is one line; at large font sizes it shrinks to fit its tab, never below its size at 100 %, because the bar's height is fixed and a second line would draw over the next tab. In a rail, it keeps its size and wraps onto two lines.
  - Android shows the rail from the medium width up, whatever the height (FR-NAV-05), so a phone in landscape gets the rail.
  - A tab root's title is its tab label, except How to Get There's, "How to get there" (`plan.title`).
  - Screen titles are headings, for TalkBack's heading navigation as for VoiceOver's (`ScreenTitle` on Android).
  - Screens cross-fade on the motion scheme's effects spring (`ScreenTransitions` on Android).
  - Settings is a titled placeholder until LB-403. The tab roots are placeholders with their title and the More action until LB-101, LB-201 and LB-301.

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
| SC-15 Manage favourites | A modal sheet from the favourite lines carousel's "Edit" (LB-110, owner decision, 2026-11-03); saved stops from the Near Me sheet's "Edit" |
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
- A card shows the line badge, the display name in 2 lines (ellipsised), and both headsigns ("→ Hospital San Pedro · → Lardero") in secondary text, which wrap rather than clip. Screen readers read a card as one, the headsigns as "Hacia Hospital San Pedro, Hacia Lardero" (`lines.towards`). Tapping a card opens Line detail.
- Every card has the tallest card's height, so the row is never ragged at large text sizes. Cards are 168 dp wide on Android and 150 pt on iOS, where the inset list row clips its content: either way a phone shows two cards and the edge of a third, which shows that the row scrolls. They widen with the text size, up to 80 % of the row, so at 200 % a name keeps more than a word per line. No-break spaces keep each arrow with its headsign when the text wraps (LB-103).
- The hint shows the ☆ symbol, and screen readers read it as words: "Marca la estrella de una línea para tenerla aquí" (`lines.favourites.hint_spoken`).
- Order: the user's manual order (FR-FAV-02), adjusted through "Edit" (drag to reorder, remove).
- Empty state: one compact hint card, "Marca ☆ en una línea para tenerla aquí" ("Tap ☆ on a line to pin it here"). The section never disappears entirely, so the feature stays discoverable.
- With 20 favourites, adding another shows the limit message and saves nothing (FR-FAV-05).
  - **As built (LB-110):** "Editar" ("Edit") ends the section's heading while there is a favourite to edit, even one with no card; screen readers name it "Editar líneas favoritas", which keeps the word it shows. It opens a modal sheet, "Editar líneas favoritas", with "Listo" ("Done") (owner decision, 2026-11-03): one row per favourite line in the user's order, each with a remove control, the line's badge and name, and a handle to drag it by. A favourite whose line has left the catalogue has no card, and the sheet lists it in its place as "Línea que ya no circula" ("Line that no longer runs"), since the device keeps only its id; it counts towards the limit until it is removed. Once every favourite is removed, the sheet shows the carousel's hint rather than going blank.
  - The order is saved when a drag ends, and the carousel follows. A removed line goes through its toggle, so its row's star goes off. At large text a row puts its badge above its name, as the list's rows do.
  - Screen readers read a row as one, with its place as its state ("2 de 3"), and move it with "Subir" and "Bajar" ("Move up", "Move down") without dragging, hearing the new place after each move; the first row has no "Subir" and the last no "Bajar". On Android the drag handle says nothing, since the actions do its work.
  - Focus (§8): removing a line moves the focus to the next line's remove button, else the one before, else Done; on Android, closing the sheet gives the focus back to "Editar". The favourites heading keeps "Editar"'s height without it, so the list never moves when it comes or goes; on iOS "Editar" has the 44 pt touch target.
  - On Android the sheet is the design system's `ModalSheet`, the drag comes from Reorderable (ADR-0019), and a row's remove button is labelled as the list's toggle ("Quitar línea 10 de favoritas"). On iOS it is a system list in edit mode, whose delete and reorder controls are the system's.
  - The limit message, from a row's star or the line detail's: "Ya tienes 20 líneas favoritas, el máximo. Quita alguna para añadir otra." On Android a long snackbar with a close button, which TalkBack announces without moving its focus; on iOS an alert, with the first sentence as its title and the second as its message.

**Full list**
- Two groups, Day lines then Night lines (Búho), each sorted numerically by code: 1, 2, 3 … 11, then B1, B2, B3. The list is data-driven; there is no line 8.
- A row shows the badge, the display name, a status chip when the line is not operational (FR-LINE-02), and a trailing favourite toggle with an accessible label ("Añadir línea 3 a favoritas", "Quitar línea 3 de favoritas"). Tapping the row opens Line detail ("abrir línea" to TalkBack); the toggle is its own element, a star filled in the accent colour once the line is a favourite, at least 48 dp or 44 pt to touch. It is a button, not a checkbox: its label already says the state (LB-103).
- At large text sizes (Android from a 1.5 font scale, iOS at accessibility sizes) a row puts its badge above its name, so the name has the row's width beside the toggle and breaks between words.
- Search filters by code, terminus or any stop name, ignoring accents and case. Results keep the grouping, and the carousel hides while a query is active.
  - **As built (LB-102 part 1):** each word of the query must begin a word of the line's code, display name or headsigns, folded as the stop search folds them (`architecture.md` §4.3), and a code typed with a space or a dash ("B 1") still finds its line; stop names come with part 2, once the stops are cached, and the prompt says "Buscar línea" until then. Both platforms filter the list in place (owner decision, 2026-11-03): on Android the field is the list's first item, on iOS the system search field under the large title, always shown.
  - A search that finds nothing shows one sentence, "Ninguna línea coincide con tu búsqueda." (`lines.no_matches`), and one action, "Borrar búsqueda" (`search.clear`), which empties the field. Screen readers announce the sentence without moving their focus: on Android it is the title of a pane that appears, since a live region speaks only when an existing node changes; on iOS, an announcement. On iOS the state keeps the grouped list's background, so the screen keeps its colour between results and none.
  - Clearing the search, with the field's clear button or "Borrar búsqueda", leaves the focus in the field, since each button goes with the text. With text, the field keeps its prompt as its spoken name. On Android the list ends above the keyboard (`adjustResize` and the keyboard's insets), as the field heads the list and the window never pans.
  - Android shows the field only with lines to filter, and its first-load skeleton has the field's shape. On iOS the search field belongs to the navigation stack and shows in every state; a query typed during the first load filters the lines once they arrive.

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
- **Until LB-104 (LB-103, owner decision, 2026-11-02):** the detail shows the line's badge and whole name under the title "Línea 10" (`line.title`), with the favourite toggle in the toolbar (FR-LINE-09), the same toggle and labels as the rows'. The title is "Línea" (`line.title.generic`) until the line's code is known, so the screen is never untitled. Its first load draws the header's shape and is announced as "Cargando línea" (`line.loading`), and an error shows its reason and Retry.
  - A deliberate exception to §6.2: the bar names the line in words, and the badge is in the body. LB-104 designs the bar of the sketch above, with the badge in it.

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
- Expressive APIs are experimental. Opt in only inside `designsystem/` wrappers, such as `LbButtonGroup` and `LbLoadingIndicator`. An architecture test enforces this (`lb011_expressiveApisStayInTheDesignSystem`).
  - Compose Material 3 is pinned at 1.5.0-beta02, over the BOM's 1.4.0, whose expressive components and emphasized type are internal (ADR-0005). Move to 1.5.0 when it is stable.
  - Each wrapper is built with the first screen that needs it.
- **As built (LB-011 part 1):** `:android:core:designsystem` holds:
  - `Tokens`: the light and dark schemes, the type scale, the shapes, the spacing scale and the touch target. The placeholder palette is greys plus a muted blue accent (#2F5D8A, #A7C8EC in dark). A test checks that every text colour reaches 4.5:1 and that every surface is grey.
  - `LogroBusTheme`, over `MaterialExpressiveTheme`.
  - `LineBadge`.
  - `EtaText`.
  - Since LB-012:
    - `MoreActionButton` and `UpButton`: the More action of every tab root and the back action of a pushed screen, with their spoken labels ("Más", "Atrás") and 48 dp touch targets;
    - `ScreenTitle`, a top app bar's title as a heading;
    - `ScreenTransitions`, the cross-fade between screens.
  - Since LB-102: `SearchField`, a search field that filters what is under it as the user types: a `TextField` in the search bar's shape and colours, with Material Symbols' search icon, a 48 dp clear button once it has text ("Borrar búsqueda"), and the keyboard's search key, which closes the keyboard; and `SearchFieldPlaceholder`, its shape for a first load, as tall as the field at every text size.
  - Since LB-110: `ModalSheet`, a modal bottom sheet for a task over a screen, fully expanded, whose content gets a `close` for its own Done; it keeps Material's experimental sheet API inside the design system. And Material Symbols' "do_not_disturb_on" and "drag_handle" icons (`ic_remove`, `ic_drag_handle`).

| Need | Component |
| --- | --- |
| Top-level navigation | `NavigationSuiteScaffold`: a short navigation bar on phones, a navigation rail on tablets and foldables |
| Tab roots | `LargeFlexibleTopAppBar` / `TopAppBar` collapsing on scroll, with the More action |
| Lines search | The design system's `SearchField`, the list's first item, which filters the list in place (LB-102, owner decision, 2026-11-03). M3's collapsed `SearchBar` only opens an expanded search view (`ExpandedFullScreenSearchBar`), and disables the soft keyboard until it does |
| Favourite lines | `HorizontalUncontainedCarousel` of fixed-width cards (LB-103). The carousel centres its items vertically, so each card takes the tallest one's height, measured on copies hidden from TalkBack |
| Editing favourite lines | `ModalSheet` over a `LazyColumn` of `ListItem`s, each a Reorderable `ReorderableItem` dragged by its handle (ADR-0019), with "Move up" and "Move down" as `CustomAccessibilityAction`s (LB-110) |
| A one-off message | `Snackbar` in the screen's `SnackbarHost`, long, with a close button: the favourite limit (LB-110) |
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

**As built (LB-011 part 2):** `DesignSystem` holds:
- `Tokens`, with Android's palette as ARGB values, the dynamic `Tokens.Colors`, the spacing, the radii and the 44 pt target;
- `LineBadge` and `EtaText`, whose words follow SwiftUI's environment locale;
- `adaptiveGlass(in:)` and `AdaptiveGlassContainer`;
- the button styles, at least 44 × 44 pt:
  - `AdaptiveGlassButtonStyle`, whose fallback is a capsule of `.regularMaterial` with a primary label;
  - `AdaptiveGlassProminentButtonStyle`, whose enabled label is `onAccent`;
- `Tokens.Typography`.

`GlassRendering` in the environment forces the iOS 18–25 fallback, for snapshots and previews. `Tokens.Colors.cardBackground` (LB-103) and `groupedBackground` (LB-102) are the system's grouped list colours until the brand palette (LB-413).

Since LB-012, the `scrollToTopRequests` environment value counts the re-selections of a tab at its root. The shell sets it on every tab root, and a root with a scrolling list scrolls to its top whenever it changes.

**System first.** These pick up Liquid Glass automatically when built with the iOS 26 SDK and run on iOS 26 or later:

| Need | Component |
| --- | --- |
| Top-level navigation | `TabView` with `Tab(…)`, and `.tabBarMinimizeBehavior(.onScrollDown)` on scrolling tabs (iOS 26) |
| Tab roots | `NavigationStack` with large titles; toolbar items for More |
| Lines search | `.searchable(text:placement:prompt:)` on the Lines stack, with `.navigationBarDrawer(displayMode: .always)` (LB-102) |
| Near Me sheet | `.sheet` with `.presentationDetents([.height(peek), .medium, .large])`, `.presentationBackgroundInteraction(.enabled(upThrough: .medium))` and `.interactiveDismissDisabled()`. Don't set `.presentationBackground`, so the system glass stays |
| Direction toggle | `Picker(.segmented)` |
| Favourite lines | `ScrollView(.horizontal)` + `HStack` with `.scrollTargetBehavior(.viewAligned)` (LB-103): at most 20 cards (FR-FAV-05), and a stack of fixed height gives every card the tallest one's |
| Editing favourite lines | `.sheet` with a `NavigationStack`, an inline title and Done as its confirmation action, over a `List` in edit mode (`.environment(\.editMode, .constant(.active))`) with `.onMove` and `.onDelete`; "Move up" and "Move down" as `.accessibilityActions` (LB-110) |
| A message that needs an answer | `.alert`: the favourite limit (LB-110) |
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
| Freshness, stale | Warning colour, "Actualizado hace N s" ("Updated N s ago"). Placeholder warning: #8A5300 in light, #F2B861 in dark (`StatusColors`, LB-011), at least 4.5:1 on every surface |

### 6.2 Line badge

`LineBadge(code, color, onColor, size = Regular | Small | Large)` is the only way a line is shown, in lists, chips, map markers, itinerary legs and notifications. Its accessibility label is "Línea 3" / "Line B1", never the colour name.

- **Sizes:** Small, Regular and Large (32 × 24, 40 × 28 and 48 × 32 at minimum). The badge grows with the font scale rather than truncate.
- **Text colour:** `onColor` is whichever of black and white contrasts more with the line colour (`onColorFor` in `:shared:core:common`, WCAG 2.2). All 13 current lines reach at least 4.5:1; line 3's purple is the closest, at 4.66:1 in black.

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

As built (LB-011): shared code sends `EtaUi`, with an `EtaDisplay` (`Now`, `Minutes`, `ClockTime`), `lowConfidence`, `scheduled`, an `EtaDelay` and `cancelled`, and each platform's `EtaText` draws and speaks it.
- The spoken label leaves out the line, which the row's `LineBadge` says, for example "approximately in 4 minutes, 3 minutes late".
- It says what the screen shows, with the same precedence: cancelled, then scheduled (which hides a delay), then the delay.
- "Now" pulses down to 70 % opacity, which keeps it at 4.5:1 on every surface, and holds still when the system's animations are off.
- Clock times show as HH:mm with the same digits in every locale.

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
| `lines.favourites.hint` (FR-LINE-14) | Marca ☆ en una línea para tenerla aquí | Tap ☆ on a line to pin it here |
| `lines.favourites.hint_spoken` (spoken, the hint) | Marca la estrella de una línea para tenerla aquí | Tap the star on a line to pin it here |
| `lines.open_line` (spoken, a row's or a card's action, Android) | abrir línea | open line |
| `lines.favourite.add` (spoken, the toggle, FR-LINE-15) | Añadir línea 3 a favoritas | Add line 3 to favourites |
| `lines.favourite.remove` (spoken, the toggle) | Quitar línea 3 de favoritas | Remove line 3 from favourites |
| `lines.towards` (spoken, each headsign of a card) | Hacia Hospital San Pedro | Towards Hospital San Pedro |
| `line.title` (line detail) | Línea 10 | Line 10 |
| `line.title.generic` (line detail, before the code is known) | Línea | Line |
| `line.loading` (spoken, line detail's first load) | Cargando línea | Loading line |
| `lines.group.day` | Líneas diurnas | Day lines |
| `lines.group.night` | Líneas nocturnas (Búho) | Night lines (Búho) |
| `lines.not_operational` (status chip, FR-LINE-02) | Sin servicio | Not in service |
| `lines.empty` | No hay líneas que mostrar. | There are no lines to show. |
| `lines.loading` (spoken, the first load) | Cargando líneas | Loading lines |
| `lines.search` (the search field's prompt, LB-102 part 1) | Buscar línea | Search lines |
| `lines.no_matches` (a search that found nothing) | Ninguna línea coincide con tu búsqueda. | No lines match your search. |
| `search.clear` (the action of a search that found nothing; Android's clear button) | Borrar búsqueda | Clear search |
| `lines.favourites.edit` (the carousel's button, LB-110) | Editar | Edit |
| `lines.favourites.edit_title` (the edit sheet's title, and the button's spoken name) | Editar líneas favoritas | Edit favourite lines |
| `lines.favourites.done` | Listo | Done |
| `lines.favourites.no_longer_served` (a favourite whose line has left the catalogue) | Línea que ya no circula | Line that no longer runs |
| `lines.favourites.remove_no_longer_served` (spoken, Android's remove button for it) | Quitar de favoritas la línea que ya no circula | Remove the line that no longer runs from favourites |
| `lines.favourites.position` (spoken, a row's place in the edit sheet; iOS `lines.favourites.position %lld %lld`) | 2 de 3 | 2 of 3 |
| `lines.favourites.move_up`, `lines.favourites.move_down` (screen readers' actions) | Subir, Bajar | Move up, Move down |
| `lines.favourites.limit` (Android, plural; iOS `lines.favourites.limit %lld` and `lines.favourites.limit_hint`) | Ya tienes 20 líneas favoritas, el máximo. Quita alguna para añadir otra. | You already have 20 favourite lines, the most you can save. Remove one to add another. |
| `nearme.saved.title` | Mis paradas | My stops |
| `nearme.nearby.title` | Cerca de ti | Near you |
| `plan.title` | Cómo llegar | How to get there |
| `action.more` (spoken; iOS `more.action`) | Más | More |
| `action.navigate_up` (spoken, Android) | Atrás | Back |
| `more.title` | Más | More |
| `more.settings` | Ajustes | Settings |
| `more.about_data` | Sobre los datos | About data |
| `settings.title` | Ajustes | Settings |
| `about.title` | Sobre los datos | About data |
| `plan.origin.current` | Mi ubicación | My location |
| `plan.destination.hint` | ¿A dónde vas? | Where to? |
| `freshness.updated_ago` | Actualizado hace %d s | Updated %d s ago |
| `line.badge` (spoken) | Línea %s | Line %s |
| `eta.now` | Ahora | Now |
| `eta.minutes` | %d min | %d min |
| `eta.scheduled` | Programado | Scheduled |
| `eta.cancelled` | Cancelado | Cancelled |
| `eta.late` / `eta.early` | +%d min tarde / %d min antes | +%d min late / %d min early |
| `eta.approximate` | ~%s | ~%s |
| `eta.*_spoken` | llega ahora (, aproximadamente); en %d minuto(s); a la / a las %s (by the hour); programado a la / a las %s; programado; aproximadamente %s; %d minuto(s) tarde / antes; cancelado | arriving now (, approximately); in %d minute(s); at %s; scheduled at %s; scheduled; approximately %s; %d minute(s) late / early; cancelled |
| `eta.spoken_join` | %s, %s | %s, %s |
| `about.last_sync.title` | Última sincronización | Last sync |
| `about.last_sync.never` | Aún no se ha sincronizado | Not synced yet |
| `about.dataset.lines` | Líneas | Lines |
| `about.dataset.stops` | Paradas | Stops |
| `error.network` | No hay conexión. Comprueba tu red. | No connection. Check your network. |
| `error.server` | El servicio no responde. Inténtalo más tarde. | The service isn’t responding. Try again later. |
| `error.data` | No se han podido leer los datos. | The data couldn’t be read. |
| `error.retry` | Reintentar | Retry |
| `offline.banner` (v1.0) | Sin conexión. Mostrando datos guardados. | You are offline. Showing saved data. |
