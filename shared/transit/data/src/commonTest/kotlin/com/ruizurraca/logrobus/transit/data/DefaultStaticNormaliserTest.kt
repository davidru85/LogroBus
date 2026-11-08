package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.geo.Polyline
import com.ruizurraca.logrobus.core.testing.FixtureLoader
import com.ruizurraca.logrobus.transit.domain.Direction
import com.ruizurraca.logrobus.transit.domain.LineKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/** A catalogue and what the run that produced it cost (spec §5.4, step 8). */
private typealias Normalised = Pair<StaticCatalogue, DataQualityReport>

/**
 * Spec §5.4 and `api_spec.md` §6, against the captures of LB-003: the two static payloads normalise to the catalogue
 * of spec §3, DD-10's three provisional stops appear, and every damaged item costs itself alone.
 */
class DefaultStaticNormaliserTest {
    private val logger = RecordingLogger()
    private val lines = fixture("lines_2026-10-22.json")
    private val stops = fixture("stops_2026-10-22.json")

    private fun fixture(name: String): JsonElement = Json.parseToJsonElement(FixtureLoader.read(name))

    private fun normaliser(overrides: Map<String, String> = mapOf("PRADOVIEJOO" to "PRADOVIEJO")) =
        DefaultStaticNormaliser(overrides, logger)

    private fun AppResult<Normalised>.succeeded(): Normalised =
        when (this) {
            is AppResult.Success -> value
            is AppResult.Failure -> fail("Expected a success, got $error")
        }

    private fun <T> AppResult<T>.failed(): AppError =
        when (this) {
            is AppResult.Success -> fail("Expected a failure, got $value")
            is AppResult.Failure -> error
        }

    private fun AppResult<StaticCatalogue>.catalogue(): StaticCatalogue =
        when (this) {
            is AppResult.Success -> value
            is AppResult.Failure -> fail("Expected a success, got $error")
        }

    /** The envelope of api_spec.md §3 around [lines] and [stops], as bare arrays of items. */
    private fun payload(
        lines: String = "[]",
        stops: String = "[]",
    ): Pair<JsonElement, JsonElement> =
        Json.parseToJsonElement("""{"result":{"lines":$lines}}""") to
            Json.parseToJsonElement("""{"result":{"stops":$stops}}""")

    @Test
    fun lb006_theTwoFixturesNormaliseToACompleteCatalogue() {
        val (catalogue, report) = normaliser().normaliseWithReport(lines, stops).succeeded()

        // api_spec.md §9: 13 lines and 254 stops on 2026-10-22, and DD-10 adds the 3 pattern stops API-02 lacks.
        assertEquals(13, catalogue.lines.size)
        assertEquals(254, catalogue.stops.count { !it.isProvisional })
        assertEquals(257, catalogue.stops.size)
        // Nothing else was dropped or repaired: these two payloads are clean but for DD-10.
        assertEquals(DataQualityReport(provisionalStopsAdded = 3), report)

        // DD-04: every line of the capture serves both directions.
        catalogue.lines.forEach { line ->
            assertEquals(setOf(Direction.OUTBOUND, Direction.INBOUND), line.patterns.keys, "Line ${line.id}")
        }
        // Spec §5.5: line 1's code, display name and kind.
        val line1 = catalogue.lines.single { it.id == 1 }
        assertEquals("1", line1.code)
        assertEquals("Lardero - Hospital San Pedro", line1.displayName)
        assertEquals(LineKind.DAY, line1.kind)
        // Its patterns' headsigns are their own last stops (spec §1.6).
        assertEquals("Iglesia de Lardero", line1.patterns.getValue(Direction.OUTBOUND).headsign)
        assertEquals("Puente Madre-Hospital", line1.patterns.getValue(Direction.INBOUND).headsign)
        // Every pattern stop resolves to a stop, which the sync requires (StaticNormaliser's KDoc).
        val stopIds = catalogue.stops.mapTo(HashSet()) { it.id }
        catalogue.lines.forEach { line ->
            line.patterns.values.forEach { pattern ->
                assertTrue(pattern.stops.all { it.stopId in stopIds }, "Line ${line.id}")
            }
        }
        // And normalise is the same run, without the report.
        assertEquals(catalogue, normaliser().normalise(lines, stops).catalogue())
    }

