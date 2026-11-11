package com.ruizurraca.logrobus.transit.data

import app.cash.turbine.test
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.database.LogroBusDatabase
import com.ruizurraca.logrobus.core.database.logroBusDatabase
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds

/** LB-101 (FR-LINE-01, FR-LINE-02): the lines that the static sync stored, for the Lines list. */
class LinesRepositoryTest {
    private val driver = testDriver()
    private val database = logroBusDatabase(driver)
    private val logger = RecordingLogger()

    private val line4 =
        Line(
            id = 4,
            code = "4",
            displayName = "Casa Blanca - Yagüe",
            color = 0xFFE30613,
            onColor = 0xFFFFFFFF,
            kind = LineKind.DAY,
            isOperational = true,
        )
    private val lineB1 =
        Line(
            id = 101,
            code = "B1",
            displayName = "Varea - La Cava",
            color = 0xFFE30613,
            onColor = 0xFFFFFFFF,
            kind = LineKind.NIGHT,
            isOperational = false,
        )

    private fun TestScope.repository() =
        LinesRepositoryImpl(database, AppDispatchers(StandardTestDispatcher(testScheduler)), logger)

    @AfterTest
    fun closeDatabase() = driver.close()

    @Test
    fun lb101_thereAreNoLinesBeforeTheFirstSync() =
        runTest {
            repository().observeLines().test {
                assertEquals(AppResult.Success(emptyList()), awaitItem())
            }
        }

    @Test
    fun lb101_linesAreReadAsTheSyncStoredThem() =
        runTest {
            database.insert(lineB1, kind = "night")
            database.insert(line4, kind = "day")

            repository().observeLines().test {
                assertEquals(AppResult.Success(listOf(line4, lineB1)), awaitItem())
            }
        }

    @Test
    fun lb101_aNewSyncEmitsTheNewLines() =
        runTest {
            database.insert(line4, kind = "day")

            repository().observeLines().test {
                assertEquals(AppResult.Success(listOf(line4)), awaitItem())
                database.insert(lineB1, kind = "night")
                assertEquals(AppResult.Success(listOf(line4, lineB1)), awaitItem())
            }
        }

    @Test
    fun lb103_eachLineHasTheHeadsignOfEachDirection_outboundFirst() =
        runTest {
            // FR-LINE-13: a favourite line's card shows both headsigns. A line with no patterns has none.
            database.insert(line4, kind = "day")
            database.insert(lineB1, kind = "night")
            database.lineQueries.insertPattern(4, "inbound", "Casa Blanca", "")
            database.lineQueries.insertPattern(4, "outbound", "Yagüe", "")

            repository().observeLines().test {
                val expected = listOf(line4.copy(headsigns = listOf("Yagüe", "Casa Blanca")), lineB1)
                assertEquals(AppResult.Success(expected), awaitItem())
            }
        }

    @Test
    fun lb103_aPatternOfAnUnknownDirectionHasNoHeadsign_andItsLineStaysOnce() =
        runTest {
            // A direction a later version adds: this version leaves its headsign out, never the line.
            database.insert(line4, kind = "day")
            database.lineQueries.insertPattern(4, "outbound", "Yagüe", "")
            database.lineQueries.insertPattern(4, "circular", "Centro", "")

            repository().observeLines().test {
                assertEquals(AppResult.Success(listOf(line4.copy(headsigns = listOf("Yagüe")))), awaitItem())
            }
        }

    @Test
    fun lb101_aLineOfAnUnknownKindIsDroppedAndLogged() =
        runTest {
            database.insert(line4, kind = "day")
            database.insert(lineB1, kind = "tram")

            repository().observeLines().test {
                assertEquals(AppResult.Success(listOf(line4)), awaitItem())
            }
            assertEquals(LogLevel.Warn, logger.entries.single().level)
        }

    @Test
    fun lb101_aLineWhoseIdIsNoIntIsDroppedAndLogged() =
        runTest {
            // DD-02 ids are Ints; a wider one would be truncated into another line's id.
            database.insert(line4, kind = "day")
            database.lineQueries.insertLine(Int.MAX_VALUE + 1L, "B1", "raw", "Varea - La Cava", 0, 0, "night", true)

            repository().observeLines().test {
                assertEquals(AppResult.Success(listOf(line4)), awaitItem())
            }
            assertEquals(LogLevel.Warn, logger.entries.single().level)
        }

    @Test
    fun lb101_aDatabaseThatCannotBeReadGivesAFailure_notACrash() =
        runTest {
            driver.execute(null, "DROP TABLE line", 0)

            // The flow reads three database queries on the IO dispatcher before it fails, and CI's iOS runner has
            // three cores shared with every other job: Turbine's 5 s default expired once with no other failure
            // (PR #62). The assertion is unchanged; only the wait it is given has room.
            repository().observeLines().test(timeout = 30.seconds) {
                assertEquals(AppResult.Failure(AppError.Data), awaitItem())
                awaitComplete()
            }
            val entry = logger.entries.single()
            assertEquals(LogLevel.Warn, entry.level)
            assertNotNull(entry.throwable)
        }

    private fun LogroBusDatabase.insert(
        line: Line,
        kind: String,
    ) = lineQueries.insertLine(
        id = line.id.toLong(),
        code = line.code,
        raw_name = "raw ${line.displayName}",
        display_name = line.displayName,
        color = line.color,
        on_color = line.onColor,
        kind = kind,
        operational = line.isOperational,
    )
}
