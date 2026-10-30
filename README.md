# LogroBus

**English** · [Español](README.es.md)

LogroBus is an unofficial native app for Logroño's urban bus network, on Android and iOS. It answers two questions quickly: *when is my bus coming?* and *how do I get there?*

> **Status:** milestone M0 Foundations in progress. The Gradle build, the 19 shared Kotlin Multiplatform modules and the Android and iOS app skeletons exist (LB-001), and CI checks every pull request (LB-002); the shared core is in place (LB-004), with the engine that refreshes live data (LB-009), the client that talks to the source (LB-005, in progress) and the local database with stop search (LB-007), and both apps start them at launch, with the conventions every screen follows (LB-010); the sync that keeps lines and stops on the device is built, and waits for the source's data formats (LB-008, in progress); the design system is in place on both platforms (LB-011); both apps open on their three tabs (LB-012); and the telemetry contracts wait for a vendor, sending nothing without consent (LB-017). See [Project status](#project-status).

## Features (MVP)

The app is organised into three sections and always opens on **Lines**. It runs on Android phones and on iPhone (no iPad in the MVP).

| Section | What it does |
| --- | --- |
| **Lines** | Your favourite lines in a carousel at the top, then every day and night (Búho) line sorted by code, with search. Line detail shows the route map, both directions, the stops with their next arrivals, and live buses. |
| **Near Me** | A map with your location and the nearby stops. Tap a stop to see its next buses in a bottom sheet. The sheet also lists your saved stops with live arrivals, and the stops closest to you. |
| **How to Get There** | Trip planning from A to B: routes that combine lines, walking times and step-by-step directions. |

The app also includes:
- Real-time arrivals with a visible indication of how fresh and how reliable each prediction is.
- Spanish and English.

Planned for v1.0: offline mode, news and service notices, fares, recharge points, a home-screen widget and notifications.

## Tech stack

| Area | Technology |
| --- | --- |
| Architecture | Clean architecture, organised in feature modules; the presentation layer (ViewModels and UI) is written per platform, and the presentation models are shared |
| Shared code | Kotlin Multiplatform: domain, data, networking (Ktor), persistence (SQLDelight), presentation models |
| Android | Kotlin, Jetpack Compose, Material 3 Expressive. Minimum Android 8.0 (API 26) |
| iOS | Swift, SwiftUI, Liquid Glass on iOS 26+. Minimum iOS 18, iPhone only |
| Maps | MapLibre Native on both platforms |
| Location | Google Play services' Fused Location Provider on Android, Core Location on iOS; foreground only |

## Repository structure

```
AGENTS.md                             Rules for AI agents and contributors (canonical)
CLAUDE.md, GEMINI.md                  Pointers to AGENTS.md for specific AI agents
README.md, README.es.md               This file (English, Spanish)
docs/                                 Requirements, architecture, API contract, design, guidelines, backlog, decisions
docs/playbooks/                       Step-by-step workflows for humans and AI agents
scripts/                              Bootstrap, quality gates, CI helpers and agent hooks
.githooks/                            Git hooks (installed by scripts/bootstrap.sh)
.claude/, .github/                    Claude Code configuration; PR templates and CI workflows
logrono-bus-app-functional-spec.md    Functional specification v0.1
shared/                               Kotlin Multiplatform modules: core, transit, favourites, features, umbrella
android/                              Android app, core modules (design system, map, navigation) and feature modules
iosApp/                               iOS app (XcodeGen spec) and the LogroBusUI Swift package
build-logic/, gradle/                 Gradle convention plugins; the version catalog
tools/architecture-tests/             Automated module and package rules
config/                               Tool configuration (detekt)
version.properties                    The app version, shared by Android and iOS
```

The full layout is described in [docs/architecture.md](docs/architecture.md).

## Getting started

After cloning, run `scripts/bootstrap.sh` once: it installs the git hooks and reports any missing tools. `scripts/checks/run-all.sh` runs every quality gate.

The shared modules already build: `./gradlew ciUnitTest` runs the unit tests and the architecture rules. If `ANDROID_HOME` is not set, create a `local.properties` file with `sdk.dir=<path to your Android SDK>`; Git ignores it.

To run the Android app, start an emulator or connect a phone (Android 8.0 or later), then run `./gradlew :android:app:installDebug`. For now it shows a placeholder for the Lines tab.

To run the iOS app on macOS, run `scripts/ios/generate-project.sh` (it is also run by `scripts/bootstrap.sh`, and must be rerun after Kotlin changes). The script builds the shared framework and generates `iosApp/iosApp.xcodeproj`; open that project in Xcode and run the `iosApp` scheme on an iPhone simulator.

