# ADR-0020: The app's language and theme, each platform's own way

| Field | Value |
| --- | --- |
| Status | Accepted (2026-11-03) |
| Date | 2026-11-03 |
| Deciders | Project owner (2026-11-03, when LB-403 started: the iOS language in the system's Settings). Written up by the agent for LB-403 |
| Related | FR-SET-01, NFR-I18N-01, NFR-A11Y, LB-403, LB-405, ADR-0018, `design_guidelines.md` §2.3, `architecture.md` §7 |

## Context

FR-SET-01 asks Settings for a language (System, Español, English) and a theme (System, Light, Dark), and LB-403's criteria ask that a new language applies to every UI string without a restart.

- **Android** has a per-app language: the framework's `LocaleManager` from Android 13, and AppCompat's `AppCompatDelegate.setApplicationLocales` on every version, which keeps the choice below 13 itself. Either recreates the activity in the new language, with no restart. AppCompat's night mode does the same for the theme, and the window, the system bars and Compose's `isSystemInDarkTheme()` all follow it.
- **iOS has no public API to change an app's language from inside the app.** Its per-app language lives in the app's page of the system's Settings, and choosing one there relaunches the app. An in-app override, `.environment(\.locale)` and a lookup of our own for every string, leaves the system's controls (Edit, Delete, Cancel, alerts) and part of what VoiceOver reads in the device's language.
- **The theme** needs no new storage on either platform beyond ADR-0018's preferences store.

## Decision

- **Android:**
  - **AppCompat** (`androidx.appcompat:appcompat`, 1.8.0) in the app module only. `MainActivity` is an `AppCompatActivity` on AppCompat's DayNight window theme.
  - **Language:** `AppCompatDelegate.setApplicationLocales`, behind the settings feature's `AppLanguages` port, which the shell binds. The manifest declares the languages (`android:localeConfig`, Spanish and English), which Android 13+ also offers in its own per-app language setting, and AppCompat's `AppLocalesMetadataHolderService` with `autoStoreLocales`, which keeps the choice below Android 13.
  - **Theme:** AppCompat's night mode. The activity reads the saved theme before its first frame, so the app never opens in the other theme and then turns, and applies each later change, which recreates it.
- **iOS** (owner decision, 2026-11-03):
  - **Language:** Settings shows the language the app runs in, and the row opens the app's page in the iPhone's Settings (`UIApplication.openSettingsURLString`), where iOS offers the app's languages. iOS relaunches the app after a change, so on iOS the criterion's "without a restart" does not hold; every string, the system's own included, is then in the chosen language.
  - **Theme:** the windows' `overrideUserInterfaceStyle`, from the saved theme, so sheets and alerts follow it, and the system's comes back when the theme is System.
- **Shared:** the theme and the ETA format are `UserPreferences` in ADR-0018's store. The language is not: each platform keeps it, and Settings only reports it as `AppLanguage`.

## Consequences

- **One platform-native way each.** The system's controls, accessibility services and, on Android 13+, the system's own settings all agree with the app.
- **AppCompat in the app.** The activity becomes an `AppCompatActivity` and the window theme an AppCompat one. AppCompat keeps its night mode and locales for the whole process, so Robolectric tests reset them after themselves.
- **A read before the first frame on Android.** The settings file is a few bytes, read once per launch with `runBlocking` in `attachBaseContext`; it counts towards LB-411's cold-start budget.
- **iOS's language takes a relaunch,** which the system does, and the app's state is restored as after any relaunch. The acceptance criterion records it.
- **iOS's theme arrives with the store's first value,** just after the first frame: a device in the other appearance may show it for a frame. To check on a device; a synchronous first read would remove it.

## Alternatives considered

- **iOS: an in-app language switch** through `.environment(\.locale)` and our own string lookup. Rejected by the owner: the system's controls and part of VoiceOver would stay in the device's language, and every string outside a `Text` would need its own code.
- **Android without AppCompat:** the framework's `LocaleManager` from Android 13, and our own locale override below it. Rejected: AppCompat is the documented way, and keeps the choice below 13.
- **The theme in Compose only** (`LogroBusTheme(darkTheme = …)` from the store). Rejected: the window, its starting background and the system bars would still follow the system.
- **iOS: `.preferredColorScheme` on the root view.** Rejected: it applies to the nearest presentation, and a sheet or alert may not follow it.
