# Playbook: review checklists

**Used by:** human reviewers, every AI agent, and the Claude Code reviewer subagents (one per section).

## How to review

- **Get the change:** `git diff origin/main...HEAD`, or `gh pr diff <number>` for an open PR.
- **Report findings** as a list, most severe first. Each finding gives:
  - a severity: `blocker`, `major`, `minor` or `nit`;
  - its location, as `path:line`;
  - the rule it breaks, as a doc and section;
  - a concrete fix.
- **End with a verdict:** `approve`, `approve with nits` or `changes required`.
- **Only report what you can point to in the diff.** If a section has nothing to check, say so in one line.

## 1. General (every change)

- [ ] Architecture rules hold ([`AGENTS.md`](../../AGENTS.md) §5, rules 1–13): clean-architecture layering and feature isolation; no DTOs above the data layer; no business or formatting logic in ViewModels or views; `AppClock`; money in cents; `PollingEngine`; cache-backed static data; lenient networking; nothing hard-coded that remote config owns; telemetry only through contracts.
- [ ] The language policy holds: code, comments, identifiers, commit messages and docs are in technical English.
- [ ] Tests match [`testing_strategy.md`](../testing_strategy.md) §3 for this kind of change. No test calls the live source.
- [ ] TDD was followed (§7): the PR shows red, green and refactor commits per cycle; new behaviour has tests that are named after the story or DD rule and that cover each acceptance criterion. Bug fixes include a regression test.
- [ ] Module boundaries hold (ADR-0007): no feature depends on another feature; domain modules are free of data, network, database and platform dependencies.
- [ ] Telemetry goes only through the shared contracts (ADR-0009). No offline-mode work in the MVP (ADR-0008).
- [ ] The PR description contains the real `scripts/checks/run-all.sh` summary, the evidence for every check that CI does not run yet, and the required CI checks are green.
- [ ] The docs are updated: backlog, project log, **both READMEs with equivalent content**, and any affected doc or ADR.
- [ ] No secrets, keystores or `local.properties` are included. No new dependency without a justification and an allowed licence.
- [ ] The code follows [`code_style.md`](../code_style.md), with no new lint findings.

## 2. Data rules (`shared/core`, `shared/network`, `shared/data`, `shared/database`)

- [ ] Every source quirk is fixed in a mapper, once. Each `DD-nn` rule touched has a `ddNN_` test ([`api_spec.md`](../api_spec.md) §6).
- [ ] Collections are decoded item by item. Bad items are dropped and counted in the `DataQualityReport`. Any 2xx is accepted.
- [ ] VM and ET data are never joined by vehicle id (DD-03). An ambiguous direction stays `null` (DD-04).
- [ ] The DD-05 correction applies only beyond the 30-minute heuristic, and there are tests for both CET and CEST.
- [ ] Instants are UTC; display uses `Europe/Madrid`; `AppClock` is the only source of "now".
- [ ] Static sync stays atomic: validation thresholds hold, and a failed sync leaves the previous data intact.
- [ ] Polling goes through `PollingEngine`, respects the interval floor and pauses in the background.
- [ ] No coordinates or free text are logged (NFR-PRIV-02).

## 3. Android UI (`android/`)

- [ ] Theme and tokens come from `designsystem` only. Expressive opt-ins are confined to `designsystem` wrappers. Line colours come from data.
- [ ] Each screen is split into `XRoute` (wiring) and a stateless `XScreen(UiState, events)`, and state is collected with `collectAsStateWithLifecycle()`.
- [ ] The `XViewModel` lives in `:android:feature:<x>`, uses shared use cases, and builds state only with the shared `UiState` types and mappers. It contains no business or formatting logic.
- [ ] Every UX state of spec §7.3 is rendered and has a `@Preview` in light and dark.
- [ ] Lines are always shown with `LineBadge` (code plus colour). ETAs use the semantic display model and the spoken labels of [`design_guidelines.md`](../design_guidelines.md) §6.3.
- [ ] Touch targets are at least 48 dp; nothing truncates at 200 % font; content is drawn edge-to-edge with insets handled.
- [ ] Every string is in resources, in es and en. Navigation keeps one back stack per tab.

## 4. iOS UI (`iosApp/`)

- [ ] System components come first: `TabView` / `Tab`, `NavigationStack`, `.searchable`, sheets with detents.
- [ ] Glass appears only on the floating control layer, through the `DesignSystem/` helpers (the SwiftLint custom rules pass). No glass on rows, cards or badges, and no glass on glass.
- [ ] Each screen has a Swift `@MainActor @Observable` `XViewModel` in its `Feature<X>` target. It gets use cases from `SharedDependencies`, collects inside `.task {}`, and builds state with the shared mappers. It contains no business or formatting logic.
- [ ] No iPad-specific code or device family: the MVP is iPhone only (NFR-PLAT-03).
- [ ] Every UX state has a `#Preview`. The iOS 18–25 fallback appearance stays usable (NFR-PLAT-01).
- [ ] Accessibility: ETA rows are combined into one element with spoken labels; images are labelled; controls have traits. The view is checked with Reduce Transparency and Increase Contrast.
- [ ] Touch targets are at least 44 pt. Every string is in `Localizable.xcstrings`. There is no force unwrap or force try, and UI types are `@MainActor`.
