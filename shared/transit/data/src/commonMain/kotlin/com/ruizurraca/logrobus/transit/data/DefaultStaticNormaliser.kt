package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.color.onColorFor
import com.ruizurraca.logrobus.core.common.color.parseCssColor
import com.ruizurraca.logrobus.core.common.geo.GeoPoint
import com.ruizurraca.logrobus.core.common.geo.Polyline
import com.ruizurraca.logrobus.core.common.warn
import com.ruizurraca.logrobus.core.network.Decoded
import com.ruizurraca.logrobus.core.network.IdParser
import com.ruizurraca.logrobus.transit.data.dto.LatLngDto
import com.ruizurraca.logrobus.transit.data.dto.LineDto
import com.ruizurraca.logrobus.transit.data.dto.LineStopDto
import com.ruizurraca.logrobus.transit.data.dto.LineStopsDto
import com.ruizurraca.logrobus.transit.data.dto.StopDto
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.LineKind
import kotlinx.serialization.json.JsonElement

private const val TAG = "StaticNormaliser"

// api_spec.md §3: the key of each collection inside its envelope.
private const val LINES = "lines"
private const val STOPS = "stops"

// Spec §8.8: pattern shapes are stored at precision 6.
private const val SHAPE_PRECISION = 6

// API-01 and API-02 send status 1 for a line or a stop in service (OQ-02).
private const val OPERATIONAL = 1

// DD-08: `parseCssColor` keeps the alpha it is given, and a line badge is opaque, so it is forced here. A colour that
// is neither `rgba()` nor `#RRGGBB`, and a colour that is missing, become grey: a badge, never a hole in the list.
private const val OPAQUE = 0xFF000000L
private const val UNREADABLE_COLOUR = 0xFF9E9E9EL

// The normaliser's own version, which the fingerprint carries.
private const val VERSION = 1

/**
 * The normaliser of architecture.md §4.4 over API-01's lines and API-02's stops (spec §5.4, `api_spec.md` §6).
 *
 * It fixes the source's quirks and never invents ids beyond the ones the payloads carry: an item whose id, name or
 * position is unusable, one outside the [ServiceArea], and a repeated line or stop id are each dropped and counted in
 * the [DataQualityReport], because a dropped item costs that item alone (NFR-REL-02). Only a missing envelope, or a
 * collection that is not an array, fails the whole normalisation with `AppError.Data`; item damage never does.
 *
 * DD-10: every pattern stop API-02 lacks becomes a provisional stop, taken from the pattern entry, so that no pattern
 * points at nothing.
 *
 * The name overrides are taken here as a map, not as a ready-made [NameNormaliser], so that [fingerprint] can only
 * ever describe the overrides that are actually applied (DD-11, spec §8.5).
 */
