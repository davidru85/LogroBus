package com.ruizurraca.logrobus.buildlogic

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Which configurations get the patched Netty (GHSA-c4c3-7fpv-j4q5, docs/ci_cd.md §6). */
class UnifiedTestPlatformTest {
    @Test
    fun lb002_everyUnifiedTestPlatformConfigurationGetsThePatchedNetty() {
        // The two configurations of AGP 9.3 that resolve Netty, through gRPC.
        listOf(
            "unified-test-platform-core",
            "unified-test-platform-android-test-plugin-host-emulator-control",
        ).forEach { assertTrue(isUnifiedTestPlatformConfiguration(it), it) }
    }

    @Test
    fun lb002_appAndUnitTestClasspathsAreLeftAlone() {
        listOf(
            "implementation",
            "debugRuntimeClasspath",
            "releaseRuntimeClasspath",
            "debugUnitTestRuntimeClasspath",
            "debugAndroidTestRuntimeClasspath",
        ).forEach { assertFalse(isUnifiedTestPlatformConfiguration(it), it) }
    }
}
