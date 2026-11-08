package com.ruizurraca.logrobus.buildlogic

import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configureEach
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

/** The catalog aliases of Robolectric's Android framework jars: one per SDK the tests instantiate. */
private val FRAMEWORK_JAR_ALIASES =
    listOf(
        "robolectric-android-sdk-33",
        "robolectric-android-sdk-36",
        "robolectric-android-sdk-37",
        // The shared KMP modules' host tests name no targetSdkVersion, so Robolectric falls back to API 23.
        "robolectric-android-sdk-legacy",
    )

/** The copy task that fills the directory; every `Test` task of the module depends on it. */
private const val COPY_FRAMEWORK_JARS_TASK = "copyRobolectricFrameworkJars"

/** Where the jars are copied, under the module's own build directory. */
private const val FRAMEWORK_JAR_DIRECTORY = "robolectric-framework-jars"

/** `robolectric.offline`: resolve only from `robolectric.dependency.dir`, never over the network. */
private const val OFFLINE_PROPERTY = "robolectric.offline"

/** `robolectric.dependency.dir`: the directory `LocalDependencyResolver` reads `<artifactId>-<version>.jar` from. */
private const val DEPENDENCY_DIRECTORY_PROPERTY = "robolectric.dependency.dir"

/**
 * LB-003: the test-scope network guard (docs/testing_strategy.md §1 rule 2) makes every hostname unresolvable in the
 * test JVM, and Robolectric fetches its Android framework jar over HTTP from that same JVM unless it finds it on disk
 * or is told not to. So the build resolves the jars once, with Gradle's retries and cache, and copies them exactly
 * where Robolectric looks: [configureTestNetworkGuard] stays absolute, with no carve-out for Maven Central.
 *
 * Both properties are needed, and neither is enough alone:
 * - `robolectric.dependency.dir` selects `LocalDependencyResolver`, which opens `<dir>/<artifactId>-<version>.jar`,
 *   the resolved file name unchanged, so the copy must not rename anything.
 * - `robolectric.offline=true` makes a jar missing from that directory a failure. Without it the resolver is only
 *   preferred: `LegacyDependencyResolver.pickOne` still falls through to `MavenDependencyResolver`, which would reach
 *   the network.
 *
 * The tradeoff: the first resolution downloads about 590 MB for the three SDKs (and 76 MB more for the shared modules' API 23 jar), once per module that runs Robolectric
 * tests, all from Gradle's cache. That is the price of the guard staying absolute: Robolectric's own fetch went into
 * `~/.m2`, which CI never caches and which docs/project_log.md records already failing a job on a network error.
 *
 * Nothing is resolved until a `Test` task runs, so a module whose tests never run — a KMP module with host tests
 * disabled, say — resolves and copies nothing. A module that runs only plain JUnit tests pays the copy and ignores
 * it.
 */
internal fun Project.configureRobolectricOffline() {
    // One configuration per SDK, each with a single dependency. One configuration cannot hold all three: it is the same
    // module, `org.robolectric:android-all-instrumented`, in three versions, so Gradle would conflict-resolve them to
    // the highest and copy one jar, leaving the tests that ask for SDK 33 or 36 without a file.
    val jarConfigurations =
        FRAMEWORK_JAR_ALIASES.map { alias ->
            configurations
                .create("${ROBOLECTRIC_FRAMEWORK_JARS_PREFIX}${alias.substringAfterLast('-')}") {
                    isCanBeConsumed = false
                    isCanBeResolved = true
                    description =
                        "Robolectric's Android $alias framework jar, copied for its offline resolver (LB-003)."
                }.also { jars -> dependencies.add(jars.name, libs.findLibrary(alias).get()) }
        }

    // The module's own build directory, the same one its `Test` tasks read from below.
    val directory = project.layout.buildDirectory.dir(FRAMEWORK_JAR_DIRECTORY)
    val copyJars =
        tasks.register<Sync>(COPY_FRAMEWORK_JARS_TASK) {
            group = "verification"
            description = "Copies Robolectric's Android framework jars for its offline resolver (LB-003)."
            from(jarConfigurations)
            into(directory)
        }

    // The absolute path of the directory above; known while the build is configured.
    val jarDirectory = directory.get().asFile.absolutePath
    tasks.withType(Test::class.java).configureEach {
        systemProperty(OFFLINE_PROPERTY, "true")
        systemProperty(DEPENDENCY_DIRECTORY_PROPERTY, jarDirectory)
        dependsOn(copyJars)
    }
}

/** The prefix of the per-SDK resolvable configurations; not a production bucket ([ModuleRules] never sees them). */
private const val ROBOLECTRIC_FRAMEWORK_JARS_PREFIX = "robolectricFrameworkJars"
