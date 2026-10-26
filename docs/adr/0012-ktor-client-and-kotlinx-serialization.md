# ADR-0012: Ktor client and kotlinx.serialization for networking

| Field | Value |
| --- | --- |
| Status | Proposed |
| Date | 2026-10-26 |
| Deciders | Project owner (spec §8.3 baseline, 2026-10-22). Written up by the agent for LB-014; awaiting acceptance |
| Related | NFR-MNT-03, NFR-REL-02, spec §8.3, §8.7, `api_spec.md` §3, `architecture.md` §4.1, ADR-0001, ADR-0007 |

## Context

The data source is an undocumented, unversioned JSON API behind `https://transporteurbano.logrono.es` (`api_spec.md`).

Both apps need the same networking rules, implemented once in shared code:
- timeouts, gzip and the project `User-Agent` (spec §8.7);
- lenient decoding: per-item collections, any 2xx accepted, and a non-2xx status or a broken body treated as a recoverable error for that resource only (AGENTS.md §5.9, NFR-REL-02);
- `Date` header sampling for `AppClock` (spec §8.6).

## Decision

- **Ktor client** in `:shared:core:network`:
  - one `HttpClient` per process, on the OkHttp engine on Android and the Darwin engine (`NSURLSession`) on iOS;
  - `expectSuccess = false`, so the response mapping, not Ktor, decides what a failure is;
  - the headers and timeouts of spec §8.7;
  - a response hook that feeds every BFF `Date` header to `ServerCorrectedClock.onServerDate`.
- **kotlinx.serialization JSON**, configured as spec §8.3 states (`ignoreUnknownKeys = true`, `coerceInputValues = true`, `explicitNulls = false`).
  - Collections are decoded item by item from a `JsonArray`, so one bad item is dropped and logged, never the whole response.
- **Module rules:** Ktor is allowed only in `:shared:core:network`, in data modules and in the planner's `data` package (AGENTS.md §5.1, enforced by `ModuleRules`).
- **Tests:** they use Ktor's `MockEngine` and the golden fixtures, and never call the live source (AGENTS.md §9).

## Consequences

- **One implementation for both platforms.** Every source quirk is fixed once and tested once, on the JVM host and on the iOS simulator.
- **The engines differ underneath.** OkHttp and `NSURLSession` behave differently on proxies, ATS and caching. Engine-specific behaviour stays behind the shared client, and the iOS tests run the Darwin engine.
- **Versions are coupled.** Ktor, kotlinx.serialization and the Kotlin compiler plugin move together: bump them as a set, and check SKIE compatibility with Kotlin (ADR-0004).
- **No dependency yet.** The libraries enter `gradle/libs.versions.toml` with LB-005, the first story that makes a request.

## Alternatives considered

- **Platform clients behind a shared interface** (OkHttp or Retrofit on Android, `URLSession` on iOS). Rejected: the lenient decoding, the headers, the `Date` sampling and their tests would exist twice, in two languages. That breaks the rule that every source quirk is fixed exactly once (AGENTS.md §5.2).
- **Ktorfit** (Retrofit-style annotations over Ktor). Rejected: it adds code generation (KSP on every target) for about a dozen endpoints. The per-item lenient decoding needs hand-written handling anyway.
- **JVM JSON libraries** (Moshi, Gson). Rejected: they do not run on Kotlin/Native.