internal class DefaultStaticNormaliser(
    nameOverrides: Map<String, String>,
    private val logger: Logger,
) : StaticNormaliser {
    private val names = NameNormaliser(nameOverrides)

    /**
     * The normaliser's version and a digest of its sorted name overrides: new overrides or a new version change it, and
     * the sync then renormalises an unchanged payload (spec §8.5). Sorting makes the same set of overrides hash the
     * same whatever order it arrived in.
     */
    override val fingerprint: String = fingerprintOf(nameOverrides)

    override fun normalise(
        lines: JsonElement,
        stops: JsonElement,
    ): AppResult<StaticCatalogue> =
        normaliseWithReport(lines, stops).andThen { (catalogue, _) -> AppResult.Success(catalogue) }

    /**
     * [normalise] and what it cost: the [DataQualityReport] of spec §5.4, step 8. A pure function of its arguments, so
     * every caller reads its own report and never another's, which a last-run accessor would not give.
     */
    fun normaliseWithReport(
        lines: JsonElement,
        stops: JsonElement,
    ): AppResult<Pair<StaticCatalogue, DataQualityReport>> =
        AppResult
            .Success(lines)
            .decodeCollection(LINES, LineDto.serializer(), logger)
            .andThen { lineDtos ->
                AppResult.Success(stops).decodeCollection(STOPS, StopDto.serializer(), logger).andThen { stopDtos ->
                    normalised(lineDtos, stopDtos)
                }
            }

    private fun normalised(
        lineDtos: Decoded<LineDto>,
        stopDtos: Decoded<StopDto>,
    ): AppResult<Pair<StaticCatalogue, DataQualityReport>> {
        val tally = Tally(lineDtos.dropped, stopDtos.dropped)
        // Every pattern entry that was kept, by stop id: what a provisional stop is made from (DD-10), and where an
        // outbound pattern's termini come from (spec §5.5). Filled while the lines are read, used once the stops are.
        val references = LinkedHashMap<Int, PatternReference>()
        val seen = HashSet<Int>(lineDtos.items.size)
        val lines = lineDtos.items.mapNotNull { lineOf(it, references, seen, tally) }.sortedBy { it.id }
        val stops = stopsOf(stopDtos.items, references, tally)
        val catalogue = StaticCatalogue(withOnlyKnownStops(lines, stops.mapTo(HashSet()) { it.id }), stops)
        return AppResult.Success(catalogue to tally.report())
    }

    // Spec §5.4: the causes are checked in pipeline order, and a damaged line leaves the catalogue, not the run.
    private fun lineOf(
        dto: LineDto,
        references: MutableMap<Int, PatternReference>,
        seen: MutableSet<Int>,
        tally: Tally,
    ): CatalogueLine? {
        val id = IdParser.lineId(dto.id?.toString())
        val name = names.normalise(dto.name)
        return when {
            id == null -> dropped(tally, Cause.NO_ID, "line", null, logger)
            name == null -> dropped(tally, Cause.NO_NAME, "line", id, logger)
            !seen.add(id) -> dropped(tally, Cause.DUPLICATE, "line", id, logger)
            else -> built(id, name, dto, references, tally)
        }
    }

    private fun built(
        id: Int,
        name: String,
        dto: LineDto,
        references: MutableMap<Int, PatternReference>,
        tally: Tally,
    ): CatalogueLine {
        // DD-07: `rawName` is the source's own string, trimmed, and `displayName` the one the app shows (spec §5.5).
        val patterns = patternsOf(id, name, dto.stops, references, tally)
        // Spec §5.5: when a terminus itself holds a hyphen the name cannot be split, so the outbound pattern's own
        // termini are the display name's.
        val parsed = LineNameParser.parse(name, terminiOf(patterns[Direction.OUTBOUND], references))
        if (parsed == null) {
            // A fallback, and counted: the id stands in for a code, and the name as delivered for a display name.
            logger.warn(TAG, "Line $id has no code in its name; its id stands in")
            tally.linesWithoutCode++
        }
        val read = parseCssColor(dto.color)
        if (read == null) tally.linesWithUnreadableColour++
        val color = (read ?: UNREADABLE_COLOUR) or OPAQUE
        return CatalogueLine(
            id = id,
            code = parsed?.code ?: id.toString(),
            rawName = dto.name?.trim() ?: name,
            displayName = parsed?.displayName ?: name,
            color = color,
            onColor = onColorFor(color),
            kind = parsed?.kind ?: LineKind.DAY,
            isOperational = dto.status == OPERATIONAL,
            patterns = patterns,
        )
    }

    // DD-04: API-01's keys are the directions, `asc` outbound and `desc` inbound.
    private fun patternsOf(
        lineId: Int,
        headsign: String,
        stops: LineStopsDto?,
        references: MutableMap<Int, PatternReference>,
        tally: Tally,
    ): Map<Direction, CataloguePattern> {
        if (stops == null) return emptyMap()
        val patterns = mutableMapOf<Direction, CataloguePattern>()
        listOf(Direction.OUTBOUND to stops.asc, Direction.INBOUND to stops.desc).forEach { (direction, lineStops) ->
            patternOf(lineId, headsign, lineStops, references, tally)?.let { patterns[direction] = it }
        }
        return patterns
    }

    /**
     * One direction's pattern: its stops in sequence, its headsign, and the shape their `onwardShape` segments make
     * (spec §4.1). A stop's `shapeIndex` is how many points of the shape came before its own segment, so a dropped
     * stop takes its segment with it and the indexes stay in the kept points.
     */
    private fun patternOf(
        lineId: Int,
        fallbackHeadsign: String,
        lineStops: List<LineStopDto>,
        references: MutableMap<Int, PatternReference>,
        tally: Tally,
    ): CataloguePattern? {
        val shape = mutableListOf<GeoPoint>()
        val patternStops = mutableListOf<CataloguePatternStop>()
        var lastName: String? = null
        lineStops.forEach { stop ->
            val id = IdParser.stopId(stop.id?.toString())
            val lat = stop.lat
            val lng = stop.lng
            when {
                id == null -> {
                    dropped(tally, Cause.NO_ID, "pattern stop", null, logger)
                }

                lat == null || lng == null -> {
                    dropped(tally, Cause.NO_POSITION, "pattern stop", id, logger)
                }

                !ServiceArea.contains(lat, lng) -> {
                    dropped(tally, Cause.OUTSIDE_AREA, "pattern stop", id, logger)
                }

                else -> {
                    patternStops += CataloguePatternStop(stopId = id, shapeIndex = shape.size)
                    val name = names.normalise(stop.name)
                    lastName = name ?: lastName
                    references.referenced(id, PatternReference(name, lat, lng, stop.status == OPERATIONAL), lineId)
                    tally.shapePointsDropped += stop.onwardShape.appendTo(shape)
                }
            }
        }
        if (patternStops.isEmpty()) return null
        return CataloguePattern(
            headsign = lastName ?: fallbackHeadsign,
            shapePolyline = Polyline.encode(shape, SHAPE_PRECISION),
            stops = patternStops,
        )
    }

    private fun stopsOf(
        dtos: List<StopDto>,
        references: Map<Int, PatternReference>,
        tally: Tally,
    ): List<CatalogueStop> {
        val stops = LinkedHashMap<Int, CatalogueStop>(dtos.size)
        dtos.forEach { dto ->
            val id = IdParser.stopId(dto.id?.toString())
            val name = names.normalise(dto.name)
            val lat = dto.lat
            val lng = dto.lng
            when {
                id == null -> {
                    dropped(tally, Cause.NO_ID, "stop", null, logger)
                }

                name == null -> {
                    dropped(tally, Cause.NO_NAME, "stop", id, logger)
                }

                lat == null || lng == null -> {
                    dropped(tally, Cause.NO_POSITION, "stop", id, logger)
                }

                !ServiceArea.contains(lat, lng) -> {
                    dropped(tally, Cause.OUTSIDE_AREA, "stop", id, logger)
                }

                stops.containsKey(id) -> {
                    dropped(tally, Cause.DUPLICATE, "stop", id, logger)
                }

                else -> {
                    stops[id] =
                        CatalogueStop(
                            id = id,
                            name = name,
                            lat = lat,
                            lng = lng,
                            isOperational = dto.status == OPERATIONAL,
                            isProvisional = false,
                            lineIds = dto.lines.toSet(),
                        )
                }
            }
        }
        addProvisional(references, stops, tally)
        return stops.values.sortedBy { it.id }
    }

    // DD-10: a pattern stop that API-02 lacks, or that came out of it damaged, becomes a stop of its own, so that every
    // pattern stop resolves (StaticNormaliser's KDoc).
    private fun addProvisional(
        references: Map<Int, PatternReference>,
        stops: MutableMap<Int, CatalogueStop>,
        tally: Tally,
    ) {
        references.forEach { (id, reference) ->
            if (id in stops) return@forEach
            val name = reference.name
            if (name == null) {
                dropped(tally, Cause.NO_NAME, "provisional stop", id, logger)
            } else {
                stops[id] =
                    CatalogueStop(
                        id = id,
                        name = name,
                        lat = reference.lat,
                        lng = reference.lng,
                        isOperational = reference.isOperational,
                        isProvisional = true,
                        lineIds = reference.lineIds.toSet(),
                    )
                tally.provisionalStopsAdded++
            }
        }
    }

    private fun terminiOf(
        pattern: CataloguePattern?,
        references: Map<Int, PatternReference>,
    ): Pair<String, String>? {
        val stops = pattern?.stops.orEmpty()
        val first = stops.firstOrNull()?.let { references[it.stopId]?.name }
        val last = stops.lastOrNull()?.let { references[it.stopId]?.name }
        return if (first != null && last != null) first to last else null
    }
}

