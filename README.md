# LogroBus

**English** · [Español](README.es.md)

LogroBus is an unofficial native app for Logroño's urban bus network, on Android and iOS. It answers two questions quickly: *when is my bus coming?* and *how do I get there?*

> **Status:** milestone M0 Foundations in progress. The Gradle build, the 19 shared Kotlin Multiplatform modules and the Android and iOS app skeletons exist (LB-001); the data layer comes next. See [Project status](#project-status).

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

- **2026-10-24:**
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
