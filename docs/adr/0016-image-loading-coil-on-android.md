# ADR-0016: Image loading with Coil 3 on Android and AsyncImage on iOS

| Field | Value |
| --- | --- |
| Status | Accepted (2026-10-26) |
| Date | 2026-10-26 |
| Deciders | Project owner: accepted on 2026-10-26, after PR #19 merged, including this refinement of spec §8.3. Written up by the agent for LB-014 |
| Related | NFR-MNT-03, spec §8.3, ADR-0001, ADR-0005 |

## Context

Spec §8.3 lists "Coil 3 (KMP)" for images, with a disk cache for CMS images. ADR-0001 then made the UI native on each platform: Jetpack Compose on Android and SwiftUI on iOS, with no shared UI.

The MVP shows few remote images. Most are CMS content, such as news images, which arrive in v1.0.

Coil 3's multiplatform support renders through Compose, so SwiftUI cannot use it without Compose Multiplatform, which ADR-0001 rules out.

## Decision

- **Android:** Coil 3 (`coil-compose`, with its OkHttp network fetcher) in the Android UI modules, with its memory and disk caches.
- **iOS:** SwiftUI's `AsyncImage`, backed by the system `URLCache`.
- **Shared code** carries image URLs only, never image loading or caching.
- **No dependency yet.** Coil enters the version catalog with the first story that shows a remote image (the v1.0 news stories).

This refines spec §8.3, which names Coil 3 for both platforms. The owner's acceptance of this ADR records the deviation.

## Consequences

- **Each platform uses its idiomatic tool,** with no Compose Multiplatform dependency on iOS.
- **Two caching behaviours.** iOS has only `URLCache`, which is weaker than Coil's disk cache.
  - If v1.0 needs offline images or a larger cache, revisit iOS: for example Nuke, which needs the owner's approval as a third-party SDK.
- **Nothing to build in the MVP,** unless a remote image appears earlier.

## Alternatives considered

- **Coil 3 in shared code for both platforms.** Rejected: on iOS it needs Compose Multiplatform to render, and ADR-0001 rejected shared UI.
- **Glide or Picasso on Android.** Rejected: they are not Compose-first, and Coil is the Compose standard.
- **Nuke or Kingfisher on iOS now.** Deferred: it is a third-party SDK with no MVP need. `AsyncImage` covers the v1.0 news case.