/** [next] on a success's value; a failure passes through. */
private inline fun <T, R> AppResult<T>.andThen(next: (T) -> AppResult<R>): AppResult<R> =
    when (this) {
        is AppResult.Success -> next(value)
        is AppResult.Failure -> this
    }

/**
 * [lines] without the pattern stops that have no stop, and without a pattern emptied that way: the sync rejects a
 * catalogue whose pattern stop resolves to nothing (StaticNormaliser's KDoc). A pattern's shape stays whole, since a
 * shape points at no stop.
 */
private fun withOnlyKnownStops(
    lines: List<CatalogueLine>,
    known: Set<Int>,
): List<CatalogueLine> =
    lines.map { line ->
        val patterns =
            line.patterns
                .mapValues { (_, pattern) -> pattern.copy(stops = pattern.stops.filter { it.stopId in known }) }
                .filterValues { it.stops.isNotEmpty() }
        line.copy(patterns = patterns)
    }

// The normaliser's version and a digest of its sorted name overrides (DD-11): what the sync folds into the content hash
// of spec §8.5, so that new overrides renormalise an unchanged payload.
private fun fingerprintOf(nameOverrides: Map<String, String>): String {
    val overrides = nameOverrides.entries.sortedBy { it.key }.joinToString("\n") { "${it.key}=${it.value}" }
    return "v$VERSION-${sha256Hex(overrides)}"
}

