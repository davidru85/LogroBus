# ADR-0009: Telemetry contracts now, vendor later

| Field | Value |
| --- | --- |
| Status | Accepted |
| Date | 2026-10-22 |
| Deciders | Project owner |
| Related | OQ-16, NFR-OBS-01 to 03, NFR-PRIV-01 to 03, `security_privacy.md`, backlog LB-017, LB-409 |

## Context

The owner wants metrics (likely Firebase Analytics) and crash reporting (likely Crashlytics), but the vendor is not decided yet. The app code must not wait for that decision, and it must not depend on any vendor SDK.

## Decision

- **Contracts.** `:shared:core:telemetry` defines the vendor-neutral contracts:

  ```kotlin
  interface AnalyticsTracker { fun track(event: AnalyticsEvent) }

  sealed interface AnalyticsEvent {            // NFR-OBS-03 catalogue: ids and enums only
      data object AppOpen : AnalyticsEvent
      data class StopView(val stopId: Int) : AnalyticsEvent
      data class LineView(val lineId: Int) : AnalyticsEvent
      data class PlanRequest(val originIsCurrentLocation: Boolean) : AnalyticsEvent
      data class PlanResult(val itineraryCount: Int, val rejectionReason: PlanRejection?) : AnalyticsEvent
      data class FavouriteAdd(val type: FavouriteType) : AnalyticsEvent
      // v1.0: WidgetAdd, NoticeOpen
  }

  interface CrashReporter {
      fun recordNonFatal(error: Throwable, area: CrashArea)
      fun breadcrumb(crumb: Breadcrumb)          // enum-based, no free text
  }

  interface ConsentStore { val consent: StateFlow<TelemetryConsent> }  // NFR-PRIV-03
  ```

- **Privacy by type design.** Events carry only ids, enums, counts and booleans. The types make it impossible to send coordinates or free text (NFR-PRIV-02, NFR-OBS-03).
- **Consent gate.** Shared wrappers `ConsentGatedAnalyticsTracker` and `ConsentGatedCrashReporter` drop everything until the user has consented.
- **MVP implementations.** `NoOp*` in release builds, and `Logging*` (console) in debug builds.
- **Vendor implementations** are native, chosen in LB-409:
  - Android: a Kotlin class in the app module.
  - iOS: a Swift class conforming to the exported Kotlin protocol.
  Either is injected into Koin at startup. No vendor SDK is referenced from shared code.
  - *Update, 2026-10-30 (LB-017):* as built, the enums of the snippet above are nested in their events (`PlanResult.Rejection`, `FavouriteAdd.Kind`), because a core module cannot use the planner and favourites domains and nested names cannot clash in Swift. `Breadcrumb` is a sealed type whose first case is `ScreenView(Breadcrumb.Screen)`, and `recordNonFatal` takes a `CrashArea`. The injection point is a `PlatformTelemetry(analytics, crashReporter)` passed to `initKoin` on both platforms, and the gates wrap it as they wrap the build's implementations. A Konsist rule allows only ids, counts, flags and telemetry enums in events and breadcrumbs, and `ModuleRules` rejects vendor SDKs outside `:android:app` (`architecture.md` §4.7).

## Consequences

- Features can emit events and record non-fatals from the first story. Choosing the vendor later changes only the platform implementations and the DI wiring.
- The consent dialog and the settings toggles (LB-409) still wait for the vendor decision. The store privacy declarations (LB-414) must reflect the final vendor.

## Alternatives considered

- **Add Firebase now.** Commits to a vendor before the owner has decided, and puts personal-data processing in the MVP prematurely.
- **No telemetry abstraction.** Vendor calls would spread through the code and be hard to gate by consent.
