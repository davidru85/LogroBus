package com.ruizurraca.logrobus.core.network

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlin.test.Test
import kotlin.test.assertEquals

/** `api_spec.md` §3: lenient decoding of the source's payloads. */
class SourceJsonTest {
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
}
