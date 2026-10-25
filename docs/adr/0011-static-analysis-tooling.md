# ADR-0011: Static analysis tooling: ktlint CLI, detekt 2.0 alpha and Kover

| Field | Value |
| --- | --- |
| Status | Accepted (2026-10-24) |
| Date | 2026-10-24 |
| Deciders | Project owner: chose detekt 2.0.0-alpha.6, then accepted the ktlint CLI and the Kover scope after PR #11 merged (2026-10-24) |
| Related | LB-002, NFR-MNT-01, NFR-MNT-02, `code_style.md` §2.1, `testing_strategy.md` §3, `ci_cd.md` §3 |

## Context

`code_style.md` and `testing_strategy.md` require ktlint (the `ktlint_official` style), detekt with project rules, and Kover coverage gates. The build uses Kotlin 2.4.20, AGP 9.3 with built-in Kotlin, the Android KMP library plugin and Gradle 9.7. On 2026-10-24:

- **detekt:** the latest stable release, 1.23.8, is built against Kotlin 2.0. Its type-resolved analysis, which rules such as `ForbiddenMethodCall` need, does not support Kotlin 2.4. Only the 2.0 line (2.0.0-alpha.6, August 2026) is built against Kotlin 2.4.10, Gradle 9.6 and AGP 9.3.1.
- **ktlint:** the Gradle plugins add another moving part on top of AGP's built-in Kotlin, while the CLI needs only a classpath.
- **Kover:** 0.9.11 is stable and works with the Android KMP library plugin through its host tests (verified on `:shared:core:presentation`).

## Decision

- **ktlint 1.8.0 runs as a CLI.** It runs through the root tasks `ktlintCheck` and `ktlintFormat` (`JavaExec`, the shadowed artefact from the catalog, the JDK 21 toolchain), over every `src/**/*.kt` and `*.gradle.kts`, build-logic included.
- **detekt 2.0.0-alpha.6, pinned in the catalog.** This is the owner's decision.
  - The conventions apply it to every app and shared module (`build-logic` and `tools/architecture-tests` get ktlint only), with `config/detekt/detekt.yml` on top of the defaults.
  - Each module has a `ciDetekt` alias that runs the type-resolved tasks: `detektMainAndroid` and `detektHostTestAndroid` in KMP modules, `detektMain` and `detektTest` in Android modules. In KMP modules it also runs every iOS, Apple and native source-set task, which detekt can only analyse without types.
  - Move to 2.0 stable when it ships, and record the move in the project log.
- **Kover 0.9.11 on every shared module.**
  - Domain and data modules and `:shared:core:presentation` are gated at 80 % line coverage, and feature modules on their `domain`, `data` and `presentation` packages. A tested rule, `coverageGate(path)`, decides this.
  - Android ViewModels are reported, not gated, once they exist.
- Each tool has its own CI job (`ktlint`, `detekt`, `Coverage`) behind the `Kotlin gate`, and the root `check` runs all three.

## Consequences

- Every project rule works on common and Android code, including the type-resolved ones. Probes confirmed that `println` and `Clock.System.now()` in shared common code are reported.
- In iOS-only source sets, type-resolved rules such as `ForbiddenMethodCall` cannot fire, because detekt has no classpath there. Review covers them, and they hold no code yet.
- A module's local `check` also runs detekt's plain `detekt` task. It finds a superset of CI's findings, so it never gives a false green.
- An alpha build tool can change its configuration format between releases. The configuration is kept small (only the differences from the defaults) to limit that churn. detekt does not ship in the apps.
- Formatting is one command (`./gradlew ktlintFormat`), and CI rejects unformatted code.

## Alternatives considered

- **detekt 1.23.8, with its Kotlin version forced.** Fragile, and its type-resolved rules may not work on Kotlin 2.4 code.
- **Postponing detekt until 2.0 is stable.** That leaves the time and telemetry rules to manual review.
- **The ktlint Gradle plugins (jlleitschuh, Spotless).** Workable, but they add plugin compatibility to track against AGP 9's built-in Kotlin, for no gain over the CLI.
