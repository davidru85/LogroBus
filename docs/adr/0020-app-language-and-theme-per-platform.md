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
  - **Theme:** from Android 12, the system's per-app night mode (`UiModeManager.setApplicationNightMode`), which the system keeps across launches and applies before any of the app's code runs, starting window included. Below Android 12, AppCompat's night mode: the activity reads the saved theme before its first frame, once per process, so the app never opens in the other theme and then turns. That read is the one `runBlocking` outside tests (`code_style.md` §2.2, owner decision, 2026-11-03). Each later change recreates the activity once.
  - **Language changed outside the app:** from Android 13 the system's settings offer the app's languages too; `MainActivity` reads the language again at each creation, so Settings shows it.
  - **Play's language splits are off** (`bundle.language.enableSplit = false`): the language is chosen in the app, so every language's strings must be installed, not only the device's.
  - *Update, 2026-11-04 (LB-405):* **A device in another language.** The app's strings fall back to Spanish from `values/`, but Material's and Android's strings, the plural rules and the date formats followed the device. `MainActivity` now attaches in the device's languages that the app speaks, in their order, or else in Spanish with the device's region and regional preferences (`AppLocales.kt`), and makes that the process's default locale, so everything on screen is in one language. A language chosen in Settings is one of the two, so it is unaffected. One case remains, below Android 13: once "System" is chosen in Settings during a session, AppCompat applies the device's own locale to the activity until the app restarts, so Android's and Material's words, the plural rules and the date formats follow the device while the app's strings stay Spanish. Giving the application itself Spanish resources, by overriding its `getResources()`, broke every other context's configuration in Robolectric, so it was not done. The other way, keeping the choice below Android 13 in the app's own settings instead of AppCompat's storage, would change this ADR and waits for the owner.
- **iOS** (owner decision, 2026-11-03):
  - **Language:** Settings shows the language the app runs in, and the row opens the app's page in the iPhone's Settings (`UIApplication.openSettingsURLString`), where iOS offers the app's languages. iOS relaunches the app after a change, so on iOS the criterion's "without a restart" does not hold; every string, the system's own included, is then in the chosen language.
  - *Update, 2026-11-04 (LB-405):* **A device in another language** gets the app in Spanish, as on Android: iOS gives the app its development language (`Bundle.main.preferredLocalizations` is `es` on a French device), and the system's own words follow. But the lookups that take a locale, such as a `LocalizedStringResource`'s (the tab labels) and DesignSystem's, matched English for the device's `fr_FR`, so a French device saw English tabs and line badges over Spanish screens. The app's root view therefore gives SwiftUI the language iOS gave the app with the device's region (`Locale.app`, `es_FR` on a French device), so the screens, the system's words and the formats agree (verified on iOS 26.5, `ShellUITests.test_lb405_…`). A first look, with the class method `Bundle.preferredLocalizations(from:forPreferences:)`, which knows no development language, had read the English as iOS's own choice; the owner's answer then, each platform's own rule, gives Spanish on both.
  - **Theme:** the windows' `overrideUserInterfaceStyle`, from the saved theme, so sheets and alerts follow it, and the system's comes back when the theme is System. The app reads the saved theme at once at launch (user defaults are synchronous), so its first frame is in it.
- **Shared:** the theme and the ETA format are `UserPreferences` in ADR-0018's store. The language is not: each platform keeps it, and Settings only reports it as `AppLanguage`.

## Consequences

- **One platform-native way each.** The system's controls, accessibility services and, on Android 13+, the system's own settings all agree with the app.
- **AppCompat in the app.** The activity becomes an `AppCompatActivity` and the window theme an AppCompat one. AppCompat keeps its night mode and locales for the whole process, so Robolectric tests reset them after themselves.
- **A read before the first frame below Android 12.** The settings file is a few bytes, read once per process with `runBlocking` in `attachBaseContext`; it counts towards LB-411's cold-start budget. From Android 12 nothing is read: the system applies the theme.
- **iOS's language takes a relaunch,** which the system does, and the app's state is restored as after any relaunch. The acceptance criterion records it.
- **The launch screens** are drawn by each system before any app code: Android 12+'s starting window follows the app's night mode, while iOS's launch screen and Android's below 12 follow the system's appearance.

## Alternatives considered

- **iOS: an in-app language switch** through `.environment(\.locale)` and our own string lookup. Rejected by the owner: the system's controls and part of VoiceOver would stay in the device's language, and every string outside a `Text` would need its own code.
- **Android without AppCompat:** the framework's `LocaleManager` from Android 13, and our own locale override below it. Rejected: AppCompat is the documented way, and keeps the choice below 13.
- **The theme in Compose only** (`LogroBusTheme(darkTheme = …)` from the store). Rejected: the window, its starting background and the system bars would still follow the system.
- **iOS: `.preferredColorScheme` on the root view.** Rejected: it applies to the nearest presentation, and a sheet or alert may not follow it.
