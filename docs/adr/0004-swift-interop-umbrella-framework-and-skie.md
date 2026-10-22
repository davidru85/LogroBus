# ADR-0004: Swift interop: umbrella framework and SKIE

| Field | Value |
| --- | --- |
| Status | Proposed |
| Date | 2026-10-22 |
| Deciders | Tech lead |
| Related | ADR-0001, spec §8.2, `architecture.md` §3.2 and §5.2 |

## Context

The iOS app consumes shared ViewModels that expose `StateFlow<UiState>`, sealed state hierarchies and `suspend` functions. By default, Kotlin/Native's Objective-C export turns flows into opaque objects, sealed classes into plain class hierarchies, and suspend functions into completion handlers. That is awkward to use from SwiftUI and easy to misuse.

The spec splits the shared code into several Gradle modules. Exporting each module as its own Apple framework would embed several Kotlin runtimes, and their types would not be interchangeable.

## Decision

- **One umbrella module, `:shared:umbrella`,** produces a single static framework named `Shared`. It `export`s `presentation`, `domain` and `core`, and is the only Kotlin artefact Xcode links. Xcode integrates it directly through the `embedAndSignAppleFrameworkForXcode` build phase, without CocoaPods.
- **SKIE** (Touchlab) is applied to the umbrella module. It maps `Flow` and `StateFlow` to `AsyncSequence`, sealed hierarchies to exhaustive Swift enums through `onEnum(of:)`, and `suspend` functions to `async`.
- **SwiftUI views never touch Kotlin ViewModels directly.** Each screen has an `@MainActor @Observable` wrapper in `iosApp/Bridge/` that obtains its ViewModel from the Kotlin `ViewModels` factory object, mirrors `state` inside `.task {}`, forwards events, and clears the ViewModel when the wrapper is released.
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
