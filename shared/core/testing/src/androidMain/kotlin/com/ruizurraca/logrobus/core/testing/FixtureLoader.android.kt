package com.ruizurraca.logrobus.core.testing

import java.io.File

/**
 * The JVM side of [readFixtureFile] and [fixturesDirectory] (docs/testing_strategy.md §4.1).
 *
 * `androidMain` and not `androidHostTest`, although only the host tests read fixtures: the `expect` lives in
 * `commonMain`, so Kotlin requires its `actual` in every main compilation of the target, and a test source set is
 * compiled after that check. Nothing ships it anyway — `ModuleRules` lets only test configurations depend on this
 * module.
 */

internal actual fun readFixtureFile(name: String): String {
    val directory = fixturesDirectory()
    val file = File(directory, name)
    check(file.isFile) { missingFixtureMessage(name, directory) }
    return file.readText()
}

/**
 * [FIXTURES_DIRECTORY_VARIABLE] first: the build sets it on every test task, so this is the path in CI and in a normal
 * local run. The walk up from `user.dir` is the fallback for a test launched from an IDE, whose working directory is
 * the module rather than the repository root.
 */
internal actual fun fixturesDirectory(): String {
    System.getenv(FIXTURES_DIRECTORY_VARIABLE)?.let { return it }
    return repositoryFixturesDirectory()
}

/** Walks up from the working directory to the first one holding both `settings.gradle.kts` and `fixtures/`. */
private fun repositoryFixturesDirectory(): String {
    val start = System.getProperty("user.dir")?.let(::File) ?: File(".")
    val root =
        generateSequence(start.absoluteFile) { it.parentFile }
            .firstOrNull { File(it, REPOSITORY_MARKER).isFile && File(it, FIXTURES_DIRECTORY).isDirectory }
            ?: error(
                "$FIXTURES_DIRECTORY_VARIABLE is not set and no directory above '$start' holds " +
                    "$REPOSITORY_MARKER and $FIXTURES_DIRECTORY (docs/testing_strategy.md §4.1).",
            )
    return File(root, FIXTURES_DIRECTORY).absolutePath
}