    @Test
    fun dd10_aPatternStopThatApi02LacksBecomesAProvisionalStop() {
        val (catalogue, report) = normaliser().normaliseWithReport(lines, stops).succeeded()

        // jq: both directions carry 257 distinct pattern stop ids, and API-02 carries exactly three fewer.
        val provisional = catalogue.stops.filter { it.isProvisional }.associateBy { it.id }
        assertEquals(setOf(383, 384, 385), provisional.keys)
        assertEquals(3, report.provisionalStopsAdded)

        // Each takes its name and position from the pattern entry; DD-11 trims the trailing space of 385's.
        assertEquals("Provisional Templarios", provisional.getValue(383).name)
        assertEquals(42.431697, provisional.getValue(383).lat)
        assertEquals(-2.417535, provisional.getValue(383).lng)
        assertEquals("Provisional Cameros", provisional.getValue(384).name)
        assertEquals("Provisional Templarios", provisional.getValue(385).name)
        // Line 3 is the line whose patterns carry all three, so each is linked to it.
        assertEquals(setOf(3), provisional.getValue(383).lineIds)
        assertEquals(setOf(3), provisional.getValue(385).lineIds)
        assertTrue(provisional.values.none { it.lineIds.isEmpty() })
    }

    @Test
    fun lb006_anItemOutsideTheServiceAreaIsDroppedAndCounted() {
        val (linesBody, stopsBody) =
            payload(
                lines = "[${line(id = 1)}]",
                stops = "[${stop(id = 38)}, ${stop(id = 900, lat = 0.0, lng = 0.0)}]",
            )

        val (catalogue, report) = normaliser().normaliseWithReport(linesBody, stopsBody).succeeded()

        // 0,0 is in the Gulf of Guinea; stop 39 is only in a pattern, so DD-10 makes it.
        assertEquals(listOf(38, 39), catalogue.stops.map { it.id })
        assertEquals(1, report.droppedOutsideServiceArea)
        assertEquals(1, report.dropped)
        assertEquals(1, report.provisionalStopsAdded)
    }

    @Test
    fun lb006_aRepeatedIdIsKeptOnceAndCounted() {
        val (linesBody, stopsBody) =
            payload(
                lines = "[${line(id = 1)}, ${line(id = 1)}]",
                stops = "[${stop(id = 38)}, ${stop(id = 38, name = "Otro nombre")}]",
            )

        val (catalogue, report) = normaliser().normaliseWithReport(linesBody, stopsBody).succeeded()

        // The first of each wins, so the write can never fail on a primary key.
        assertEquals(listOf(1), catalogue.lines.map { it.id })
        assertEquals(38, catalogue.stops.first().id)
        assertEquals("Iglesia de Lardero", catalogue.stops.first().name)
        assertEquals(2, report.droppedAsDuplicate)
        assertEquals(2, report.dropped)
        // NFR-PRIV-02: the log names the type and the cause, never the dropped item's values.
        assertTrue(logger.entries.none { it.message.contains("Otro nombre") })
        assertTrue(logger.entries.none { it.message.contains("TERMINUS") })
    }

    @Test
    fun lb006_anItemWithoutAnIdIsDroppedAndCounted() {
        // IdParser takes whole digits only (DD-01, DD-02, DD-06): a sign is not one.
        val (linesBody, stopsBody) =
            payload(
                lines = "[${line(id = 1)}, ${line(id = -7)}]",
                stops = "[${stop(id = 38)}, ${stop(id = -9)}]",
            )

        val (catalogue, report) = normaliser().normaliseWithReport(linesBody, stopsBody).succeeded()

        assertEquals(listOf(1), catalogue.lines.map { it.id })
        assertEquals(listOf(38, 39), catalogue.stops.map { it.id })
        assertEquals(2, report.droppedWithoutId)
        assertEquals(2, report.dropped)
    }

