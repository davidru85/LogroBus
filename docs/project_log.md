# Project log

A dated record of what was done and decided, newest first. Add an entry at the end of every working session. Keep entries short and link to the backlog stories, ADRs and open questions involved.

## Entry template

```markdown
## YYYY-MM-DD: <short title>

**Done**
- …

**Decisions**
- …

**Open / next**
- …
```

---

## 2026-10-22: Bilingual README and the README rule

**Done**
- Created the project README in English (`README.md`) and Spanish (`README.es.md`), with links to switch language.
- Added the README rule to `AGENTS.md` (§2, §10, §11, §12) and linked both READMEs from `docs/README.md`.

**Decisions**
- Every merged PR must update both READMEs. The update goes inside the PR, at least in "Recent changes" and "Project status".

**Open / next**
- Start M0 with LB-001.

## 2026-10-22: Owner decisions on OQ-13, OQ-14, OQ-15 and OQ-17

**Done**
- Recorded the owner's answers in `open_questions.md`, ADR-0002, ADR-0003, `AGENTS.md`, `architecture.md` and `design_guidelines.md`.

**Decisions**
- **OQ-13:** the iOS deployment target is 18.0, with Liquid Glass on iOS 26+ and a material fallback below. ADR-0003 is now accepted.
- **OQ-14:** the `applicationId`, bundle id and Kotlin package root are `com.ruizurraca.logrobus`. The name "LogroBus" and the scheme `logronobus://` are unchanged.
- **OQ-15:** the app always opens on Lines. Deep links open their target tab. G1 is met with one tap to Near Me.
- **OQ-17:** saved stops with live arrivals are shown in the Near Me sheet ("My stops").

**Open / next**
- Choose the https domain for deep links (spec §6.15) before LB-603.
- Start M0 with LB-001.

## 2026-10-22: Project documentation bootstrapped

**Done**
- Read the functional specification v0.1 (`logrono-bus-app-functional-spec.md`).
- Created `AGENTS.md` as the canonical agent instructions, and `CLAUDE.md` as a pointer that imports it.
- Created `docs/`:
  - `README.md`, `architecture.md`, `api_spec.md`, `design_guidelines.md`
  - `backlog.md`, `open_questions.md`, `project_log.md`
  - `adr/`, with ADR-0001 to ADR-0005.
- Initialised the git repository on branch `main` and made the first commit.

**Decisions**
- **Three-section navigation** (Lines, Near Me, How to Get There) replaces the five-tab structure of spec §7.1. Home was dissolved: favourite lines moved to the Lines carousel, and saved and nearby stops moved to the Near Me sheet. News, fares, recharge points and info moved to a More entry (ADR-0002, accepted).
- **Native UI on both platforms** over a shared KMP core: Compose with Material 3 Expressive, and SwiftUI with Liquid Glass. This resolves OQ-01 (ADR-0001, accepted).
- **iOS deployment target 18.0**, with the Liquid Glass fallback (ADR-0003, proposed; OQ-13).
- **Swift interop** through a single umbrella framework, `Shared`, plus SKIE (ADR-0004, proposed).
- **Android UI stack:** M3 Expressive, Navigation 3 with per-tab back stacks, and `NavigationSuiteScaffold` (ADR-0005, proposed).
- **MVP scope** is milestones M0–M4, built around the three sections. News, fares, recharge points, the widget and notifications move to M5 (v1.0); their spec priorities still apply to the public release.

**Open / next**
- The owner should confirm OQ-13 (iOS target), OQ-14 (identifiers), OQ-15 (launch tab) and OQ-17 (favourite stops placement).
- Start M0 with LB-001 (scaffold), then LB-003 (fixtures) and LB-004 to LB-006 (core, network, normaliser).
- Decide where to keep the functional spec: it currently sits at the repository root.
