package com.ruizurraca.logrobus.architecture

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * scripts/record-fixtures.sh is the only place in the project that may call the live source (spec §8.7,
 * docs/testing_strategy.md §4.3), so nothing else in the suite would notice when it drifts from the contract:
 * the request headers, the canonical output, the one request per second, the atomic replace and the shape gate.
 *
 * The recording cases run the real script against a local stub server written here with python3 and bound to
 * 127.0.0.1 on a free port. The recorder reaches it through `RECORD_BASE_URL`, `RECORD_GEOCODER_BASE_URL` and
 * `RECORD_FIXTURES_DIR`, the overrides that exist for these tests. No test touches `fixtures/` or the live source
 * (AGENTS.md §9).
 *
 * The shape cases call scripts/ci/fixture-shape.sh --diff straight on two documents, with no server; the endpoint
 * table is read through `--list`, which needs neither a network nor jq. The canonical form the recorder writes is
 * asserted with jq, because both tools are already a prerequisite of the recorder itself.
 */
class FixtureRecorderScriptTest {
    @TempDir
    lateinit var dir: File

    private val script: File = repositoryRoot.resolve("scripts/record-fixtures.sh")
    private val shapeScript: File = repositoryRoot.resolve("scripts/ci/fixture-shape.sh")
    private val stubs = mutableListOf<Process>()
    private var stubCount = 0
    private var fixtureDirs = 0

