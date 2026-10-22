# ADR-0008: Offline mode deferred to v1.0; cache-backed static data in the MVP

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-10-22 |
| Deciders | Project owner |
| Related | Spec G3, FR-ERR-01, FR-ERR-02, NFR-REL-04; `requirements.md`; backlog LB-008, LB-401, LB-607 |

## Context

The spec makes the app "fully browsable offline from first launch" (G3, FR-ERR-01). That requires a seed dataset bundled in both binaries, an offline banner (FR-ERR-02) and cached arrivals for saved stops. The owner has decided that offline functionality is **not** part of the MVP.

## Decision

**In the MVP:**
- **No bundled seed.** Static data (lines, stops, shapes) is downloaded on first use and **cached** in SQLDelight with the TTLs, content hash and atomic replace of spec §8.5. The cache stays because local stop search (FR-STOP-01, under 100 ms), pattern lookups and performance depend on it, not to provide an offline mode.
- **Without connectivity:**
  - Screens that cannot get their data show the error state with the reason "network" and a Retry action (FR-ERR-05).
  - Live requests retry with backoff while the screen is visible (FR-ERR-03).
  - A failed background refresh is silent (FR-ERR-04).
  - If cached static data exists, repositories serve it. This is incidental behaviour, neither promised nor tested end to end.
- **Not built:** the offline banner, arrival snapshots for saved stops, the "expired" offline presentation, and offline browsing guarantees.

**Deferred to v1.0** (LB-607): G3, FR-ERR-01 (seed), FR-ERR-02 (offline banner), cached arrivals for saved stops, NFR-REL-04.

## Consequences

- Less MVP work: no seed pipeline (`seed/`, `scripts/update-seed.sh`, `SeedReader`), no `arrival_snapshot` table and no offline UX state.
- A first launch without network shows an error state until a connection exists. This is acceptable for the MVP.
- The data layer keeps the cache and atomic sync, so the v1.0 offline mode is an addition, not a redesign.

## Alternatives considered

- **Keep the full offline mode in the MVP (spec).** Rejected by the owner to reduce scope.
- **No local cache at all.** Rejected: local search, pattern joins and the start-up budget need the cache.