You will need:

- JDK 21.
- The latest stable Android Studio with the Kotlin Multiplatform plugin.
- Xcode 26 or later and XcodeGen (`brew install xcodegen`), on macOS, for the iOS app.

## Documentation

| Document | Contents |
| --- | --- |
| [AGENTS.md](AGENTS.md) | Working rules for AI agents and contributors |
| [Functional specification](logrono-bus-app-functional-spec.md) | Scope, data sources, requirements |
| [docs/requirements.md](docs/requirements.md) | MVP scope and the requirements added after the spec |
| [docs/engineering_workflow.md](docs/engineering_workflow.md) | Branches, commits, pull requests, review |
| [docs/testing_strategy.md](docs/testing_strategy.md) | Testing and TDD |
| [docs/ci_cd.md](docs/ci_cd.md) | Continuous integration and delivery |
| [docs/architecture.md](docs/architecture.md) | Modules, layers, data flow, platform integration |
| [docs/api_spec.md](docs/api_spec.md) | Contract of the bus API and the geocoder |
| [docs/design_guidelines.md](docs/design_guidelines.md) | The three sections, Material 3 Expressive and Liquid Glass |
| [docs/backlog.md](docs/backlog.md) | Milestones and stories |
| [docs/adr/](docs/adr/README.md) | Architecture decision records |

## Project status

| Milestone | Scope | Status |
| --- | --- | --- |
| M0 Foundations | Project skeleton, data layer, CI | In progress |
| M1 Lines | Lines section | Not started |
| M2 Near Me | Near Me section | Not started |
| M3 How to Get There | Trip planner | Not started |
| M4 MVP hardening | Error handling, settings, accessibility, translations, Figma designs, privacy declarations | Not started |
| M5 v1.0 | Offline mode, news, fares, recharge points, widget, notifications, release | Not started |

### Recent changes

- **2026-10-30:**
  - LB-008, part 1: the engine that keeps the lines and stops on the device up to date.
    - The first screen that needs them downloads them, and they are refreshed once they are a day old. There is no built-in copy (ADR-0008), so without a connection that first screen will show an error with Retry.
    - A download that looks broken, with fewer than 5 lines or 100 stops, or one that fails halfway, changes nothing: the previous lines and stops stay. An identical download is not written again, unless the app has changed how it reads it.
    - Each refresh counts how many times in a row a saved stop has been missing, so that a stop that is no longer served can be marked later.
    - It is not switched on yet: it starts working once the app can read the source's lines and stops (LB-005, LB-006).
  - LB-017 is done. Both apps have their telemetry contracts, ready for the analytics and crash-reporting vendor that is still to be chosen.
    - Analytics events carry only ids, counts, yes-or-no values and fixed categories, never what you type or where you are. A build check rejects anything else.
    - Nothing is sent without your consent, asked separately for analytics and for crash reports. Until the consent screen arrives, nothing is sent at all.
    - Crash reports keep each error's type and where it happened, never its message, which could quote a search or a position.
    - Release builds have no telemetry yet. Debug builds will print it to the developer console once you can give consent; until then, tests check it. Only the app itself may add a vendor's library, on Android and on iOS, and each app hands its own telemetry to the shared code at launch.
  - Every commit now has one identity, `David Ruiz <davidru85@gmail.com>`, and no co-author lines, as the owner decided. The rule is in `AGENTS.md`, the git hooks and the local checks enforce it, and the setup script sets it for each clone. The owner rewrote the existing history to match.
  - LB-012 is done. Both apps open on their three tabs: Lines, Near me and Directions.
    - Each tab remembers where you were. Open About data from Lines, switch to Near me and back, and it is still there. Tapping the tab again takes it back to its start.
    - A settings button on every tab opens More, with Settings (empty until its options arrive) and About data, which is now reachable.
    - On Android, the tabs move from the bottom bar to a side rail on tablets, foldables and phones in landscape, and Back from Near me or Directions returns to Lines. The tab names stay readable at the largest text sizes, and screen readers can jump between screen titles.
    - Screenshots in light, dark and extra-large text guard the tabs and the new screens on both platforms.
    - The tab screens are placeholders until each section arrives.
- **2026-10-29:**
  - LB-011 is done. Part 2 gives the iPhone app the same design system.
    - The same placeholder colours, line badges and arrival times as Android, with the same words, read aloud by VoiceOver in Spanish or English.
    - Floating map buttons use Liquid Glass on iOS 26, and a frosted material on older versions.
    - Screenshots in light, dark and extra-large text guard each piece, glass included.
  - The agent harness lets Claude Code open pull requests without asking each time, as the owner decided. Merging stays with the owner.
