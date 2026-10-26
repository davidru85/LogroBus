# ADR-0014: Koin for dependency injection

| Field | Value |
| --- | --- |
| Status | Accepted (2026-10-26) |
| Date | 2026-10-26 |
| Deciders | Project owner (spec §8.3 baseline, 2026-10-22): accepted on 2026-10-26, after PR #19 merged. Written up by the agent for LB-014 |
| Related | NFR-MNT-03, spec §8.3, AGENTS.md §5.12, `architecture.md` §5, ADR-0004, ADR-0007 |

## Context

The dependency graph has a few dozen bindings: repositories, use cases, clocks, configuration and the telemetry contracts. It is assembled in `:shared:umbrella` (ADR-0007).

The two platforms consume it differently:
- **Android:** ViewModels receive shared use cases.
- **iOS:** Swift must not depend on a DI framework's generics. It gets use cases from the `SharedDependencies` factory object (ADR-0004, AGENTS.md §5.12).

## Decision

- **Koin** assembles the graph in `:shared:umbrella`, with one Koin module per shared module.
- **Start-up:** each platform starts Koin from its app shell. Android passes its context and platform implementations (`initKoin`), and Swift calls `doInitKoin` with its implementations.
- **Android:** ViewModels get use cases with Koin's Android and Compose integrations (`koinViewModel()`).
- **iOS:** Swift never calls Koin. `SharedDependencies` resolves from the graph and exposes plain factory functions.
- **Safety net:** a unit test verifies the whole graph, so a missing binding fails CI rather than the app at runtime.
- **No dependency yet.** Koin enters the version catalog with LB-010.

## Consequences

- **Little ceremony and no code generation.** No KSP and no compiler plugin, so nothing is added to the Kotlin/Native build.
- **Resolution happens at runtime.** The graph-verification test closes most of that gap. A binding resolved with the wrong scope can still surface only at runtime.
- **Swift is insulated** from Koin through `SharedDependencies`, so changing the DI library later does not affect Swift code.

## Alternatives considered

- **Compile-time DI for KMP** (kotlin-inject, Metro). Rejected for now:
  - Both need KSP or a compiler plugin on every target, which adds build time and compatibility checks with each Kotlin bump (as ADR-0004 already requires for SKIE).
  - Metro is new.
  - The verification test gives most of the safety at a lower cost.
- **Manual DI** (a hand-written graph object). Rejected: it grows with every feature, and Android ViewModel factories need boilerplate that Koin provides.
- **Hilt.** Rejected: it is Android-only and cannot build the shared graph.
