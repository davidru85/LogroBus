# ADR-0015: kotlin.time for instants, kotlinx-datetime for the Madrid time zone

| Field | Value |
| --- | --- |
| Status | Proposed |
| Date | 2026-10-26 |
| Deciders | Project owner (spec §8.3 baseline, 2026-10-22). Written up by the agent for LB-014; awaiting acceptance |
| Related | NFR-MNT-03, DD-05, FR-ERR-06, spec §8.3, §8.6, AGENTS.md §5.5 |

## Context

AGENTS.md §5.5 sets the time rules:
- store instants in UTC;
- take "now" only from `AppClock`;
- always display `Europe/Madrid`, whatever the device time zone.

DD-05 reinterprets vehicle next-stop times, which are Madrid local time mislabelled as `Z`. That needs time-zone rules with daylight saving, tested across the CET and CEST transitions.

Since Kotlin 2.1.20 the standard library has `kotlin.time.Instant` and `Clock`, and in Kotlin 2.4 they need no opt-in: LB-004 builds `AppClock` on them. kotlinx-datetime 0.7 adopted the standard-library `Instant`.

## Decision

- **Instants:** `kotlin.time.Instant`, `Duration` and `Clock` from the standard library, everywhere. `AppClock` (LB-004) is the only source of "now".
- **Time zones and calendars:** kotlinx-datetime, in shared code, for:
  - `TimeZone.of("Europe/Madrid")`;
  - `LocalDateTime` and `LocalDate` conversions;
  - the DD-05 reinterpretation.

  It is not used for user-visible formatting, which stays on each platform with its locale (AGENTS.md §5.4).
- **No dependency yet.** kotlinx-datetime enters the version catalog with the first story that converts to Madrid time (LB-006, the VM mapper for DD-05).

## Consequences

- **One implementation of the Madrid rules,** tested once on both platforms, with tests for the CET and CEST transitions (last Sundays of March and October).
- **The time-zone database comes from the platform.** kotlinx-datetime uses `java.time` on the JVM and the system time-zone database on iOS, so a future change to Spanish daylight-saving rules depends on OS updates.
- **No second `Instant` type,** because instants come from the standard library.

## Alternatives considered

- **`expect`/`actual` over `java.time` and `NSTimeZone`/`NSCalendar`.** Rejected: the DD-05 logic and its daylight-saving tests would exist twice. kotlinx-datetime already wraps both platform APIs.
- **Fixed offsets** (UTC+1 in winter, UTC+2 in summer, hard-coded). Rejected: they are wrong around the transitions, which DD-05 must handle, and they duplicate the time-zone database.
