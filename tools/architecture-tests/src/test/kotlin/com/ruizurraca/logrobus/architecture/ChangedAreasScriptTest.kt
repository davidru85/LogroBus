package com.ruizurraca.logrobus.architecture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * scripts/ci/changed-areas.sh decides whether the Kotlin jobs run. The required "Kotlin gate" passes when they
 * are skipped, so a wrong `kotlin=false` would let untested Kotlin changes merge (docs/ci_cd.md §4).
 * Each test builds a throwaway git repository and runs the real script in it.
 */
class ChangedAreasScriptTest {

    @TempDir
    lateinit var repo: File

    private val script: File = repositoryRoot.resolve("scripts/ci/changed-areas.sh")

    @Test
    fun lb002_docsOnlyChangeSkipsTheKotlinJobs() {
        commitBase("README.md")
        commitChange("docs/guide.md")
        assertEquals("kotlin=false", areas("main"))
    }

    @Test
    fun lb002_pushWithoutBaseRunsTheKotlinJobs() {
        commitBase("README.md")
        assertEquals("kotlin=true", areas())
    }

    @Test
    fun lb002_largeDiffWithAnEarlyKotlinPathRunsTheKotlinJobs() {
        commitBase("README.md")
        // ".github/" sorts first, and thousands of later paths overflow the pipe buffer.
        commitChange(".github/workflows/kotlin-checks.yml", *Array(LARGE_DIFF) { "docs/notes/note-$it.md" })
        assertEquals("kotlin=true", areas("main"))
    }

    @Test
    fun lb002_movingAFileOutOfAKotlinPathRunsTheKotlinJobs() {
        commitBase("shared/core/common/src/commonMain/kotlin/Moved.kt")
        File(repo, "docs").mkdirs()
        git("mv","shared/core/common/src/commonMain/kotlin/Moved.kt", "docs/Moved.kt.txt")
        git("commit", "-q", "-m", "change")
        assertEquals("kotlin=true", areas("main"))
    }

    @Test
    fun lb002_fixtureChangeRunsTheKotlinJobs() {
        commitBase("README.md")
        commitChange("fixtures/api-01-lines.json")
        assertEquals("kotlin=true", areas("main"))
    }

    private fun commitBase(vararg paths: String) {
        git("init", "-q", "-b", "main")
        write(*paths)
        git("add", "-A")
        git("commit", "-q", "-m", "base")
        git("checkout", "-q", "-b", "change")
    }

    private fun commitChange(vararg paths: String) {
        write(*paths)
        git("add", "-A")
        git("commit", "-q", "-m", "change")
    }

    private fun write(vararg paths: String) = paths.forEach { path ->
        File(repo, path).apply { parentFile.mkdirs() }.writeText(path)
    }

    private fun areas(vararg base: String): String = run(listOf("bash", script.path) + base).trim()

    private fun git(vararg args: String) {
        run(listOf("git", "-c", "user.name=Test", "-c", "user.email=test@example.invalid") + args)
    }

    private fun run(command: List<String>): String {
        val process = ProcessBuilder(command)
            .directory(repo)
            .redirectErrorStream(true)
            .apply {
                // Isolate the throwaway repository from the developer's git configuration.
                environment()["GIT_CONFIG_GLOBAL"] = "/dev/null"
                environment()["GIT_CONFIG_NOSYSTEM"] = "1"
            }
            .start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS) && process.exitValue() == 0) {
            "${command.joinToString(" ")} failed: $output"
        }
        return output
    }

    private companion object {
        const val LARGE_DIFF = 5_000
        const val TIMEOUT_SECONDS = 60L
    }
}