    @AfterEach
    fun stopTheStubs() {
        stubs.forEach { process ->
            process.destroy()
            if (!process.waitFor(STOP_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly()
            }
        }
        stubs.clear()
    }

    @Test
    fun lb003_theRecorderCoversEveryFixtureOfTheSpec() {
        // Spec §10.2's set plus API-12's fifth path (`sections`) and the two geocoder captures of api_spec.md §5.
        // --list reads the table only: it must not need a network, jq or a stub server.
        val result = run(listOf("bash", script.path, "--list"), emptyMap())

        assertEquals(0, result.exitCode, result.output)
        val files =
            result.output
                .lines()
                .filter { it.isNotBlank() }
                .map { it.split('\t')[2] }
        assertEquals(EXPECTED_FIXTURES, files)
    }

    @Test
    fun lb003_aCaptureSendsTheProjectUserAgent() {
        val stub = startStub(Route("/api/linesDiscovery/lines", 200, """{"lines":[]}"""))

        val result = recorder(stub.baseUrl, fixtures(), "1", "API-01")

        assertEquals(0, result.exitCode, result.output)
        val request = stub.requests().single()
        assertTrue(
            USER_AGENT.matches(request.userAgent),
            "User-Agent must be LogronoBus/<version> (fixture recorder ...): ${request.userAgent}",
        )
        assertTrue(
            request.acceptEncoding.contains("gzip"),
            "Accept-Encoding must ask for gzip: ${request.acceptEncoding}",
        )
    }

    @Test
    fun lb003_aPostSendsTheJsonBodyTheTableDeclares() {
        // API-07a's planner request: the source reads its criteria from the body, and Content-Type marks it as JSON.
        val stub = startStub(Route("/api/transit/route", 201, """{"routes":[]}"""))

        val result = recorder(stub.baseUrl, fixtures(), "0", "API-07a")

        assertEquals(0, result.exitCode, result.output)
        val request = stub.requests().single()
        assertEquals("/api/transit/route", request.path)
        assertEquals("application/json", request.contentType)
        assertEquals(
            """{"origin":{"lat":42.4650,"lng":-2.4450},"destination":{"lat":42.4560,"lng":-2.4300},""" +
                "\"departureAt\":\"2026-10-19T07:19:00.000Z\",\"maxResults\":5}",
            request.body,
        )
    }

    @Test
    fun lb003_aCaptureIsPrettyPrintedWithSortedKeys() {
        val body = """{"zeta":1,"alpha":{"beta":[1,2],"gamma":null},"omega":"x"}"""
        val stub = startStub(Route("/api/linesDiscovery/lines", 200, body))
        val fixtures = fixtures()

        val first = recorder(stub.baseUrl, fixtures, "1", "API-01")

        assertEquals(0, first.exitCode, first.output)
        val captured = File(fixtures, "lines_2026-10-22.json")
        assertEquals(canonical(body), captured.readText())
        assertTrue(first.output.contains("written"), first.output)

        val before = captured.readBytes()
        val second = recorder(stub.baseUrl, fixtures, "1", "API-01")

        assertEquals(0, second.exitCode, second.output)
        assertArrayEquals(before, captured.readBytes(), "re-recording unchanged data must produce no diff")
        assertTrue(second.output.contains("unchanged"), second.output)
    }

    @Test
    fun lb003_requestsAreAtLeastOneSecondApart() {
        // spec §8.7 and risk R2: the source sends no rate-limit headers, so the recorder paces itself.
        val stub =
            startStub(
                Route("/api/linesDiscovery/lines", 200, """{"lines":[]}"""),
                Route("/api/linesDiscovery/stops", 200, """{"stops":[]}"""),
                Route("/api/vehicleMonitoring/all", 200, """{"vehicles":[]}"""),
            )

        val result = recorder(stub.baseUrl, fixtures(), "1", "API-01", "API-02", "API-03")

        assertEquals(0, result.exitCode, result.output)
        val times = stub.requests().map { it.seconds }
        assertEquals(3, times.size, "every endpoint of the run must be captured once")
        times.zipWithNext().forEach { (before, after) ->
            assertTrue(
                after - before >= MIN_GAP_SECONDS,
                "requests were ${"%.3f".format(after - before)} s apart, expected at least 1 s",
            )
        }
    }

    @Test
    fun lb003_aShapeDiffReportsAnAddedField() {
        val result = shapeDiff("""{"a":1}""", """{"a":1,"b":"x"}""")

        assertEquals(1, result.exitCode, result.output)
        assertEquals(listOf("+ b string"), result.output.lines().filter { it.isNotBlank() })
    }

    @Test
    fun lb003_aShapeDiffReportsARemovedField() {
        val result = shapeDiff("""{"a":1,"b":"x"}""", """{"a":1}""")

        assertEquals(1, result.exitCode, result.output)
        assertEquals(listOf("- b string"), result.output.lines().filter { it.isNotBlank() })
    }

    @Test
    fun lb003_aShapeDiffReportsATypeChange() {
        val result = shapeDiff("""{"a":1}""", """{"a":"1"}""")

        assertEquals(1, result.exitCode, result.output)
        assertEquals(listOf("~ a number->string"), result.output.lines().filter { it.isNotBlank() })
    }

    @Test
    fun lb003_anIdenticalShapeReportsNoDiff() {
        // The payload differs, the shape does not: live data changes on every capture, the contract must not (risk R1).
        val result =
            shapeDiff("""{"lines":[{"id":1,"name":"A"}]}""", """{"lines":[{"id":2,"name":"B"},{"id":3,"name":"C"}]}""")

        assertEquals(0, result.exitCode, result.output)
        assertEquals("", result.output.trim())
    }

    @Test
    fun lb003_aNullAndAValueAreTheSameShape() {
        // A nullable field must not read as drift every time the source fills it in.
        val result =
            shapeDiff("""{"stops":[{"id":"38","name":null}]}""", """{"stops":[{"id":"38","name":"Portales"}]}""")

        assertEquals(0, result.exitCode, result.output)
        assertEquals("", result.output.trim())
    }

    @Test
    fun lb003_anIntegerAndAFloatAreTheSameShape() {
        // The source mixes 42.0 and 42 for the same field.
        val result = shapeDiff("""{"stops":[{"id":"38","lat":42}]}""", """{"stops":[{"id":"38","lat":42.0}]}""")

        assertEquals(0, result.exitCode, result.output)
        assertEquals("", result.output.trim())
    }

    @Test
    fun lb003_anEmptyCollectionIsTheSameShapeAsAPopulatedOne() {
        // A night-time `activities: []` or an empty `news` list used to read as every child path removed, because an
        // empty array contributed no element row at all. The element path of an empty array reports `unknown`, and
        // the diff reads `unknown` as compatible with any type and any path beneath it (risk R1).
        val populated = """{"r":{"a":[{"id":1}]}}"""
        val empty = """{"r":{"a":[]}}"""

        val emptied = shapeDiff(populated, empty)

        assertEquals(0, emptied.exitCode, emptied.output)
        assertEquals("", emptied.output.trim())

        val filled = shapeDiff(empty, populated)

        assertEquals(0, filled.exitCode, filled.output)
        assertEquals("", filled.output.trim())
    }

    @Test
    fun lb003_anAddedFieldOnANonEmptyCollectionIsStillDrift() {
        // The empty-collection rule must not swallow the drift the gate exists for.
        val result = shapeDiff("""{"r":{"a":[{"id":1}]}}""", """{"r":{"a":[{"id":1,"x":2}]}}""")

        assertEquals(1, result.exitCode, result.output)
        assertEquals(listOf("+ r.a[].x number"), result.output.lines().filter { it.isNotBlank() })
    }

    @Test
    fun lb003_anOversizedBodyFailsTheEndpoint() {
        // A broken or hostile source must not fill the disk beside fixtures/: the recorder caps a body at
        // MAX_BODY_BYTES (16 MiB, an order of magnitude above the ~1.1 MB the source serves today, risk R2). Both
        // paths must fail the endpoint, name it and leave fixtures/ alone (NFR-REL-02).
        val padding = "a".repeat(OVERSIZED_BODY_BYTES)
        val oversized = """{"padding":"$padding"}"""
        val cases =
            listOf(
                // A declared Content-Length above the cap: curl --max-filesize stops the transfer on the wire.
                Triple(Route("/api/linesDiscovery/lines", 200, oversized), "API-01", "lines_2026-10-22.json"),
                // A gzip body small on the wire and above the cap once decompressed: --compressed makes the wire
                // cap approximate, so only the check after the download catches this one.
                Triple(
                    Route("/api/linesDiscovery/stops", 200, oversized, "gzip"),
                    "API-02",
                    "stops_2026-10-22.json",
                ),
            )

        cases.forEach { (route, endpoint, name) ->
            val stub = startStub(route)
            val fixtures = fixtures()
            val committed = File(fixtures, name).apply { writeText("{\n  \"previous\": true\n}\n") }
            val before = committed.readBytes()

            val result = recorder(stub.baseUrl, fixtures, "0", endpoint)

            assertNotEquals(0, result.exitCode, "$name: ${result.output}")
            assertTrue(result.output.contains(endpoint), "the failure must name the endpoint: ${result.output}")
            assertTrue(result.output.contains("cap"), "the failure must name the cap: ${result.output}")
            assertArrayEquals(before, committed.readBytes(), "$name: an oversized body must not replace a fixture")
            assertEquals(listOf(name), fixtures.list()!!.sorted(), "$name: an oversized body must not add a fixture")
        }
    }

    @Test
    fun lb003_checkModeFailsOnDriftAndPassesOnAnUnchangedShape() {
        // The weekly CI job runs --check: it must fail on a new field and pass when only the data moved.
        val fixtures = fixtures()
        File(fixtures, "lines_2026-10-22.json").writeText("""{"lines":[{"id":1,"name":"A"}]}""")
        val same = startStub(Route("/api/linesDiscovery/lines", 200, """{"lines":[{"id":2,"name":"B"}]}"""))

        val unchanged = recorder(same.baseUrl, fixtures, "0", "--check", "API-01")

        assertEquals(0, unchanged.exitCode, unchanged.output)
        assertTrue(unchanged.output.contains("unchanged"), unchanged.output)

        val grown = startStub(Route("/api/linesDiscovery/lines", 200, """{"lines":[{"id":2,"name":"B","extra":1}]}"""))

        val drifted = recorder(grown.baseUrl, fixtures, "0", "--check", "API-01")

        assertEquals(1, drifted.exitCode, drifted.output)
        assertTrue(drifted.output.contains("DRIFT"), drifted.output)
        assertTrue(drifted.output.contains("+ lines[].extra number"), drifted.output)

        val absent = recorder(same.baseUrl, fixtures(), "0", "--check", "API-01")

        assertEquals(1, absent.exitCode, absent.output)
        assertTrue(absent.output.contains("missing"), absent.output)
    }

    @Test
    fun lb003_aBodyThatIsNotAnObjectIsRejected_evenWhenItIsEmptyOrScalar() {
        // `jq empty` accepts a zero-length body, `null` and a scalar, and `jq .` then writes nothing or a value that
        // has no shape at all. A weekly --check would compare two empty shapes and report unchanged over a fixture
        // that had been destroyed, so a body that is not an object or an array fails the endpoint (NFR-REL-02).
        listOf("", "null", "42", "\"a word\"").forEach { body ->
            val stub = startStub(Route("/api/linesDiscovery/lines", 200, body))
            val fixtures = fixtures()
            File(fixtures, "lines_2026-10-22.json").writeText("""{"previous":true}""")
            val before = File(fixtures, "lines_2026-10-22.json").readBytes()

            val result = recorder(stub.baseUrl, fixtures, "0", "API-01")

            assertNotEquals(0, result.exitCode, "body [$body] must be rejected: ${result.output}")
            assertTrue(result.output.contains("API-01"), result.output)
            assertArrayEquals(before, File(fixtures, "lines_2026-10-22.json").readBytes(), "body [$body]")
        }
    }

    @Test
    fun lb003_twoModeWordsAreAnError_ratherThanSilentlyListing() {
        // A caller that passed two modes got the first one and exit 0: `--list --check` listed and never checked.
        val result = run(listOf("bash", script.path, "--list", "--check"), emptyMap())

        assertNotEquals(0, result.exitCode, result.output)
    }

    @Test
    fun lb003_theStagingDirectoryIsIgnoredByGit() {
        // A killed run leaves .record-fixtures.XXXXXX beside fixtures/. It holds a whole capture, so it must never
        // appear in a change set.
        val pattern = File(repositoryRoot, ".gitignore").readLines().map { it.trim() }
        assertTrue(pattern.any { it == ".record-fixtures.*/" }, "the staging directory must be ignored: $pattern")
    }

    @Test
    fun lb003_aFailedCaptureLeavesTheFixturesUntouched() {
        // The second endpoint answers 503: the whole run must fail and fixtures/ keep the previous capture.
        val stub =
            startStub(
                Route("/api/linesDiscovery/lines", 200, """{"lines":[]}"""),
                Route("/api/linesDiscovery/stops", 503, """{"error":"unavailable"}"""),
            )
        val fixtures = fixtures()
        val committed = File(fixtures, "lines_2026-10-22.json").apply { writeText("{\n  \"previous\": true\n}\n") }
        val before = committed.readBytes()

        val result = recorder(stub.baseUrl, fixtures, "0", "API-01", "API-02")

        assertNotEquals(0, result.exitCode, result.output)
        assertTrue(result.output.contains("API-02"), "the failure must name the endpoint: ${result.output}")
        assertArrayEquals(before, committed.readBytes(), "a failed run must not replace a fixture")
        assertFalse(File(fixtures, "stops_2026-10-22.json").exists(), "a failed run must not create a fixture")
    }

    private data class Result(
        val exitCode: Int,
        val output: String,
    )

    private data class Request(
        val path: String,
        val seconds: Double,
        val userAgent: String,
        val acceptEncoding: String,
        val contentType: String,
        val body: String,
    )

    private data class Route(
        val path: String,
        val status: Int,
        val body: String,
        /** The `Content-Encoding` the stub answers with: "identity" unless a test wants a decompressed body. */
        val encoding: String = "identity",
    )

    private inner class Stub(
        val baseUrl: String,
        private val log: File,
    ) {
        fun requests(): List<Request> =
            log
                .readLines()
                .filter { it.isNotBlank() }
                .map { line ->
                    val fields = line.split('\t')
                    Request(
                        path = fields[0],
                        seconds = fields[1].toDouble(),
                        userAgent = fields[2],
                        acceptEncoding = fields[3],
                        contentType = fields[4],
                        body = fields.getOrElse(5) { "" },
                    )
                }
    }

    /** Starts a stub server for [routes] and waits until it reports the port it bound. */
    private fun startStub(vararg routes: Route): Stub {
        val stubDir = File(dir, "stub-${stubCount++}").apply { mkdirs() }
        val routesFile =
            File(stubDir, "routes.tsv").apply {
                // path \t status \t encoding \t body: the body is last, so a tab inside it is harmless.
                writeText(routes.joinToString("") { "${it.path}\t${it.status}\t${it.encoding}\t${it.body}\n" })
            }
        val log = File(stubDir, "requests.log")
        val portFile = File(stubDir, "port")
        val python = File(dir, "stub.py").apply { if (!isFile) writeText(STUB) }
        val process =
            ProcessBuilder("python3", python.path, routesFile.path, log.path, portFile.path)
                .directory(stubDir)
                .redirectErrorStream(true)
                .redirectOutput(File(stubDir, "stub.out"))
                .start()
        stubs += process
        val port = awaitPort(portFile, process, stubDir)
        return Stub("http://127.0.0.1:$port", log)
    }

    private fun awaitPort(
        portFile: File,
        process: Process,
        stubDir: File,
    ): Int {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(START_SECONDS)
        while (System.nanoTime() < deadline) {
            if (portFile.isFile && portFile.readText().isNotBlank()) {
                return portFile.readText().trim().toInt()
            }
            check(process.isAlive) { "the stub server exited: ${File(stubDir, "stub.out").readText()}" }
            Thread.sleep(POLL_MILLIS)
        }
        throw AssertionError("the stub server did not bind a port: ${File(stubDir, "stub.out").readText()}")
    }

    /** Runs the recorder against [baseUrl], writing into [fixturesDir], with [arguments] such as `--check API-01`. */
    private fun recorder(
        baseUrl: String,
        fixturesDir: File,
        interval: String = "1",
        vararg arguments: String,
    ): Result =
        run(
            listOf("bash", script.path) + arguments,
            mapOf(
                "RECORD_BASE_URL" to baseUrl,
                "RECORD_GEOCODER_BASE_URL" to baseUrl,
                "RECORD_FIXTURES_DIR" to fixturesDir.path,
                "RECORD_MIN_INTERVAL" to interval,
            ),
        )

    private fun shapeDiff(
        old: String,
        new: String,
    ): Result {
        val oldFile = File(dir, "shape-old.json").apply { writeText(old) }
        val newFile = File(dir, "shape-new.json").apply { writeText(new) }
        return run(listOf("bash", shapeScript.path, "--diff", oldFile.path, newFile.path), emptyMap())
    }

    /** The fixture directory of a test, created empty: `fixtures/` is never written. */
    private fun fixtures(): File = File(dir, "fixtures-${fixtureDirs++}").apply { mkdirs() }

    /** What `jq --sort-keys --indent 2` prints for [body]: the canonical form the recorder must write. */
    private fun canonical(body: String): String {
        val input = File(dir, "canonical.json").apply { writeText(body) }
        return runChecked(listOf("jq", "--sort-keys", "--indent", "2", ".", input.path))
    }

    private fun run(
        command: List<String>,
        env: Map<String, String>,
    ): Result {
        val process =
            ProcessBuilder(command)
                .directory(dir)
                .redirectErrorStream(true)
                .apply {
                    // A developer's own RECORD_* overrides must not reach a test.
                    environment().keys.filter { it.startsWith("RECORD_") }.forEach { environment().remove(it) }
                    environment().putAll(env)
                }.start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "${command.joinToString(" ")} did not finish" }
        return Result(process.exitValue(), output)
    }

