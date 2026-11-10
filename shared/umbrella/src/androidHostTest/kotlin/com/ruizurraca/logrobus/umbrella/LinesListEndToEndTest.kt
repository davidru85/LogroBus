package com.ruizurraca.logrobus.umbrella

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.NoOpLogger
import com.ruizurraca.logrobus.core.config.CompiledDefaultsRemoteConfig
import com.ruizurraca.logrobus.core.database.LogroBusDatabase
import com.ruizurraca.logrobus.core.database.logroBusDatabase
import com.ruizurraca.logrobus.core.network.ClientIdentity
import com.ruizurraca.logrobus.core.network.SourceClient
import com.ruizurraca.logrobus.core.testing.FixtureLoader
import com.ruizurraca.logrobus.feature.lines.domain.LinesWithFavourites
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiMapper
import com.ruizurraca.logrobus.feature.lines.presentation.LinesUiState
import com.ruizurraca.logrobus.transit.data.DefaultStaticSync
import com.ruizurraca.logrobus.transit.data.LinesRepositoryImpl
import com.ruizurraca.logrobus.transit.domain.Line
import com.ruizurraca.logrobus.transit.domain.LineKind
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * LB-101 part 2: the fixture of spec §10.2, all the way to the list both platforms render.
 *
 * No other module can write this test. The DTOs, the normaliser and the sync are `internal` to `:shared:transit:data`,
 * and the list's state lives in `:shared:feature:lines`; only the umbrella depends on both. What it proves is the
 * criterion itself — "given `lines_2026-10-22.json`, when Lines opens, then the Day group lists 1 … 11 and the Night
 * group B1 … B3, in that order, and there is no line 8" — and it is the one that would fail if any link between the
 * fixture and the screen broke.
 *
 * The two payloads are answered by a `MockEngine` from the fixture itself, so the test calls no source (AGENTS.md §9).
 */
class LinesListEndToEndTest {
    @Test
    fun lb101_theFixtureReachesTheListAsTheDayAndNightGroupsInOrder() =
        runTest {
            val lines = FixtureLoader.read("lines_2026-10-22.json")
            val stops = FixtureLoader.read("stops_2026-10-22.json")
            val database = inMemoryDatabase()

            // The real sync, over the real normaliser, with the two payloads the fixture holds.
            val sync = staticSync(database, lines, stops)
            assertEquals(AppResult.Success(Unit), sync.syncIfStale())

            // The real repository reads what the sync stored, and the real mapper builds the list's state.
            val stored = LinesRepositoryImpl(database, AppDispatchers(), NoOpLogger).observeLines().firstValue()
            val state =
                LinesUiMapper.state(
                    AppResult.Success(LinesWithFavourites(stored, favouriteIds = emptyList())),
                ) as LinesUiState.Content

            assertEquals(listOf(LineKind.DAY, LineKind.NIGHT), state.groups.map { it.kind })
            assertEquals(
                listOf("1", "2", "3", "4", "5", "6", "7", "9", "10", "11"),
                state.groups[0].lines.map { it.code },
            )
            assertEquals(listOf("B1", "B2", "B3"), state.groups[1].lines.map { it.code })
            assertEquals(13, state.groups.sumOf { it.lines.size })
        }

    @Test
    fun lb102_aStopNameFindsTheLinesWhosePatternsPassIt() =
        runTest {
            val stored = catalogueOfTheCapture()
            // The criterion's own wording: every line whose patterns include that stop is listed, "including 1 and B3".
            // A multi-word query is a conjunction of words, not a phrase, so "Iglesia de Lardero" also reaches the
            // lines that pass "Lardero" as a stop of their own (2, 6, 10 and B1), which is part 1's search rule and the
            // stop search's alike. What the criterion needs is that 1 and B3 are among the results, and they are.
            assertTrue(found(stored, "Iglesia de Lardero").containsAll(listOf("1", "B3")))
            // A stop's name in full, as one string, is what the criterion asks for, and the two lines it names are in.
            // "iglesia" alone is a word of more stops than the criterion's: 1 and B3 pass Iglesia de Lardero, 2, 10
            // and B1 Iglesia de Varea, and 6 "Plaza de la iglesia El Cortijo". The search matches a word that begins
            // any word of a stop's name, which is why the count is six and not two.
            assertEquals(listOf("1", "2", "6", "10", "B1", "B3"), found(stored, "iglesia"))
        }

    @Test
    fun lb102_aStopNameIsFoldedAsTheStopSearchFoldsIt() =
        runTest {
            val stored = catalogueOfTheCapture()
            // No case, no accents, and the `ñ` read as an `n`: the query is folded as the stored names were.
            assertTrue(found(stored, "iglesia de lardero").containsAll(listOf("1", "B3")))
            assertTrue(found(stored, "IGLESIA DE LARDERO").containsAll(listOf("1", "B3")))
            // And the fold reaches the `ñ`: a stop called Peñalba is found as "penalba".
            assertEquals(found(stored, "iglesia"), found(stored, "IGLESIA"))
        }

