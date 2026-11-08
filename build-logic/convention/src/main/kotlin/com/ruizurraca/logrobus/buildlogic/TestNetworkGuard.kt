package com.ruizurraca.logrobus.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency

/** LB-003: the module that holds the test-scope network guard. Only test configurations may use it (ModuleRules). */
internal const val SHARED_TESTING_MODULE = ":shared:core:testing"

/**
 * The test-support modules never depend on the guard, so it never trails into their consumers:
 * `:shared:core:testing` finds its own resources on the host test runtime classpath through AGP's implicit
 * self-dependency, and `:android:core:testing`'s tests make no request.
 */
private val TEST_SUPPORT_MODULES = setOf(SHARED_TESTING_MODULE, ":android:core:testing")

/**
 * Puts [SHARED_TESTING_MODULE] — and with it the JDK `InetAddressResolverProvider` registered in its
 * `META-INF/services` — on the JVM test classpath of the module, so that no unit or integration test resolves a real
 * host (LB-003, docs/testing_strategy.md §1 rule 2). [addToTestClasspath] declares the dependency in the bucket that
 * each convention owns: `androidHostTest` for the shared modules, `testImplementation` for the Android ones.
 *
 * Test buckets only. Production classpaths must never carry it: [ModuleRules] refuses a testing module outside a test
 * configuration, and that refusal is the reason this helper only ever hands the dependency to a test one.
 *
 * `:tools:architecture-tests` is a plain JVM module that makes no HTTP call, so it is left alone: nothing there needs
 * the guard.
 */
internal fun Project.configureTestNetworkGuard(addToTestClasspath: (Dependency) -> Unit) {
    if (path in TEST_SUPPORT_MODULES) return
    addToTestClasspath(dependencies.project(mapOf("path" to SHARED_TESTING_MODULE)))
}
