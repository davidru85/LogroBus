# ADR-0018: Multiplatform Settings for small persisted preferences

| Field | Value |
| --- | --- |
| Status | Accepted (2026-10-26) |
| Date | 2026-10-26 |
| Deciders | Project owner (spec §8.3 baseline, 2026-10-22): accepted on 2026-10-26, after PR #19 merged. Written up by the agent for LB-014 |
| Related | NFR-MNT-03, NFR-PRIV-03, spec §8.3, `architecture.md` §7, ADR-0009, LB-017 |

## Context

Shared code needs to persist a handful of small, non-sensitive values, for example:
- the analytics consent state (`ConsentStore`, ADR-0009, LB-017);
- whether onboarding was shown;
- user settings such as the preferred tab behaviour.

Favourites and saved places are relational and live in SQLDelight (ADR-0013). These values are flags and short strings, and they must be readable from shared code on both platforms.

## Decision

- **Multiplatform Settings** (`com.russhwolf:multiplatform-settings`, with its coroutines extension), behind small typed stores in shared code such as `ConsentStore`.
- **Storage:** DataStore on Android and `NSUserDefaults` on iOS (`architecture.md` §7).
- **Values:** only non-sensitive ones. No secrets or tokens (`security_privacy.md`), and no coordinates or free text.
- **No dependency yet.** The library enters the version catalog with the first story that persists a setting, likely LB-017.

## Consequences

- **One small API** for a few values on both platforms, with a no-op or in-memory implementation for tests.
- **Platform storage.** The values are visible in the platform's standard storage and are not encrypted, which is acceptable for non-sensitive values only.
- **Reacting to changes** goes through the coroutines extension's flows.

## Alternatives considered

- **AndroidX DataStore for Kotlin Multiplatform, used directly.** Rejected: it writes its own file on iOS instead of using `NSUserDefaults`, and it is more ceremony than a few flags need.
- **A settings table in SQLDelight.** Rejected: schema migrations for key-value flags, and a database read for values needed at start-up.
- **`expect`/`actual` over SharedPreferences and `NSUserDefaults`.** Rejected: it re-implements the library, and its tests, by hand.