- **2026-10-28:**
  - LB-011, part 1: the Android app has its design system.
    - A neutral placeholder look in light and dark mode, until the final designs arrive. Every text colour is easy to read.
    - Line badges always show the line code, in black or white, whichever reads better on the line's colour. Screen readers say "Line 3", never the colour.
    - Arrival times show "Now", "4 min" or the clock time, with "~" for uncertain predictions, late and early times, scheduled and cancelled buses. Each is read aloud in Spanish or English.
    - Screenshots in light, dark and extra-large text guard the look of each piece on every pull request.
    - The iOS side comes in part 2.
  - LB-010 is done. Part 2 sets the pattern every screen follows on both platforms: the screen gets its logic from the shared code, previews and tests use scripted data, leaving a screen stops its updates, and a failure shows a short reason with Retry.
    - The first screen that follows it is the start of "About data": when the lines and the stops were last updated, on Madrid's clock, in Spanish and English. It opens from the More menu once the tabs arrive (LB-012).
    - The agent harness lets Claude Code push branches without asking each time, as the owner decided. Pushing to `main` and force pushes stay blocked.
  - LB-010, part 1: both apps now start the shared core when they launch: one clock corrected by the server, the client of the data source, the live-data engine and the local database, each created once. Release builds log errors only. iOS sees shared types as native Swift types (SKIE). The screen conventions come in part 2, with the first screen.
- **2026-10-27:**
  - LB-007 is done. The app has its local database, which keeps the lines, stops and routes, your favourites and saved places, and your recent trip searches.
    - Stop search ignores accents, the ñ and capital letters, so "logrono" finds "Logroño".
    - It matches the start of every word, in any order, and a stop number such as "38" finds that stop first.
    - It answers in a few milliseconds over all 254 stops.
  - LB-005, part 2, first half: the client sends its query parameters the way the source expects them, and both apps refuse unencrypted HTTP connections. The data formats of each endpoint wait for the recorded sample data (LB-003).
  - LB-005, part 1: the shared client that talks to the bus data source.
    - It identifies the app and asks for compressed data.
    - It gives up after 10 s on live data and 30 s on the line and stop catalogue.
    - It reads the server's clock from every answer, so arrival times stay right when the phone's clock is wrong, even behind a cache.
    - A broken answer, or one bad item in a list, fails only that piece of data; the other items still show.
    - It keeps no cookies and no cached answers, and its error logs never contain locations or answers.
    - The data formats of each endpoint come in part 2.
  - LB-009 is done. One shared engine now refreshes live data, such as arrivals and bus positions, for the screens that show it:
    - Screens showing the same data share one request.
    - It waits the configured interval, never under 10 s, with a random second either way, so users do not all hit the source at once.
    - It stops in the background and refreshes on return.
    - After an error it keeps the last data and retries after 2, 4, 8 and 16 s, then every 30 s.
    - Re-entering a screen never makes it poll faster.
- **2026-10-26:**
  - LB-004 is done. The shared core can now log, tell whether the app is in the foreground, and read the phone's location. Location comes from Google Play services on Android and from Core Location on iOS. Updates run only while a screen is listening for them, and coordinates are never printed, not even in error logs. Asking for permission arrives with the Near Me screen (LB-201).
  - The owner accepted the seven ADRs of LB-014, so each library can join the build with the first story that needs it.
  - LB-014: seven architecture decision records (ADR-0012 to ADR-0018) for the libraries the spec chose: networking, database, dependency injection, dates and times, images, maps and settings.
  - LB-004, part 1: the shared core has its first building blocks:
    - a clock that corrects the phone's time with the server's, so arrival times are right even when the phone's clock is wrong;
    - the configuration defaults the app runs on;
    - distance and map-shape utilities.
- **2026-10-25:**
  - LB-002 is done: CI is complete. Three throwaway pull requests confirmed that a broken test, a style violation and a drop in test coverage each block a merge.
  - CI: the iOS job starts the iPhone simulator while the app is still building, instead of afterwards, so it finishes sooner.
  - Workflow: the owner no longer announces merges in the conversation. The AI agent watches its pull request and starts the next task once it is merged.
