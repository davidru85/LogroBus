package com.ruizurraca.logrobus.architecture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * scripts/ci/gradle-retry.sh runs Gradle again when it failed only because a repository did not serve a dependency:
 * on 2026-11-02 Maven Central answered 429 and the Gradle Plugin Portal "not found" for artifacts it has, in a
 * different job each run (docs/ci_cd.md §5). Gradle itself retries a 429 a few times and a 404 never. A failing test or
 * a compile error must fail at once, never twice. Each test runs the real script over a fake Gradle that reads its
 * answers from a file, one per run.
 */
class GradleRetryScriptTest {
    @TempDir
    lateinit var dir: File

    private val script: File = repositoryRoot.resolve("scripts/ci/gradle-retry.sh")

    @Test
    fun lb002_aDependencyTheRepositoryDidNotServeIsTriedAgain_untilGradleSucceeds() {
        val result = run(PORTAL_NOT_FOUND, SUCCESS)

        assertEquals(0, result.exitCode)
        assertEquals(2, result.runs)
    }

    @Test
    fun lb002_aRateLimitedDownloadIsTriedAgain() {
        val result = run(RATE_LIMITED, SUCCESS)

        assertEquals(0, result.exitCode)
        assertEquals(2, result.runs)
    }

    @Test
    fun lb002_aRunAgainAsksForEveryDependencyAgain() {
        // Gradle remembers a file a repository did not serve, such as a module's metadata, and would fail the same
        // way on the next run without asking again.
        run(PORTAL_NOT_FOUND, SUCCESS)

        val task = ":shared:feature:lines:ciUnitTest --quiet"
        assertEquals(listOf(task, "$task --refresh-dependencies"), File(dir, "arguments").readLines())
    }

    @Test
    fun lb002_aVariantThatAMissingModuleFileLeftUnmatchedIsTriedAgain() {
        // PR #38: Maven Central did not serve compose ui 1.10.2's module metadata, so Gradle read its POM, whose iOS
        // artifact matches no Android variant.
        val result = run(VARIANT_FROM_A_MISSING_MODULE_FILE, SUCCESS)

        assertEquals(0, result.exitCode)
        assertEquals(2, result.runs)
    }

    @Test
    fun lb002_itGivesUpAfterThreeRuns_withGradlesExitCode() {
        val result = run(PORTAL_NOT_FOUND, PORTAL_NOT_FOUND, PORTAL_NOT_FOUND, SUCCESS)

        assertEquals(1, result.exitCode)
        assertEquals(3, result.runs)
    }

    @Test
    fun lb002_aFailingTestFailsAtOnce() {
        val result = run(FAILING_TEST, SUCCESS)

        assertEquals(1, result.exitCode)
        assertEquals(1, result.runs)
    }

    @Test
    fun lb002_aCompileErrorFailsAtOnce() {
        val result = run(COMPILE_ERROR, SUCCESS)

        assertEquals(1, result.exitCode)
        assertEquals(1, result.runs)
    }

    @Test
    fun lb002_theArgumentsReachGradle_andSuccessNeedsNoRetry() {
        val result = run(SUCCESS)

        assertEquals(0, result.exitCode)
        assertEquals(1, result.runs)
        assertEquals(listOf(":shared:feature:lines:ciUnitTest --quiet"), File(dir, "arguments").readLines())
    }

    private data class Result(
        val exitCode: Int,
        val runs: Int,
    )

    /** Runs the script over a fake Gradle that gives [answers] in turn: its output, and failure unless it is SUCCESS. */
    private fun run(vararg answers: String): Result {
        answers.forEachIndexed { index, answer -> File(dir, "answer-$index").writeText(answer) }
        val gradle =
            File(dir, "gradlew").apply {
                writeText(
                    """
                    #!/usr/bin/env bash
                    runs=${'$'}(cat "${dir.path}/runs" 2>/dev/null || echo 0)
                    echo "${'$'}@" >> "${dir.path}/arguments"
                    echo ${'$'}((runs + 1)) > "${dir.path}/runs"
                    answer=${'$'}(cat "${dir.path}/answer-${'$'}runs")
                    echo "${'$'}answer"
                    [ "${'$'}answer" = "$SUCCESS" ]
                    """.trimIndent(),
                )
                setExecutable(true)
            }
        val process =
            ProcessBuilder("bash", script.path, ":shared:feature:lines:ciUnitTest", "--quiet")
                .directory(dir)
                .redirectErrorStream(true)
                .apply {
                    environment()["GRADLE_COMMAND"] = gradle.path
                    environment()["GRADLE_RETRY_DELAY"] = "0"
                }.start()
        process.inputStream.bufferedReader().readText()
        check(process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "the script did not finish" }
        return Result(process.exitValue(), File(dir, "runs").readText().trim().toInt())
    }

    private companion object {
        const val TIMEOUT_SECONDS = 30L
        const val SUCCESS = "BUILD SUCCESSFUL in 3s"

        // As PR #38's runs printed them.
        const val PORTAL_NOT_FOUND =
            "Plugin [id: 'org.gradle.kotlin.kotlin-dsl', version: '6.7.3'] was not found in any of the following sources:"
        const val RATE_LIMITED =
            "> Could not GET 'https://repo.maven.apache.org/maven2/x.jar'. Received status code 429 from server: " +
                "Too Many Requests"
        const val FAILING_TEST =
            "TabNavigationTest > lb103_x FAILED\n49 tests completed, 1 failed\n" +
                "> There were failing tests. See the report at file:///x/index.html"
        const val VARIANT_FROM_A_MISSING_MODULE_FILE =
            "> Could not resolve org.jetbrains.compose.ui:ui-uikit:1.10.2.\n" +
                "   > No matching variant of org.jetbrains.compose.ui:ui-uikit:1.10.2 was found."
        const val COMPILE_ERROR =
            "e: file:///x/Y.kt:3:5 Unresolved reference 'z'.\n> Compilation error. See log for more details"
    }
}
