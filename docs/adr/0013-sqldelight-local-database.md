# ADR-0013: SQLDelight for the local database

| Field | Value |
| --- | --- |
| Status | Proposed |
| Date | 2026-10-26 |
| Deciders | Project owner (spec §8.3 baseline, 2026-10-22). Written up by the agent for LB-014; awaiting acceptance |
| Related | NFR-MNT-03, FR-STOP-01, spec §8.3, §8.5, §8.8, `architecture.md` §4.3, ADR-0007, ADR-0008 |

## Context

The shared code keeps a local database for:
- the cache of lines, patterns, shapes and stops, which background sync replaces atomically (ADR-0008, spec §8.5);
- stop search with accent folding (FR-STOP-01);
- favourites and saved places.

Spec §8.8 gives the schema as SQL and includes an FTS virtual table. The same database must work on Android and iOS, and in fast in-memory unit tests.

## Decision

- **SQLDelight** in `:shared:core:database`, with one schema written as `.sq` files that follow spec §8.8 and the additions in `architecture.md` §4.3.
- **Drivers:** the Android driver, the native driver on iOS, and an in-memory JVM driver for repository tests (AGENTS.md §9).
- **Search:** stop search uses the FTS4 virtual table from the spec, over a `normalized_name` column filled by the shared normaliser.
- **Atomic sync:** a sync replaces its datasets in a single transaction.
- **Reactive reads:** queries expose `Flow` through the coroutines extension.
- **Module rules:** SQLDelight is allowed only in `:shared:core:database` and in data modules (AGENTS.md §5.1, `ModuleRules`).
- **Migrations:** schema changes after the first release use `.sqm` migrations, which are verified in CI once a release exists.
- **No dependency yet.** SQLDelight enters the version catalog with LB-007.

## Consequences

- **The SQL is the schema.** It can be reviewed against spec §8.8 line by line, and FTS needs no workaround.
- **Typed code.** Generated Kotlin types and queries catch column and type mismatches at compile time.
- **The SQL is hand-written.** Queries, indexes and migrations must be written and tested by hand.
- **The platform's SQLite must support FTS4.** It does on Android and on iOS.

## Alternatives considered

- **Room for Kotlin Multiplatform.** Rejected:
  - Its annotated-entity model maps less directly to the SQL-first schema and FTS table of spec §8.8.
  - It needs KSP on every target, which adds build time to the iOS framework build.
  - Its Kotlin/Native support is younger.
  - Revisit it if SQLDelight's maintenance declines.
- **Realm Kotlin.** Rejected: MongoDB deprecated the Atlas Device SDKs in 2024, and Realm has no SQL or FTS.
- **JSON files or key-value storage.** Rejected: no queries, no full-text search, and no atomic multi-table replacement.
