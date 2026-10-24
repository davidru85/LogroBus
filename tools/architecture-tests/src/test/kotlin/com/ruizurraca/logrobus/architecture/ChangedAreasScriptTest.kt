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
        assertEquals("false", area("kotlin", "main"))
    }

    @Test
    fun lb002_pushWithoutBaseRunsTheKotlinJobs() {
        commitBase("README.md")
        assertEquals("true", area("kotlin"))
    }

    @Test
    fun lb002_largeDiffWithAnEarlyKotlinPathRunsTheKotlinJobs() {
        commitBase("README.md")
        // ".github/" sorts first, and thousands of later paths overflow the pipe buffer.
        commitChange(".github/workflows/kotlin-checks.yml", *Array(LARGE_DIFF) { "docs/notes/note-$it.md" })
        assertEquals("true", area("kotlin", "main"))
    }

    @Test
    fun lb002_movingAFileOutOfAKotlinPathRunsTheKotlinJobs() {
        commitBase("shared/core/common/src/commonMain/kotlin/Moved.kt")
        File(repo, "docs").mkdirs()
        git("mv", "shared/core/common/src/commonMain/kotlin/Moved.kt", "docs/Moved.kt.txt")
        git("commit", "-q", "-m", "change")
        assertEquals("true", area("kotlin", "main"))
    }

    @Test
    fun lb002_fixtureChangeRunsTheKotlinJobs() {
        commitBase("README.md")
        commitChange("fixtures/api-01-lines.json")
        assertEquals("true", area("kotlin", "main"))
    }

    @Test
    fun lb002_iosFilesReadByTheArchitectureTestsRunTheKotlinJobs() {
        // ArchitectureTest reads the XcodeGen spec, the package manifest and .gitignore.
        listOf("iosApp/project.yml", "iosApp/Packages/LogroBusUI/Package.swift", ".gitignore").forEach { path ->
            repo.listFiles()?.forEach { it.deleteRecursively() }
            commitBase("README.md")
            commitChange(path)
            assertEquals("true", area("kotlin", "main"), path)
        }
    }

    @Test
    fun lb002_lintConfigurationChangesRunTheKotlinJobs() {
        // ktlint reads .editorconfig and detekt reads config/; a config-only PR must still run both.
        listOf("config/detekt/detekt.yml", ".editorconfig").forEach { path ->
            repo.listFiles()?.forEach { it.deleteRecursively() }
            commitBase("README.md")
            commitChange(path)
            assertEquals("true", area("kotlin", "main"), path)
        }
    }

    @Test
    fun lb002_docsOnlyChangeSkipsTheIosJobs() {
        commitBase("README.md")
        commitChange("docs/guide.md")
        assertEquals("false", area("ios", "main"))
    }

    @Test
    fun lb002_pushWithoutBaseRunsTheIosJobs() {
        commitBase("README.md")
        assertEquals("true", area("ios"))
    }

    @Test
    fun lb002_iosRelevantChangesRunTheIosJobs() {
        // The Swift code, its lint config, the shared Kotlin it links, the build that produces the framework,
        // the version it reads and its own CI scripts.
        val paths =
            listOf(
                "iosApp/iosApp/LogroBusApp.swift",
                ".swiftlint.yml",
                "shared/core/common/src/commonMain/kotlin/A.kt",
                "gradle/libs.versions.toml",
                "version.properties",
                "scripts/ios/test.sh",
                ".github/workflows/ios-checks.yml",
            )
        paths.forEach { path ->
            repo.listFiles()?.forEach { it.deleteRecursively() }
            commitBase("README.md")
            commitChange(path)
            assertEquals("true", area("ios", "main"), path)
        }
    }

    @Test
    fun lb002_androidOnlyChangeSkipsTheIosJobs() {
        commitBase("README.md")
        commitChange("android/app/src/main/kotlin/A.kt")
        assertEquals("false", area("ios", "main"))
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

    private fun write(vararg paths: String) =
        paths.forEach { path ->
            // A comment line: unique per path, and inert even in a .gitignore, which would otherwise ignore itself.
            File(repo, path).apply { parentFile.mkdirs() }.writeText("# $path\n")
        }

    /** The value the script prints for [name] (`<name>=true|false`), or null if it prints no such line. */
    private fun area(
        name: String,
        vararg base: String,
    ): String? =
        run(listOf("bash", script.path) + base).lines().firstOrNull { it.startsWith("$name=") }?.substringAfter('=')

    private fun git(vararg args: String) {
        // No background gc or maintenance: thousands of loose objects can start it, and it would still be
        // writing to .git while JUnit deletes the temporary directory.
        val config =
            listOf(
                "user.name=Test",
                "user.email=test@example.invalid",
                "gc.auto=0",
                "maintenance.auto=false",
            )
        run(listOf("git") + config.flatMap { listOf("-c", it) } + args)
    }

    private fun run(command: List<String>): String {
        val process =
            ProcessBuilder(command)
                .directory(repo)
                .redirectErrorStream(true)
                .apply {
                    // Isolate the throwaway repository from the developer's git configuration.
                    environment()["GIT_CONFIG_GLOBAL"] = "/dev/null"
                    environment()["GIT_CONFIG_NOSYSTEM"] = "1"
                }.start()
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
