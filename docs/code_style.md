# Code style

| Field | Value |
| --- | --- |
| Status | Active; tool configurations are completed in LB-001 |
| Last updated | 2026-10-22 |
| Config files | `.editorconfig`, `.swiftlint.yml`, `config/detekt/detekt.yml` (LB-001) |

`AGENTS.md` §8 summarises the conventions. This document holds the detailed rules and the tool configuration. Formatting is automated wherever possible: a style rule that a tool can enforce must be enforced by the tool.

## 1. All languages

- Everything in the repository (code, comments, identifiers, docs) is in technical English, as set by the language policy in `AGENTS.md`.
- UTF-8, LF line endings, a final newline, no trailing whitespace (`.editorconfig`).
- Line length: 120 for Kotlin and Swift; no limit for Markdown.
- Comments explain *why*. They cite requirement ids when code implements a spec rule (`// DD-05: …`). Do not commit commented-out code.
- No `TODO` without an owner or story: write `// TODO(LB-205): …`.

## 2. Kotlin (shared and Android)

### 2.1 Tooling

- **ktlint**, with the `ktlint_official` code style configured in `.editorconfig`. Run `./gradlew ktlintFormat` before committing; the Claude Code post-edit hook also formats edited files when ktlint is installed.
- **detekt**, with `config/detekt/detekt.yml` (created in LB-001) on top of the default rules. Project-specific rules:

| Rule | Configuration | Why |
| --- | --- | --- |
| `ForbiddenMethodCall` | `kotlin.time.Clock.System.now`, `kotlinx.datetime.Clock.System.now`, `java.lang.System.currentTimeMillis` outside `:shared:core` | Time goes through `AppClock` (`AGENTS.md` §5 rule 5) |
| `ForbiddenImport` | `io.ktor.*` and `app.cash.sqldelight.*` inside domain modules and the `domain` and `presentation` packages of shared feature modules | Clean architecture (rule 1, ADR-0007) |
| `ForbiddenImport` | `com.google.firebase.*` and any other telemetry vendor SDK outside the platform telemetry implementation classes | Telemetry contracts (rule 13, ADR-0009) |
| `ForbiddenMethodCall` | `kotlin.io.println` | Use `Logger` |
| `ForbiddenImport` | `kotlinx.coroutines.GlobalScope` | Structured concurrency |
| Complexity | `LongMethod` 60, `CyclomaticComplexMethod` 15, `LargeClass` 600 | Keep units small |
| `MagicNumber` | Enabled except in tests, tokens and fixture builders | Thresholds come from named constants or remote config |

### 2.2 Conventions

- In shared modules, visibility is `internal` by default. Only what other modules or Swift need is `public`.
- Prefer immutable data: `val`, `data class`, and read-only collections across layers.
- Model errors with `AppResult` and `AppError`. Never use exceptions for expected failures. Catch exceptions only at the network and database boundaries.
- Coroutines: inject `AppDispatchers`; ViewModels use `viewModelScope`; repositories expose `Flow` and `suspend` functions; never use `runBlocking` outside tests.
- Name test functions in `lowerCamelCase` with underscores between clauses: `dd05_reinterpretsWallClockInMadrid_duringCest`. Do not use backtick names with spaces.
- The package layout mirrors the modules, `com.ruizurraca.logrobus.<area>.<layer>`. For example: `com.ruizurraca.logrobus.feature.nearme.presentation`, `com.ruizurraca.logrobus.transit.data`, and on Android `com.ruizurraca.logrobus.android.feature.nearme`.

### 2.3 Compose (Android)

- A screen is split into three pieces:
  - `XRoute`, which obtains the ViewModel, collects state and wires navigation.
  - `XScreen`, which is stateless and takes `UiState` plus event lambdas.
  - Small private composables for its parts.
- The `modifier: Modifier = Modifier` parameter is the first optional parameter of every public composable.
- Keep state hoisted. `remember` holds only UI-local state (scroll, expansion), never business data.
- Provide `@Preview` functions for every UX state in light and dark, using fake `UiState` instances from a `PreviewData` object.
- Never pass a ViewModel down to child composables.
- Use only `designsystem` components and tokens for colours, shapes and typography. No hard-coded colours, except line colours, which come from data.

## 3. Swift (iOS)

### 3.1 Tooling

- **SwiftLint**, configured in `.swiftlint.yml`. Custom rules forbid calling `.glassEffect(` or the glass button styles outside `DesignSystem/` (ADR-0003).
- Formatting follows Xcode's defaults: 4 spaces, with the 120-column guide enabled.

### 3.2 Conventions

- Use the Swift 6 language mode where KMP interop allows it. Otherwise use Swift 5 mode with strict concurrency checking set to `targeted`, and record the exception in the target's build settings with a comment.
- Mark UI-facing types `@MainActor`.
- Each screen's ViewModel is a `@MainActor @Observable final class XViewModel` in its `Feature<X>` target. It gets use cases from `SharedDependencies`, collects them inside `.task {}`, and builds state with the shared mappers. Swift ViewModels contain no business or formatting logic (ADR-0007).
- Naming: `XView` for screens, `XViewModel` for ViewModels, `XRow` and `XCard` for components.
- One primary type per file, and the file is named after it.
- Never force-unwrap (`!`) or force-try outside tests. Use `guard` for early exits.
- Use SwiftUI previews (`#Preview`) for every UX state in light and dark, with fake states.
- Accessibility: every image has a label or is explicitly decorative, and every custom control has traits. Group ETA rows with `.accessibilityElement(children: .combine)` and give them the spoken label from `design_guidelines.md` §6.3.

## 4. Shell scripts

- Write them for `bash` 3.2, the macOS default: no associative arrays, no `mapfile`, no `${var,,}`.
- Start with `#!/usr/bin/env bash` and `set -euo pipefail`. Quote every expansion.
- They must pass `shellcheck` (install it with `brew install shellcheck`). The repository's `.shellcheckrc` resolves sourced files relative to each script. When a script sources `lib.sh`, add `# shellcheck source=lib.sh` above the line. Never disable a check without a comment explaining why.
- Put each script in `scripts/` with a usage comment at the top. Make it executable and safe to run more than once.

## 5. Markdown

- Each doc starts with a title and a header table (status, last updated, related docs).
- Prefer tables for structured facts, Mermaid for diagrams, and relative links between docs. `scripts/checks/check-doc-links.sh` checks the links.
- Use ISO dates (`2026-10-22`). Cite requirement ids instead of restating requirements.
