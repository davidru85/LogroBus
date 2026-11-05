package com.ruizurraca.logrobus.buildlogic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Which configurations get the security floors of the build's tools and tests (docs/ci_cd.md §6). */
class SecurityFloorsTest {
    @Test
    fun lb002_agpToolConfigurationsGetTheSecurityFloors() {
        // AGP 9.3's instrumented test runner and Android lint, which bring Netty, Bouncy Castle, Guava, Commons Lang
        // and HttpClient.
        listOf(
            "unified-test-platform-core",
            "unified-test-platform-android-test-plugin-host-emulator-control",
            "unified-test-platform-android-test-plugin-result-listener-gradle",
            "androidLintTool",
        ).forEach { assertTrue(getsSecurityFloors(it), it) }
    }

    @Test
    fun lb002_swiftExportAndTestDependenciesGetTheSecurityFloors() {
        // KGP's Swift export, which brings OpenTelemetry, and the unit and host test dependencies, where Robolectric
        // brings Guava.
        listOf(
            "swiftExportClasspath",
            "testImplementation",
            "androidHostTestImplementation",
        ).forEach { assertTrue(getsSecurityFloors(it), it) }
    }

    @Test
    fun lb002_swiftExportClasspathGetsNoBoms() {
        // KGP adds the Swift export tools as default dependencies, which a BOM (a dependency) would remove: the
        // classpath gets the plain constraints alone. The resolution itself is checked in the 2026-11-02 project log
        // entry. AGP's tools and the tests declare their own dependencies, so they also get the BOMs.
        assertFalse(getsSecurityFloorBoms("swiftExportClasspath"))
        listOf(
            "unified-test-platform-core",
            "androidLintTool",
            "testImplementation",
            "androidHostTestImplementation",
        ).forEach { assertTrue(getsSecurityFloorBoms(it), it) }
        assertFalse(getsSecurityFloorBoms("implementation"))
    }

    @Test
    fun lb002_aTestSupportModulesOwnDependenciesGetTheSecurityFloors() {
        // LB-406's `:android:core:testing` brings Robolectric, and with it Guava, as a dependency of its own. Only
        // tests may use a test-support module (ModuleRules), so its own classpaths are test classpaths too.
        assertTrue(isTestSupportModule(":android:core:testing"))
        assertTrue(isTestSupportModule(":shared:core:testing"))
        listOf(":android:core:designsystem", ":android:app", ":shared:core:common", ":shared:umbrella").forEach {
            assertFalse(isTestSupportModule(it), it)
        }
        assertTrue(getsSecurityFloors("implementation", testSupport = true))
        assertTrue(getsSecurityFloorBoms("implementation", testSupport = true))
    }

    @Test
    fun lb002_eachConfigurationOfEachModuleGetsItsFloors() {
        // What raiseSecurityFloors applies, by module and configuration.
        assertEquals(Floors.CONSTRAINTS_AND_BOMS, securityFloorsFor(":android:core:testing", "implementation"))
        // A KMP module puts a JVM library, such as Robolectric, in androidMain; common code also targets iOS.
        assertEquals(
            Floors.CONSTRAINTS_AND_BOMS,
            securityFloorsFor(":shared:core:testing", "androidMainImplementation"),
        )
        assertEquals(Floors.NONE, securityFloorsFor(":shared:core:testing", "commonMainImplementation"))
        assertEquals(Floors.CONSTRAINTS_AND_BOMS, securityFloorsFor(":android:app", "testImplementation"))
        assertEquals(Floors.CONSTRAINTS, securityFloorsFor(":shared:umbrella", "swiftExportClasspath"))
        listOf(":android:app", ":android:core:designsystem", ":shared:core:common", ":shared:umbrella").forEach {
            assertEquals(Floors.NONE, securityFloorsFor(it, "implementation"), it)
            assertEquals(Floors.NONE, securityFloorsFor(it, "androidMainImplementation"), it)
        }
    }

    @Test
    fun lb002_appClasspathsAreLeftAlone() {
        // The app's versions are the catalog's: a floor never changes what ships.
        listOf(
            "implementation",
            "commonMainImplementation",
            "androidMainImplementation",
            "lintChecks",
            "debugRuntimeClasspath",
            "releaseRuntimeClasspath",
            "debugAndroidTestRuntimeClasspath",
        ).forEach { assertFalse(getsSecurityFloors(it), it) }
    }
}