// NFR-PRIV-02: an item's type, its id when it has one, and the cause; never a name, a position or another value.
private fun dropped(
    tally: Tally,
    cause: Cause,
    type: String,
    id: Int?,
    logger: Logger,
): Nothing? {
    tally.count(cause)
    logger.warn(TAG, "Dropped ${if (id == null) "a $type" else "$type $id"}: ${cause.message}")
    return null
}

/** The shape points of one stop's segment, appended to [points]; how many lacked a coordinate, which are counted. */
private fun List<LatLngDto>.appendTo(points: MutableList<GeoPoint>): Int {
    var unreadable = 0
    forEach { point ->
        val lat = point.lat
        val lng = point.lng
        if (lat == null || lng == null) unreadable++ else points += GeoPoint(lat, lng)
    }
    return unreadable
}

/** One pattern stop's values, which a provisional stop is made from (DD-10), and the lines whose patterns have it. */
private class PatternReference(
    var name: String?,
    val lat: Double,
    val lng: Double,
    val isOperational: Boolean,
) {
    val lineIds = linkedSetOf<Int>()
}

/** Records one pattern stop: the first entry supplies a provisional stop's values, and every pattern adds its line. */
private fun MutableMap<Int, PatternReference>.referenced(
    id: Int,
    found: PatternReference,
    lineId: Int,
) {
    val reference = getOrPut(id) { found }
    if (reference.name == null) reference.name = found.name
    reference.lineIds += lineId
}

/** Why an item left the catalogue (spec §5.4), and the one count of the [DataQualityReport] it feeds. */
private enum class Cause(
    val message: String,
) {
    NO_ID("no usable id"),
    NO_NAME("no usable name"),
    NO_POSITION("no position"),
    OUTSIDE_AREA("outside the service area"),
    DUPLICATE("a repeated id"),
}

/** One run's counts, gathered by cause and turned into a [DataQualityReport] at the end. */
private class Tally(
    private val linesDroppedOnDecode: Int,
    private val stopsDroppedOnDecode: Int,
) {
    private val causes = mutableMapOf<Cause, Int>()
    var provisionalStopsAdded = 0
    var shapePointsDropped = 0
    var linesWithoutCode = 0
    var linesWithUnreadableColour = 0

    fun count(cause: Cause) {
        causes[cause] = (causes[cause] ?: 0) + 1
    }

    fun report(): DataQualityReport =
        DataQualityReport(
            linesDroppedOnDecode = linesDroppedOnDecode,
            stopsDroppedOnDecode = stopsDroppedOnDecode,
            droppedWithoutId = causes[Cause.NO_ID] ?: 0,
            droppedWithoutName = causes[Cause.NO_NAME] ?: 0,
            droppedWithoutPosition = causes[Cause.NO_POSITION] ?: 0,
            droppedOutsideServiceArea = causes[Cause.OUTSIDE_AREA] ?: 0,
            droppedAsDuplicate = causes[Cause.DUPLICATE] ?: 0,
            provisionalStopsAdded = provisionalStopsAdded,
            shapePointsDropped = shapePointsDropped,
            linesWithoutCode = linesWithoutCode,
            linesWithUnreadableColour = linesWithUnreadableColour,
        )
}
