package com.ruizurraca.logrobus.core.network

import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.NoOpLogger
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Spec §8.7: the client asks for gzip, so on OkHttp it must decompress the body itself. OkHttp only decompresses
 * transparently when it added `Accept-Encoding` on its own. NSURLSession always decompresses, so this is JVM-only.
 */
class GzipTest {
    @Test
    fun lb005_aGzippedResponseIsDecompressed() =
        runTest {
            val gzipped = ByteArrayOutputStream().also { GZIPOutputStream(it).use { gzip -> gzip.write(BODY) } }
            val engine =
                MockEngine {
                    respond(gzipped.toByteArray(), HttpStatusCode.OK, headersOf(HttpHeaders.ContentEncoding, "gzip"))
                }
            val identity = ClientIdentity("1.2.0", "Android", "16")
            val client = SourceClient(engine, identity, stoppedClock(), { BASE_URL }, NoOpLogger)

            val result = client.get("/api/linesDiscovery/lines", RequestKind.Static)

            assertEquals(AppResult.Success(buildJsonObject { put("ok", true) }), result)
        }

    private companion object {
        val BODY = """{"ok":true}""".encodeToByteArray()
    }
}
