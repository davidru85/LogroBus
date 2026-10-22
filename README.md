# LogroBus

**English** · [Español](README.es.md)

LogroBus is an unofficial native app for Logroño's urban bus network, on Android and iOS. It answers two questions quickly: *when is my bus coming?* and *how do I get there?*

> **Status:** planning and documentation. There is no app code yet; the next milestone is M0 Foundations. See [Project status](#project-status).

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
scripts/                              Bootstrap, quality gates and agent hooks
.githooks/                            Git hooks (installed by scripts/bootstrap.sh)
.claude/, .github/                    Claude Code configuration; PR templates and CI workflows
logrono-bus-app-functional-spec.md    Functional specification v0.1
```

The `shared/`, `android/` and `iosApp/` modules will be added in milestone M0. Their planned layout is described in [docs/architecture.md](docs/architecture.md).

## Getting started

There is no app code to build yet. After cloning, run `scripts/bootstrap.sh` once: it installs the git hooks and reports any missing tools. `scripts/checks/run-all.sh` runs every quality gate.

Once the project is scaffolded (story LB-001), this section will explain how to build and run both apps. You will need:

- JDK 21.
- The latest stable Android Studio with the Kotlin Multiplatform plugin.
- Xcode 26 or later, on macOS, for the iOS app.

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
| M0 Foundations | Project skeleton, data layer, CI | Not started |
| M1 Lines | Lines section | Not started |
| M2 Near Me | Near Me section | Not started |
| M3 How to Get There | Trip planner | Not started |
| M4 MVP hardening | Error handling, settings, accessibility, translations, Figma designs, public repository, privacy declarations | Not started |
| M5 v1.0 | Offline mode, news, fares, recharge points, widget, notifications, release | Not started |

### Recent changes

- **2026-10-22:**
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

Read [AGENTS.md](AGENTS.md) before you start. Code and documentation are written in technical English (see the language policy in AGENTS.md). Work on a branch and open a pull request using one of the templates; nobody pushes to `main`. While the repository is private, CI does not run: run `scripts/checks/run-all.sh` and paste its summary into the pull request. Every pull request must update this README and its Spanish version, [README.es.md](README.es.md), before it is merged.

## Data and disclaimer

LogroBus is an independent project. It is not affiliated with Logroño City Council (Ayuntamiento de Logroño) or the bus operator.

- **Data:** Ayuntamiento de Logroño ([transporteurbano.logrono.es](https://transporteurbano.logrono.es)).
- **Real-time predictions:** provided by the operator; they may be inaccurate.
- **Map data:** © OpenStreetMap contributors.

The terms for reusing the data are still being confirmed with the City Council.

## License

No licence has been chosen yet, so all rights are reserved.