- **2026-10-24:**
  - CI checks dependencies for known security vulnerabilities: a pull request that adds a library with a known critical vulnerability cannot be merged. The first checks found three, in libraries that Android's build and test tools use (Netty, Bouncy Castle and FreeMarker), none of them part of the app. The build now uses the fixed versions.
  - Accepted ADR-0011, which chose the code quality tools: ktlint, detekt and Kover.
  - Code quality tools: every Kotlin change is checked for style (ktlint), for project rules such as "no direct clock calls" (detekt), and for test coverage: at least 80 % of shared domain and data code (Kover). CI runs all three.
  - CI now also checks iOS on GitHub's macOS machines: SwiftLint, the shared code's tests on the iOS simulator, and the app's build and tests.
  - CI: the Kotlin and Android test jobs have readable names even when they are skipped. They are skipped on purpose when a pull request does not touch code.
- **2026-10-23:**
  - Accepted ADR-0010: how the iOS project is generated and how it links the shared code.
  - LB-001, part 3 of 3: the iOS app skeleton. This completes LB-001.
    - The app (`com.ruizurraca.logrobus`, iPhone only, iOS 18 or later) opens on a placeholder for the Lines tab, in Spanish and English.
    - Its start tab comes from the shared code, as on Android.
    - The Xcode project is generated from a spec and is not stored in the repository.
    - The LogroBusUI Swift package holds the iOS screens, still empty, and consumes the shared framework.
  - LB-001, part 2 of 3: the Android app skeleton.
    - The app (`com.ruizurraca.logrobus`, Android 8.0 or later) opens on a placeholder for the Lines tab, in Spanish and English.
    - Its start tab comes from shared code, the first value both apps will share.
    - The Android core and feature modules exist, still empty.
    - CI now also runs the Android unit tests, the build and lint.
  - Started LB-002, part 1: CI now also runs the Kotlin checks on every pull request that touches Kotlin or Gradle files:
    - the build-logic tests, the architecture rules, and the unit tests of each of the 19 shared modules, each in its own job;
    - a single "Kotlin gate" job sums them up and is required before merging.
  - The repository is now public (LB-415):
    - CI runs the policy checks on every pull request, and `main` is protected on GitHub.
    - GitHub secret scanning and push protection are enabled.
    - Licence decided: none is granted, so all rights are reserved.
- **2026-10-22:**
  - Started LB-001, part 1 of 3:
    - Gradle build: Kotlin 2.4.20, Android Gradle Plugin 9.3.3, Gradle 9.7.1.
    - The 19 shared modules.
    - Automatic checks of the module dependency rules and of the architecture.
    - An independent review found 16 issues; all were addressed, with stricter rules and tests for the rules themselves.
  - CI: `actions/checkout` updated from v5 to v7 (Dependabot, PR #2).
  - Project documentation created.
  - Decided: iOS 18 minimum, identifier `com.ruizurraca.logrobus`, Lines as the launch tab, saved stops in Near Me.
  - Private GitHub repository created.
  - Language policy: the project is written in technical English, and conversations with AI agents are in Peninsular Spanish.
  - Defined before scaffolding:
    - MVP requirements and acceptance criteria.
    - Engineering guidelines: TDD, pull requests, CI/CD, releases, security, code style.
    - A harness shared by every AI agent: rules, workflows, quality checks and git hooks.
  - Decided:
    - Offline mode moves to v1.0, and there is no iPad in the MVP.
    - TDD with a commit per phase (red, green, refactor).
    - Clean architecture with feature modules.
    - Telemetry contracts with the vendor still open.
    - Figma designs and privacy declarations at the end of the MVP.
    - The repository goes public, with `main` protected, before the MVP is completed.

## Contributing

Read [AGENTS.md](AGENTS.md) before you start. Code and documentation are written in technical English (see the language policy in AGENTS.md). Work on a branch and open a pull request using one of the templates; nobody pushes to `main`. CI runs the policy, Kotlin and iOS checks on every pull request, and they must pass before merging. Run `scripts/checks/run-all.sh` first and paste its summary into the pull request: it runs the same checks locally, plus any check that has no CI job yet. Every pull request must update this README and its Spanish version, [README.es.md](README.es.md), before it is merged.

## Data and disclaimer

LogroBus is an independent project. It is not affiliated with Logroño City Council (Ayuntamiento de Logroño) or the bus operator.

- **Data:** Ayuntamiento de Logroño ([transporteurbano.logrono.es](https://transporteurbano.logrono.es)).
- **Real-time predictions:** provided by the operator; they may be inaccurate.
- **Map data:** © OpenStreetMap contributors.

The terms for reusing the data are still being confirmed with the City Council.

## License

The source code is public to read, but no licence is granted: all rights reserved. You may not copy, modify or redistribute it without the author's permission.