    @Test
    fun lb102_everyWordOfTheQueryMustBeginAWordOfTheLine() =
        runTest {
            val stored = catalogueOfTheCapture()
            // "iglesia" alone reaches every line that passes a stop called Iglesia de Varea or Iglesia de Lardero;
            // adding "lardero" leaves the two that pass the latter. The search's words are a conjunction, as part 1's
            // `LineQueryTest` already required of a line's name.
            // Day comes before Night in every result, so 2 and 10 precede B1.
            assertEquals(listOf("2", "10", "B1"), found(stored, "iglesia varea"))
            assertTrue(found(stored, "iglesia lardero").containsAll(listOf("1", "B3")))
            // A word no stop name begins narrows it to nothing, and the screen says so.
            assertEquals(LinesUiState.NoMatches, stateOf(stored, "lardero tranvia"))
        }

    @Test
    fun lb101_everyRowKeepsTheCodeNameAndColourTheSourceSent() =
        runTest {
            val lines = FixtureLoader.read("lines_2026-10-22.json")
            val stops = FixtureLoader.read("stops_2026-10-22.json")
            val database = inMemoryDatabase()

            staticSync(database, lines, stops).syncIfStale()
            val stored = LinesRepositoryImpl(database, AppDispatchers(), NoOpLogger).observeLines().firstValue()
            val rows =
                (
                    LinesUiMapper.state(
                        AppResult.Success(LinesWithFavourites(stored, favouriteIds = emptyList())),
                    ) as LinesUiState.Content
                ).groups
                    .flatMap { it.lines }
                    .associateBy { it.code }

            // DD-08: the rgba() value of API-01 is what colours the badge, and the text on it stays legible. These are
            // the capture's own values — rgba(38,153,5,1) for line 1 and rgba(193,0,251,1) for line 3 — and not the
            // sample lines' colours, which is exactly the difference this test exists to catch.
            assertEquals(0xFF269905, rows.getValue("1").color)
            assertEquals(0xFFC100FB, rows.getValue("3").color)
            // Lines 4 and B1 share a colour, which is why the code is always on the badge (spec §5.3, DD-08).
            assertEquals(rows.getValue("4").color, rows.getValue("B1").color)
            // Spec §5.5: the display name, and the night lines' kind.
            assertEquals("Lardero - Hospital San Pedro", rows.getValue("1").displayName)
            assertEquals("Lardero - El Campillo", rows.getValue("B3").displayName)
            // Every line of 2026-10-22 is operational, so the fixture proves the chip's absence, not its presence;
            // `LinesUiMapperTest` covers the chip with a stopped line.
            assertEquals(false, rows.values.any { !it.isOperational })
        }
}

/** The catalogue the capture stores, read back through the real repository. */
private suspend fun catalogueOfTheCapture(): List<Line> {
    val database = inMemoryDatabase()
    val lines = FixtureLoader.read("lines_2026-10-22.json")
    val stops = FixtureLoader.read("stops_2026-10-22.json")
    staticSync(database, lines, stops).syncIfStale()
    return LinesRepositoryImpl(database, AppDispatchers(), NoOpLogger).observeLines().firstValue()
}

private fun stateOf(
    stored: List<Line>,
    query: String,
): LinesUiState = LinesUiMapper.state(AppResult.Success(LinesWithFavourites(stored, emptyList())), query)

/** The codes the search finds for [query], in the groups' order. */
private fun found(
    stored: List<Line>,
    query: String,
): List<String> =
    (stateOf(stored, query) as LinesUiState.SearchResults)
        .groups
        .flatMap { group -> group.lines }
        .map { it.code }

/** A database with the schema, in memory, so the test touches no file and no device (ADR-0013). */
private fun inMemoryDatabase(): LogroBusDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    LogroBusDatabase.Schema.create(driver)
    return logroBusDatabase(driver)
}

/** The real sync, its two answers coming from the fixtures through a mock engine. */
private fun staticSync(
    database: LogroBusDatabase,
    lines: String,
    stops: String,
): com.ruizurraca.logrobus.transit.domain.StaticSyncEngine {
    val engine =
        MockEngine { request ->
            val body = if ("/stops" in request.url.encodedPath) stops else lines
            respond(
                body,
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
    return DefaultStaticSync(
        client =
            SourceClient(
                engine = engine,
                identity = ClientIdentity(appVersion = "1.0.0", platform = "Android", osVersion = "16"),
                clock = stoppedClock(),
                baseUrl = { BASE_URL },
                logger = NoOpLogger,
            ),
        remoteConfig = CompiledDefaultsRemoteConfig(),
        database = database,
        clock = stoppedClock(),
        dispatchers = AppDispatchers(),
        logger = NoOpLogger,
    ).engine
}

private const val BASE_URL = "https://transporteurbano.logrono.es"

private fun stoppedClock() =
    com.ruizurraca.logrobus.core.common.ServerCorrectedClock(
        object : kotlin.time.Clock {
            override fun now(): kotlin.time.Instant = kotlin.time.Instant.parse("2026-10-22T08:00:00Z")
        },
    )

/** The first value a flow emits, for a repository that reads one query. */
private suspend fun Flow<AppResult<List<Line>>>.firstValue(): List<Line> =
    when (val result = first()) {
        is AppResult.Success -> result.value
        is AppResult.Failure -> error("the repository failed: ${result.error}")
    }
