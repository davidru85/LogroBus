package com.ruizurraca.logrobus.core.database

import app.cash.sqldelight.db.SqlDriver
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * LB-102 part 2 (FR-LINE-03, NFR-I18N-03): the names of the stops a line's patterns pass, folded as the stop search
 * folds them, for the Lines search to match a line by a stop it serves.
 *
 * The query joins `pattern_stop` to `stop`, so it sees only the stops a line actually calls at: a stop in the catalogue
 * that no pattern reaches is nobody's, and a provisional stop (DD-10) is a real one for this purpose.
 */
class LineStopNamesTest {
    private val driver = testDriver()
    private val database = logroBusDatabase(driver)

    @Test
    fun lb102_aLineReportsTheFoldedNamesOfTheStopsItsPatternsPass() {
        driver.line(id = 1)
        driver.patternStop(lineId = 1, direction = "OUTBOUND", seq = 1, stopId = 38)
        driver.patternStop(lineId = 1, direction = "OUTBOUND", seq = 2, stopId = 99)
        driver.insertStop(38, "Iglesia de Lardero")
        driver.insertStop(99, "Avda. de Portugal")

        assertEquals(
            listOf("avda de portugal", "iglesia de lardero"),
            database.lineQueries
                .selectLineStopNames()
                .executeAsList()
                .map { it.normalized_name },
        )
    }

    @Test
    fun lb102_aNameTwoDirectionsShareIsReportedOnce() {
        driver.line(id = 1)
        driver.patternStop(lineId = 1, direction = "OUTBOUND", seq = 1, stopId = 38)
        driver.patternStop(lineId = 1, direction = "INBOUND", seq = 1, stopId = 38)
        driver.insertStop(38, "Iglesia de Lardero")

        assertEquals(
            1,
            database.lineQueries
                .selectLineStopNames()
                .executeAsList()
                .size,
        )
    }

    @Test
    fun lb102_aStopThatNoPatternReachesIsNotAnyLines() {
        driver.line(id = 1)
        driver.patternStop(lineId = 1, direction = "OUTBOUND", seq = 1, stopId = 38)
        driver.insertStop(38, "Iglesia de Lardero")
        // In the catalogue, but no line calls there.
        driver.insertStop(200, "Plaza del Ayuntamiento")

        assertEquals(
            listOf("iglesia de lardero"),
            database.lineQueries
                .selectLineStopNames()
                .executeAsList()
                .map { it.normalized_name },
        )
    }

    @Test
    fun lb102_everyLineReportsItsOwnNames() {
        driver.line(id = 1)
        driver.line(id = 2)
        driver.patternStop(lineId = 1, direction = "OUTBOUND", seq = 1, stopId = 38)
        driver.patternStop(lineId = 2, direction = "OUTBOUND", seq = 1, stopId = 99)
        driver.insertStop(38, "Iglesia de Lardero")
        driver.insertStop(99, "Avda. de Portugal")

        assertEquals(
            mapOf(1L to listOf("iglesia de lardero"), 2L to listOf("avda de portugal")),
            database.lineQueries
                .selectLineStopNames()
                .executeAsList()
                .groupBy({ it.line_id }, { it.normalized_name }),
        )
    }
}

private fun SqlDriver.line(id: Long) {
    execute(
        identifier = null,
        sql =
            "INSERT INTO line (id, code, raw_name, display_name, color, on_color, kind, operational) " +
                "VALUES (?, ?, 'name', 'Name', 4278190335, 4278190080, 'DAY', 1)",
        parameters = 2,
    ) {
        bindLong(0, id)
        bindString(1, "$id")
    }
    // LB-102 part 2: the patterns themselves, one per direction, so a pattern_stop has a line to hang from.
    listOf("OUTBOUND", "INBOUND").forEach { direction ->
        execute(
            identifier = null,
            sql = "INSERT INTO pattern (line_id, direction, headsign, shape_polyline) VALUES (?, ?, 'End', '')",
            parameters = 2,
        ) {
            bindLong(0, id)
            bindString(1, direction)
        }
    }
}

private fun SqlDriver.patternStop(
    lineId: Long,
    direction: String,
    seq: Long,
    stopId: Long,
) {
    execute(
        identifier = null,
        sql = "INSERT INTO pattern_stop (line_id, direction, seq, stop_id, shape_index) VALUES (?, ?, ?, ?, 0)",
        parameters = 4,
    ) {
        bindLong(0, lineId)
        bindString(1, direction)
        bindLong(2, seq)
        bindLong(3, stopId)
    }
}
