# ADR-0010: iOS project generation and framework linking

| Field | Value |
| --- | --- |
| Status | Proposed |
| Date | 2026-10-23 |
| Deciders | Project owner (pending) |
| Related | ADR-0004 (amends its integration bullet), ADR-0007, LB-001, NFR-PLAT-03, `architecture.md` §5.2 |

## Context

ADR-0004 says Xcode integrates the `Shared` framework through the `embedAndSignAppleFrameworkForXcode` build phase. ADR-0007 adds a local Swift package, `LogroBusUI`, whose feature targets consume `Shared` as an XCFramework binary target, a set-up it asked LB-001 to validate. The owner also decided that the Xcode project is generated, not committed.

LB-001 built and tested both, and found two constraints:

- `Shared` is a static framework. If the app linked it through `embedAndSignAppleFrameworkForXcode` while the package's binary target also linked it, the app would contain two copies of the Kotlin code and runtime.
- Swift Package Manager resolves binary targets before any build phase runs. A build phase therefore cannot produce or refresh the XCFramework the package points at.

## Decision

- **Project generation.** `iosApp/project.yml` (XcodeGen, version 2.46.0 or later) is the source of the Xcode project. `scripts/ios/generate-project.sh [debug|release]` does three things, and `scripts/bootstrap.sh` calls it on macOS. None of its outputs are committed.
  1. It builds the `Shared` XCFramework with Gradle and copies it into `iosApp/Packages/LogroBusUI/Frameworks/`.
  2. It writes `iosApp/Configuration/Version.xcconfig` from `version.properties`.
  3. It generates `iosApp/iosApp.xcodeproj`.
- **One link, through the package.**
  - `LogroBusUI` declares `Shared` as a binary target. The feature targets depend on it, and a `Shared` library product lets the app target `import Shared`.
  - The app links `Shared` only through the package; it does not use `embedAndSignAppleFrameworkForXcode`.
- **Refresh by script.** After a Kotlin change, developers and CI rerun the script. Release builds and archives use the `release` argument.
- **Settings on the target.** Settings that XcodeGen's presets also set, such as `TARGETED_DEVICE_FAMILY`, are declared on the app target. The built bundle is checked by `iosAppTests`.

This amends the integration bullet of ADR-0004. Its other decisions (one umbrella module, SKIE, Swift ViewModels, plain types across the boundary) stand.

## Consequences

- The Kotlin code is linked once, statically, and the app and feature targets see the same `Shared` module.
- Xcode does not rebuild Kotlin on its own: running a stale framework is possible until the script is rerun. The LB-002 iOS CI job always runs the script first.
- No `.xcodeproj` merge conflicts, but every machine needs XcodeGen (`brew install xcodegen`).
- Xcode also embeds an empty `Shared.framework` stub (about 60 KB, no symbols) through the `Shared` product. It is harmless on the simulator; check it at the first archive (LB-002 or the release story).
- Future remote Swift packages keep their pins in a `Package.resolved` inside the generated project, which is git-ignored. The ignore pattern must change when the first remote package arrives (LB-013).

## Alternatives considered

- **`embedAndSignAppleFrameworkForXcode` for the app, and feature folders in the app target instead of the package** (the fallback in ADR-0007). It rebuilds Kotlin on every Xcode build, but it gives up the package's per-feature targets and boundaries. Not needed, because the package works.
- **`embedAndSignAppleFrameworkForXcode` plus the package's binary target.** Rejected: two copies of the static framework.
- **Committing the `.xcodeproj`.** Rejected by the owner; it produces merge conflicts and drifts from the settings.
