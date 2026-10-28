package com.ruizurraca.logrobus.transit.domain

/**
 * Spec §8.5: the static datasets the MVP caches and refreshes by sync (ADR-0008). Fares, recharge points, news and
 * CMS pages join with v1.0.
 */
enum class StaticDataset {
    /** Lines with their patterns and shapes. */
    LINES,
    STOPS,
}
