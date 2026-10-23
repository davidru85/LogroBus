# ADR-0004: Swift interop: umbrella framework and SKIE

| Field | Value |
| --- | --- |
| Status | Proposed, amended by ADR-0007 (no shared ViewModels and no ViewModel bridge) and ADR-0010 (the app links `Shared` through the Swift package, not `embedAndSignAppleFrameworkForXcode`) |
| Date | 2026-10-22 |
| Deciders | Tech lead |
| Related | ADR-0001, spec §8.2, `architecture.md` §3.2 and §5.2 |

## Context

The iOS app consumes shared use cases (`Flow`, `suspend` functions) and shared presentation models (sealed `UiState` hierarchies) from Swift ViewModels (ADR-0007). By default, Kotlin/Native's Objective-C export turns flows into opaque objects, sealed classes into plain class hierarchies, and suspend functions into completion handlers. That is awkward to use from SwiftUI and easy to misuse.

The spec splits the shared code into several Gradle modules. Exporting each module as its own Apple framework would embed several Kotlin runtimes, and their types would not be interchangeable.

## Decision

- **One umbrella module, `:shared:umbrella`,** produces a single static framework named `Shared`. It `export`s the shared feature modules (use-case interfaces and presentation models), the domain modules and the core modules Swift needs, and it is the only Kotlin artefact Xcode links. It is also packaged as an XCFramework for the local Swift package (ADR-0007). Xcode integrates it directly through the `embedAndSignAppleFrameworkForXcode` build phase, without CocoaPods.
- **SKIE** (Touchlab) is applied to the umbrella module. It maps `Flow` and `StateFlow` to `AsyncSequence`, sealed hierarchies to exhaustive Swift enums through `onEnum(of:)`, and `suspend` functions to `async`.
- **Swift ViewModels** (`@MainActor @Observable`, one per screen, in `Feature<X>` targets) obtain shared use cases from the Kotlin `SharedDependencies` factory object, collect them inside `.task {}`, and build state with the shared mappers (ADR-0007). There are no Kotlin ViewModels on iOS.
- **UI models crossing into Swift use plain types**, not Kotlin value classes.

## Consequences

- Swift code is idiomatic: `for await`, exhaustive `switch` and `async` calls.
- Every Kotlin upgrade has to wait for a compatible SKIE release, so check compatibility before bumping Kotlin.
- Build times grow somewhat because of SKIE's code generation.
- If Kotlin's Swift export matures, SKIE can be replaced in a later ADR.

## Alternatives considered

- **KMP-NativeCoroutines.** A good option for flows and suspend functions, but it does not map sealed classes to Swift enums. Kept as the fallback if SKIE lags behind Kotlin releases.
- **Kotlin Swift export (experimental).** Not mature enough for production in v1.
- **Plain Objective-C export with hand-written wrappers.** Too much boilerplate and too error-prone.
