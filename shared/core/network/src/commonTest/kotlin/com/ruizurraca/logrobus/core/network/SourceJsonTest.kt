package com.ruizurraca.logrobus.core.network

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.LogLevel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlin.test.Test
import kotlin.test.assertEquals

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

    /** A DTO that rejects a value itself, as an `init` check or a custom serializer can. */
    @Serializable
    private data class CheckedDto(
        val id: Int? = null,
    ) {
        init {
            check(id != 13) { "Unlucky" }
        }
    }

    private val logger = RecordingLogger()

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

        val expected: Map<String, AppResult<JsonArray>> = bodies.associateWith { AppResult.Failure(AppError.Data) }
        assertEquals(expected, results)
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

        val entry = logger.entries.single()
        assertEquals(listOf(LogLevel.Warn, "SourceJson", null), listOf(entry[0], entry[1], entry[3]))
        assertEquals("Dropped StopDto at index 1 (JsonDecodingException)", entry[2])
    }

    @Test
    fun lb005_aBareObjectIsDecodedLeniently() {
        val body = json("""{"id":38,"name":"Ayuntamiento","lines":null,"distanceM":512}""")

        assertEquals(AppResult.Success(StopDto(38, "Ayuntamiento")), body.decodeAs(StopDto.serializer()))
    }

    @Test
    fun lb005_aBareObjectThatCannotBeDecodedIsADataError() {
        val bodies = listOf("""[]""", """"routes"""", """null""", """{"id":"x"}""", """{"lines":{}}""")

        val results = bodies.associateWith { json(it).decodeAs(StopDto.serializer()) }

        val expected: Map<String, AppResult<StopDto>> = bodies.associateWith { AppResult.Failure(AppError.Data) }
        assertEquals(expected, results)
    }

    @Test
    fun lb005_anItemThatItsTypeRejectsIsDroppedLikeAMalformedOne() {
        // kotlinx.serialization wraps what a type throws while decoding in a JsonDecodingException, so the decoders'
        // IllegalArgumentException catch holds it; this pins that, should a library update change it.
        val items = json("""[{"id":12},{"id":13},{"id":14}]""").jsonArray

        val decoded = items.decodeEach(CheckedDto.serializer(), logger)

        assertEquals(Decoded(listOf(CheckedDto(12), CheckedDto(14)), dropped = 1), decoded)
        assertEquals(AppResult.Failure(AppError.Data), json("""{"id":13}""").decodeAs(CheckedDto.serializer()))
    }
}
