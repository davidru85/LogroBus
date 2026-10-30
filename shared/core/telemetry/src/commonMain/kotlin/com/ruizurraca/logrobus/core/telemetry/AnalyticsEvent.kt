package com.ruizurraca.logrobus.core.telemetry

/**
 * The analytics events of the MVP (NFR-OBS-03, ADR-0009). They carry ids, counts, flags and enums only, so no event can
 * hold free text or coordinates (NFR-PRIV-02); a Konsist rule in `:tools:architecture-tests` keeps it that way.
 * `widget_add` and `notice_open` arrive with their features in v1.0.
 */
sealed interface AnalyticsEvent {
    /** `app_open`: the app came to the foreground. */
    data object AppOpen : AnalyticsEvent

    /** `stop_view`: a stop's next arrivals were shown. */
    data class StopView(
        val stopId: Int,
    ) : AnalyticsEvent

    /** `line_view`: a line's detail was shown. */
    data class LineView(
        val lineId: Int,
    ) : AnalyticsEvent

    /** `plan_request`: a journey was requested. Whether it starts at the user's location, never where that is. */
    data class PlanRequest(
        val originIsCurrentLocation: Boolean,
    ) : AnalyticsEvent

    /** `plan_result`: the planner answered with [itineraryCount] routes, and a [rejection] when there are none (DD-17). */
    data class PlanResult(
        val itineraryCount: Int,
        val rejection: Rejection?,
    ) : AnalyticsEvent {
        /** Why no route was found: the source's `rejection.reason` (`api_spec.md`, API-07), or the client's area check. */
        enum class Rejection {
            NO_ROUTE,
            WALK_TOO_FAR_ORIGIN,
            WALK_TOO_FAR_DESTINATION,

            /** FR-PLAN-10: a point outside the service area, rejected before calling. */
            OUTSIDE_SERVICE_AREA,
            OTHER,
        }
    }

    /** `favourite_add`: a line, stop or place was saved (FR-FAV-01). */
    data class FavouriteAdd(
        val kind: Kind,
    ) : AnalyticsEvent {
        enum class Kind {
            LINE,
            STOP,
            PLACE,
        }
    }
}
