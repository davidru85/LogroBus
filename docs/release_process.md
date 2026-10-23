# Release process

| Field | Value |
| --- | --- |
| Status | Draft; release automation is decided in M5 |
| Last updated | 2026-10-22 |
| Related | `engineering_workflow.md`, `ci_cd.md`, `security_privacy.md`, backlog E7 |

This document covers versioning, build variants, signing, distribution channels and the release checklist for both apps.

## 1. Versioning

- **Marketing version:** Semantic Versioning `MAJOR.MINOR.PATCH`, shared by Android and iOS.
  - Before the public release, versions are `0.x`: `0.1.0` is the first internal alpha (end of M2), and `0.5.0` is the closed beta (end of M4).
  - `1.0.0` is the public release (end of M5).
- **Build number:** one monotonically increasing integer, shared by Android `versionCode` and iOS `CFBundleVersion`. It comes from CI (`GITHUB_RUN_NUMBER` plus a fixed offset). Local builds use `1`.
- **Single source of truth:** `version.properties` at the repository root, created in LB-001:
  - It contains `VERSION_NAME=0.1.0`.
  - Gradle reads it.
  - `scripts/ios/generate-project.sh` generates `iosApp/Configuration/Version.xcconfig` from it (`VERSION_NAME`, read as `MARKETING_VERSION`). A build phase could not do it, because Xcode resolves build settings before any phase runs. Release builds and archives run `scripts/ios/generate-project.sh release`, so they link the optimised Kotlin framework (ADR-0010).
  - Version bumps change only this file.
- **Tags:** each release is tagged `v<version>` on `main`, for example `v0.1.0`.

## 2. Build variants

There is no staging source environment (OQ-05), so variants differ only in tooling and configuration, never in the data source.

| Variant | Android | iOS | Characteristics |
| --- | --- | --- | --- |
| Debug | `debug` build type | `Debug` configuration | Debug menu, verbose logging, StrictMode (Android), development map style, `.debug` application-id suffix so it can be installed next to release |
| Release | `release` build type | `Release` configuration | R8 shrinking (Android), production map style (OQ-10), no debug menu, no verbose logging |
| Benchmark | `benchmark` build type (release-like, debuggable for profiling) | Release with a profiling scheme | Macrobenchmark and Baseline Profile generation (LB-411) |

**Debug menu (debug builds only):**
- Override `api_base_url` and any remote-config key.
- Simulate a server clock offset.
- Show the latest `DataQualityReport`.
- Force each UX state for screenshots and manual QA.
- Reset onboarding.

## 3. Signing

| Platform | Approach |
| --- | --- |
| Android | Play App Signing. The **upload key** keystore never enters the repository: the owner keeps it in a password manager. When release automation lands, CI gets it as a base64 GitHub secret, together with its passwords |
| iOS | Automatic signing with the owner's Apple Developer team for local builds. For CI distribution, an App Store Connect API key stored as GitHub secrets (set up in M5) |

**Prerequisite (owner action, LB-707):** an Apple Developer Program membership and a Google Play Console account are needed before the first TestFlight or Play internal-testing build.

## 4. Distribution channels

| Stage | Android | iOS | Audience |
| --- | --- | --- | --- |
| Internal alpha (`0.1`–`0.4`) | Play internal testing | TestFlight internal testers | Owner and close testers |
| Closed beta (`0.5`–`0.9`) | Play closed testing | TestFlight external testers (needs Beta App Review) | Invited riders |
| Public (`1.0+`) | Production with a staged rollout: 10 % → 50 % → 100 % over at least 3 days | App Store with a 7-day phased release | Everyone |

Advance a rollout only if the crash-free rate stays at or above 99.8 % (NFR-REL-01). Until a crash-reporting backend exists (OQ-16), use the Play Console vitals and the App Store Connect crash metrics.

## 5. Release checklist

1. Every story in scope is `done`, and `main` is green.
2. Bump `VERSION_NAME` in `version.properties` through a PR, and update both READMEs ("Project status", "Recent changes").
3. Write the store release notes in es-ES and en, short and user-facing. Keep them in `docs/release_notes/<version>.md`.
4. Run the manual accessibility pass (`design_guidelines.md` §8) on the minimum and latest OS versions.
5. Run a smoke test of the five critical journeys on physical devices.
6. Review the privacy declarations against `security_privacy.md` §5 if any data flow changed.
7. Tag `v<version>` on `main` and create a GitHub release with the notes.
8. Upload, roll out per §4, and monitor crashes and reviews for 72 h.
9. Add a `project_log.md` entry recording the release.

## 6. Hotfixes

- Branch `hotfix/<version>` from the release tag, fix the problem with a regression test, open a PR to `main`, then release `PATCH + 1` from `main`.
- If `main` already contains unreleased work that must not ship, cherry-pick the fix onto the hotfix branch, release from there, and merge the fix to `main` too.

## 7. Automation (to decide in M5)

The candidates are Gradle Play Publisher for Android uploads, and either `xcodebuild` with the App Store Connect API or Fastlane for iOS. Decide with an ADR when M5 starts. Until then, releases are manual, following §5.