    @Test
    fun lb006_anItemWithoutAPositionIsDroppedAndCounted() {
        // A stop API-02 sends no position for still comes back from the pattern that carries it (DD-10).
        val (linesBody, stopsBody) =
            payload(
                lines = "[${line(id = 1)}]",
                stops = """[${stop(id = 38)}, {"id":39,"name":"Sin posición","lines":[1],"status":1}]""",
            )

        val (catalogue, report) = normaliser().normaliseWithReport(linesBody, stopsBody).succeeded()

        assertEquals(listOf(38, 39), catalogue.stops.map { it.id })
        assertEquals(1, report.droppedWithoutPosition)
        assertEquals(1, report.dropped)
        assertEquals(1, report.provisionalStopsAdded)
        // The provisional one keeps the pattern's own position.
        assertEquals(42.426317, catalogue.stops.last().lat)
    }

    @Test
    fun lb006_aStopOrALineThatCannotBeDecodedCostsOnlyItself() {
        // An array where an object belongs cannot be coerced into a DTO, so decodeEach drops that item alone.
        val (linesBody, stopsBody) =
            payload(
                lines = "[${line(id = 1)}, [1, 2]]",
                stops = "[${stop(id = 38)}, \"nope\"]",
            )

        val (catalogue, report) = normaliser().normaliseWithReport(linesBody, stopsBody).succeeded()

        assertEquals(1, report.linesDroppedOnDecode)
        assertEquals(1, report.stopsDroppedOnDecode)
        assertEquals(listOf(1), catalogue.lines.map { it.id })
        assertEquals(listOf(38, 39), catalogue.stops.map { it.id })
        assertEquals(2, report.dropped)
    }

    @Test
    fun lb006_anItemWithoutANameIsDroppedAndCounted() {
        // DD-07: `""` means no value, and after the DD-11 overrides a blank name is still no name.
        val (linesBody, stopsBody) =
            payload(
                lines = """[${line(id = 1)}, {"id":2,"name":"  "}]""",
                stops = """[${stop(id = 38)}, {"id":40,"name":"","lat":42.43,"lng":-2.46,"lines":[1],"status":1}]""",
            )

        val (catalogue, report) = normaliser().normaliseWithReport(linesBody, stopsBody).succeeded()

        assertEquals(listOf(1), catalogue.lines.map { it.id })
        // Stop 40 is in no pattern, so nothing brings it back.
        assertEquals(listOf(38, 39), catalogue.stops.map { it.id })
        assertEquals(2, report.droppedWithoutName)
        assertEquals(2, report.dropped)
    }

    @Test
    fun lb006_anUnusableEnvelopeOrCollectionIsADataFailure() {
        val (_, stopsBody) = payload()
        val notAnArray = Json.parseToJsonElement("""{"result":{"lines":"nope"}}""")
        val noEnvelope = Json.parseToJsonElement("{}")

        assertEquals(AppError.Data, normaliser().normalise(notAnArray, stopsBody).failed())
        assertEquals(AppError.Data, normaliser().normalise(noEnvelope, stopsBody).failed())
        assertEquals(AppError.Data, normaliser().normaliseWithReport(noEnvelope, stopsBody).failed())
    }

