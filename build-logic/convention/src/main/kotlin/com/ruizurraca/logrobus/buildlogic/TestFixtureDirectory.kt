package com.ruizurraca.logrobus.buildlogic

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configureEach
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import java.io.File

/** The variable `FixtureLoader` reads in `:shared:core:testing`, on both platforms (docs/testing_strategy.md §4.1). */
private const val FIXTURES_DIRECTORY_VARIABLE = "LOGROBUS_FIXTURES_DIR"

/** The golden payloads' directory, at the repository root (LB-003). */
private const val FIXTURES_DIRECTORY = "fixtures"

/** The file that marks the repository root. */
private const val REPOSITORY_MARKER = "settings.gradle.kts"

/** `simctl spawn`'s prefix for the variables it forwards to the process it starts (LB-005). */
private const val SIMCTL_CHILD_PREFIX = "SIMCTL_CHILD_"

/**
 * LB-005: puts the absolute path of the repository's `fixtures/` in [FIXTURES_DIRECTORY_VARIABLE] on every test task of
 * the module, so `FixtureLoader` (`:shared:core:testing`) resolves the golden payloads from `commonTest` on every
 * target (docs/testing_strategy.md §4.1).
 *
 * An environment variable, not a system property: the JVM test reads either, but a `KotlinNativeTest` binary is a
 * plain process with no JVM to read a property from, and one mechanism has to serve both. The build supplies the path
 * rather than the test deriving a relative one, because the working directory differs per task — a `Test` runs in the
 * module, and Gradle itself may be launched from anywhere — while only the build knows the repository root.
 *
 * Configuration is skipped when the directory is absent, so a checkout without `fixtures/` still configures: a test
 * that then needs a fixture fails with `FixtureLoader`'s own message instead of the whole build failing earlier.
 */
internal fun Project.configureTestFixtureDirectory() {
    val directory = repositoryFixturesDirectory()?.absolutePath ?: return

    // A `KotlinNativeTest` is not a `Test` — it extends `AbstractTestTask` — so both types are configured.
    tasks.withType(Test::class.java).configureEach { environment(FIXTURES_DIRECTORY_VARIABLE, directory) }
    tasks.withType(KotlinNativeTest::class.java).configureEach {
        environment(FIXTURES_DIRECTORY_VARIABLE, directory)
        // `xcrun simctl spawn` starts the test binary inside the simulator and forwards only the variables prefixed
        // with `SIMCTL_CHILD_`, stripping the prefix. Without this second entry the binary sees nothing, which is how
        // the contract tests failed on the simulator while passing on the JVM (PR's own CI run).
        environment(SIMCTL_CHILD_PREFIX + FIXTURES_DIRECTORY_VARIABLE, directory)
    }
}

/**
 * The repository's `fixtures/` directory, or null when this checkout has none.
 *
 * `isolated.rootProject` because a convention plugin reads another project's directory; the walk then starts at that
 * root and moves outwards, since the root project's directory is not necessarily the repository's.
 */
private fun Project.repositoryFixturesDirectory(): File? {
    val root =
        generateSequence(isolated.rootProject.projectDirectory.asFile.absoluteFile) { it.parentFile }
            .firstOrNull { File(it, REPOSITORY_MARKER).isFile && File(it, FIXTURES_DIRECTORY).isDirectory }
            ?: return null
    return File(root, FIXTURES_DIRECTORY)
}