    private fun runChecked(command: List<String>): String {
        val process =
            ProcessBuilder(command)
                .directory(dir)
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS) && process.exitValue() == 0) {
            "${command.joinToString(" ")} failed: $output"
        }
        return output
    }

    private companion object {
        const val TIMEOUT_SECONDS = 120L
        const val START_SECONDS = 30L
        const val STOP_SECONDS = 5L
        const val POLL_MILLIS = 25L

        // Above the recorder's 16 MiB MAX_BODY_BYTES. The identity route declares that length, so curl
        // --max-filesize stops it on the wire; the gzip route is small on the wire and only exceeds the cap once
        // decompressed, so the post-download check is the one that catches it.
        const val OVERSIZED_BODY_BYTES = 17 * 1024 * 1024

        // The recorder sleeps a whole second between requests. The slack only absorbs the millisecond rounding of
        // the stub's timestamps, so a request that came less than a second after the previous one still fails.
        const val MIN_GAP_SECONDS = 0.999

        val USER_AGENT = Regex("""^LogronoBus/[0-9.]+ \(fixture recorder .+\)$""")

        /** The fixture names of spec §10.2, plus `sections.json` and the two geocoder captures (api_spec.md §9). */
        val EXPECTED_FIXTURES =
            listOf(
                "lines_2026-10-22.json",
                "stops_2026-10-22.json",
                "vm_all_with_object_lineRef.json",
                "vm_line2.json",
                "et_line1_60min.json",
                "et_stop38.json",
                "route_ok.json",
                "route_rejected.json",
                "walk_distances.json",
                "fares.json",
                "recharge_points.json",
                "news_page1.json",
                "news_header.json",
                "sections.json",
                "cards.json",
                "pies.json",
                "rrss.json",
                "geocoder_candidates_logrono.json",
                "geocoder_find.json",
            )

        /**
         * A stand-in for the source: answers each path from its route table and appends one tab-separated line per
         * request (path, epoch seconds, User-Agent, Accept-Encoding, Content-Type, body). A route whose encoding is
         * "gzip" is compressed before it is sent, so a test can hand the recorder a body that is small on the wire
         * and large once decompressed. It binds 127.0.0.1 on port 0, so the kernel picks a free port, which the
         * server writes to the file it receives as its third argument.
         */
        val STUB =
            """
            import gzip
            import sys
            import time
            from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


            def read_routes(path):
                routes = {}
                with open(path, encoding="utf-8") as source:
                    for line in source:
                        line = line.rstrip("\n")
                        if line:
                            route_path, status, encoding, body = line.split("\t", 3)
                            routes[route_path] = (int(status), encoding, body)
                return routes


            routes_path, log_path, port_path = sys.argv[1:4]
            routes = read_routes(routes_path)


            class Handler(BaseHTTPRequestHandler):
                def do_GET(self):
                    self.answer()

                def do_POST(self):
                    self.answer()

                def answer(self):
                    length = int(self.headers.get("Content-Length") or 0)
                    body = self.rfile.read(length).decode("utf-8") if length else ""
                    with open(log_path, "a", encoding="utf-8") as log:
                        log.write("\t".join([
                            self.path,
                            "%.3f" % time.time(),
                            self.headers.get("User-Agent") or "",
                            self.headers.get("Accept-Encoding") or "",
                            self.headers.get("Content-Type") or "",
                            body,
                        ]) + "\n")
                    status, encoding, payload = routes.get(self.path, (404, "identity", "{}"))
                    encoded = payload.encode("utf-8")
                    if encoding == "gzip":
                        encoded = gzip.compress(encoded)
                    self.send_response(status)
                    self.send_header("Content-Type", "application/json")
                    if encoding != "identity":
                        self.send_header("Content-Encoding", encoding)
                    self.send_header("Content-Length", str(len(encoded)))
                    self.end_headers()
                    self.wfile.write(encoded)

                def log_message(self, *args):
                    pass


            server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
            with open(port_path, "w", encoding="utf-8") as port_file:
                port_file.write(str(server.server_address[1]))
            server.serve_forever()
            """.trimIndent() + "\n"
    }
}