    @Test
    fun dd08_aLineColourComesFromItsRgbaValue() {
        val (catalogue, report) = normaliser().normaliseWithReport(lines, stops).succeeded()

        // API-01 writes rgba(15,220,252,1) for line 10, and the badge is opaque ARGB (DD-08).
        val line10 = catalogue.lines.single { it.id == 10 }
        assertEquals(0xFF0FDCFCL, line10.color)
        // NFR-A11Y-06: black contrasts more on that cyan than white.
        assertEquals(0xFF000000L, line10.onColor)
        // Line 2 is yellow and line 9 is blue, so their text colours differ.
        assertEquals(0xFF000000L, catalogue.lines.single { it.id == 2 }.onColor)
        assertEquals(0xFFFFFFFFL, catalogue.lines.single { it.id == 9 }.onColor)
        assertEquals(0, report.linesWithUnreadableColour)
    }

    @Test
    fun lb006_thePatternShapeIsTheConcatenationOfItsSegments() {
        val (catalogue, _) = normaliser().normaliseWithReport(lines, stops).succeeded()

        // jq: the outbound segments of the capture hold 3438 points in all, and the inbound ones 3359.
        val outbound = catalogue.lines.sumOf { shapeOf(it, Direction.OUTBOUND).size }
        val inbound = catalogue.lines.sumOf { shapeOf(it, Direction.INBOUND).size }
        assertEquals(3438, outbound)
        assertEquals(3359, inbound)

        // Line 3 outbound: 482 points, each segment starting where the previous ones ended (jq: 15 + 8 + … + 61).
        val line3 =
            catalogue.lines
                .single { it.id == 3 }
                .patterns
                .getValue(Direction.OUTBOUND)
        assertEquals(482, Polyline.decode(line3.shapePolyline, 6).size)
        assertEquals(0, line3.stops.first().shapeIndex)
        // The last stop's onwardShape is empty, so its segment starts at the end of the shape.
        assertEquals(482, line3.stops.last().shapeIndex)
        assertTrue(line3.stops.zipWithNext().all { (previous, next) -> previous.shapeIndex < next.shapeIndex })
    }

    @Test
    fun lb006_theFingerprintChangesWithTheNameOverrides() {
        val withOverride = normaliser()

        assertNotEquals(withOverride.fingerprint, normaliser(emptyMap()).fingerprint)
        // The same overrides in another order are the same normaliser: the fingerprint sorts them.
        assertEquals(
            normaliser(mapOf("A" to "B", "C" to "D")).fingerprint,
            normaliser(mapOf("C" to "D", "A" to "B")).fingerprint,
        )

        // And it describes what is applied: PRADOVIEJOO is fixed only with the override (DD-11, spec §5.5).
        val fixed =
            withOverride
                .normalise(lines, stops)
                .catalogue()
                .lines
                .single { it.id == 4 }
        val raw =
            normaliser(emptyMap())
                .normalise(lines, stops)
                .catalogue()
                .lines
                .single { it.id == 4 }
        assertEquals("Palacio de Congresos - Pradoviejo", fixed.displayName)
        assertEquals("Palacio de Congresos - Pradoviejoo", raw.displayName)
    }

    private fun shapeOf(
        line: CatalogueLine,
        direction: Direction,
    ) = Polyline.decode(line.patterns.getValue(direction).shapePolyline, 6)

    /** A line whose outbound pattern is stop 38 and whose inbound one is stop 39, both a single segment long. */
    private fun line(id: Int): String =
        """{"id":$id,"name":"$id-TERMINUS A-TERMINUS B","color":"rgba(1,255,1,1)","status":1,""" +
            """"stops":{"asc":[${stopInPattern(38)}],"desc":[${stopInPattern(39)}]}}"""

    private fun stop(
        id: Int,
        name: String = "Iglesia de Lardero",
        lat: Double = 42.426317,
        lng: Double = -2.461335,
    ): String = """{"id":$id,"name":"$name","lat":$lat,"lng":$lng,"lines":[1],"status":1}"""

    private fun stopInPattern(id: Int): String =
        """{"id":$id,"name":"Iglesia de Lardero","lat":42.426317,"lng":-2.461335,"status":1,""" +
            """"onwardShape":[{"lat":42.426320,"lng":-2.461335}]}"""
}
