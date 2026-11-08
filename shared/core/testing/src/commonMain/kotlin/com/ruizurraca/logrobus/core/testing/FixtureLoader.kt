package com.ruizurraca.logrobus.core.testing

/**
 * The environment variable that carries the absolute path of `fixtures/` from the Gradle build to the test binary
 * (docs/testing_strategy.md §4.1). Both conventions that launch a test process read it: `Test` on the JVM and
 * `KotlinNativeTest` for the iOS binaries.
 */
internal const val FIXTURES_DIRECTORY_VARIABLE: String = "LOGROBUS_FIXTURES_DIR"

/** The golden payloads' directory at the repository root, as the build finds it. */
internal const val FIXTURES_DIRECTORY: String = "fixtures"

/** The file that marks the repository root, which the host tests walk up to when the variable is absent. */
internal const val REPOSITORY_MARKER: String = "settings.gradle.kts"

/**
 * LB-005: the golden payloads that LB-003 recorded into [FIXTURES_DIRECTORY] at the repository root
 * (docs/testing_strategy.md §4.1 and §4.2, `api_spec.md` §9). The contract tests decode the captures through this
 * loader instead of calling the live source (AGENTS.md §9).
 *
 * The directory does not travel in the binary: the Gradle convention passes it to every test task of a module in
 * [FIXTURES_DIRECTORY_VARIABLE], so this module's `commonTest` — and every consumer's — resolves the same files on
 * the JVM and on the iOS simulator.
 */
object FixtureLoader {
    /**
     * The whole text of `fixtures/[name]` (UTF-8), for a test to decode.
     *
     * @throws IllegalStateException when no such file is in the fixtures directory; the message names both the file
     *   and the directory, so a typo reads as a typo rather than as a decode failure.
     */
    fun read(name: String): String = readFixtureFile(name)
}

/**
 * The text of `fixtures/[name]`, on the platform that runs the test.
 *
 * An `expect` in `commonMain` on purpose, and not an implementation per test source set: the declaration has to be
 * visible to `FixtureLoader` here, because consumers call it from their own `commonTest`, whose compilation reads this
 * module's metadata and never its `androidHostTest` or `iosTest` artifacts. Common Kotlin has no file API, so the
 * reading itself is per platform — in `androidMain` and `iosMain`, where the expect/actual rule requires the matching
 * actual to be. That code never reaches production either way: only test configurations may depend on this module
 * (`ModuleRules` refuses it anywhere else), so no app links it.
 */
internal expect fun readFixtureFile(name: String): String

/**
 * The absolute path of the fixtures directory on the platform that runs the test: [FIXTURES_DIRECTORY_VARIABLE] when
 * it is set, or the repository root found above the working directory on the JVM.
 */
internal expect fun fixturesDirectory(): String

/** The one missing-file message both platforms raise, so a test asserts the same shape everywhere. */
internal fun missingFixtureMessage(
    name: String,
    directory: String,
): String =
    "The fixture '$name' is not in '$directory' (set $FIXTURES_DIRECTORY_VARIABLE, docs/testing_strategy.md §4.1)."
