package com.ruizurraca.logrobus.core.network

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.common.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** `api_spec.md` §3: lenient decoding of the source's payloads. */
class SourceJsonTest {
    /** A DTO shaped like the source's: every field nullable or defaulted (api_spec.md §3). */
    @Serializable
    private data class StopDto(
        val id: Int? = null,
        val name: String? = null,
        val lat: Double? = null,
        val lines: List<Int> = emptyList(),
    )

    /** What was logged: level, tag, message and throwable. */
    private val logged = mutableListOf<List<Any?>>()
    private val logger =
        object : Logger {
            override fun log(
                level: LogLevel,
                tag: String,
                message: String,
                throwable: Throwable?,
            ) {
                logged += listOf(level, tag, message, throwable)
            }
        }

    private fun json(text: String) = Json.parseToJsonElement(text)

    @Test
    fun lb005_theCollectionInsideTheEnvelopeIsFound() {
        val body = json("""{"result":{"lines":[{"id":1},{"id":2}],"count":2},"extra":true}""")

        assertEquals<AppResult<JsonArray>>(
            AppResult.Success(json("""[{"id":1},{"id":2}]""").jsonArray),
            body.arrayAt("result", "lines"),
        )
    }

    @Test
    fun lb005_aMissingEnvelopeIsADataError() {
        val bodies =
            listOf(
                """{}""",
                """{"result":null}""",
                """{"result":{}}""",
                """{"result":{"lines":null}}""",
                """{"result":{"lines":{"id":1}}}""",
                """{"result":[{"lines":[]}]}""",
                """{"lines":[]}""",
                """[]""",
                """42""",
                """"result"""",
            )

        val results = bodies.associateWith { json(it).arrayAt("result", "lines") }

        assertEquals<Map<String, AppResult<JsonArray>>>(bodies.associateWith { AppResult.Failure(AppError.Data) }, results)
    }

    @Test
    fun lb005_oneMalformedItemIsDroppedAndEveryOtherItemKept() {
        val items =
            json(
                """
                [
                  {"id":1,"name":"Ayuntamiento"},
                  {"id":"not a number"},
                  {"id":3,"name":"Gran Vía","status":1},
                  {"id":4,"lines":null},
                  {}
                ]
                """,
            ).jsonArray

        val decoded = items.decodeEach(StopDto.serializer(), logger)

        // An unknown key is ignored, a null for a defaulted field takes the default and a missing field is null.
        val expected = listOf(StopDto(1, "Ayuntamiento"), StopDto(3, "Gran Vía"), StopDto(4), StopDto())
        assertEquals(Decoded(expected, dropped = 1), decoded)
    }

    @Test
    fun lb005_anItemOfTheWrongShapeIsDropped() {
        val items = json("""["Ayuntamiento", null, [1], {"id":1.5}, {"lines":"1,2"}]""").jsonArray

        assertEquals(Decoded(emptyList<StopDto>(), dropped = 5), items.decodeEach(StopDto.serializer(), logger))
    }

    @Test
    fun lb005_aDroppedItemIsLoggedByTypeAndPositionWithoutItsContent() {
        // NFR-PRIV-02: a decoding error's message quotes the input, which can hold coordinates and names.
        val items = json("""[{"id":1},{"id":"x","name":"Calle Mayor","lat":42.4650}]""").jsonArray

        items.decodeEach(StopDto.serializer(), logger)

        val (level, tag, message, throwable) = logged.single()
        assertEquals(listOf(LogLevel.Warn, "SourceJson", null), listOf(level, tag, throwable))
        assertTrue("StopDto" in "$message" && "1" in "$message", "$message")
        listOf("Calle Mayor", "42.46", "\"x\"").forEach { assertFalse(it in "$message", "$message") }
    }
}
