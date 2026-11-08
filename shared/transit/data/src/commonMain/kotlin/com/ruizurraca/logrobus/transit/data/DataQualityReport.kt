package com.ruizurraca.logrobus.transit.data

/**
 * What one normalisation cost (spec §5.4, step 8; `api_spec.md` §6): the items it dropped and the ones it repaired,
 * by cause, so a broken source is visible without keeping a single one of its values (NFR-PRIV-02).
 *
 * A plain count holder: no formatting, no derived text, no accessor of a run. An item is counted once, under the first
 * cause that applies, in the order of the pipeline: no id (step 3), no name (step 2), no position, outside the service
 * area (step 7), a repeated id.
 */
internal data class DataQualityReport(
    /** API-01's `LineDto` items `decodeEach` dropped on its own, never the whole response (NFR-REL-02). */
    val linesDroppedOnDecode: Int = 0,
    /** API-02's `StopDto` items `decodeEach` dropped. */
    val stopsDroppedOnDecode: Int = 0,
    /** Spec §5.4, step 3; DD-01, DD-02 and DD-06: lines, stops and pattern stops whose id is missing or unusable. */
    val droppedWithoutId: Int = 0,
    /** Spec §5.4, step 2; DD-07 and DD-11: lines and stops whose name is missing or blank after the overrides. */
    val droppedWithoutName: Int = 0,
    /** Spec §5.4, step 7: stops, and the pattern stops that would have made a provisional one, with no position. */
    val droppedWithoutPosition: Int = 0,
    /** Spec §5.4, step 7: items whose position is outside `ServiceArea`. */
    val droppedOutsideServiceArea: Int = 0,
    /** The interface's KDoc: a repeated line or stop id, which would fail the write on every sync. */
    val droppedAsDuplicate: Int = 0,
    /** DD-10: stops synthesised from a pattern stop API-02 lacks, so that every pattern stop resolves. */
    val provisionalStopsAdded: Int = 0,
    /** A shape point without both coordinates, left out of its pattern's polyline. */
    val shapePointsDropped: Int = 0,
    /** Spec §5.5: lines whose name carries no code, named after their id instead. */
    val linesWithoutCode: Int = 0,
    /** DD-08: lines whose colour is neither `rgba()` nor `#RRGGBB`, drawn grey instead. */
    val linesWithUnreadableColour: Int = 0,
) {
    /** Everything left out of the catalogue, whatever the cause. */
    val dropped: Int
        get() =
            linesDroppedOnDecode +
                stopsDroppedOnDecode +
                droppedWithoutId +
                droppedWithoutName +
                droppedWithoutPosition +
                droppedOutsideServiceArea +
                droppedAsDuplicate
}
