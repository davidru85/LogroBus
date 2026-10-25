package com.ruizurraca.logrobus.buildlogic

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Which configurations get the security floors for AGP's tools (docs/ci_cd.md §6). */
class AgpToolConfigurationTest {
    @Test
    fun lb002_agpToolConfigurationsGetTheSecurityFloors() {
        // AGP 9.3's instrumented test runner and Android lint, which bring Netty and Bouncy Castle.
        listOf(
            "unified-test-platform-core",
            "unified-test-platform-android-test-plugin-host-emulator-control",
            "unified-test-platform-android-test-plugin-result-listener-gradle",
            "androidLintTool",
        ).forEach { assertTrue(isAgpToolConfiguration(it), it) }
    }

    @Test
    fun lb002_appAndUnitTestClasspathsAreLeftAlone() {
        listOf(
            "implementation",
            "lintChecks",
            "debugRuntimeClasspath",
            "releaseRuntimeClasspath",
            "debugUnitTestRuntimeClasspath",
            "debugAndroidTestRuntimeClasspath",
        ).forEach { assertFalse(isAgpToolConfiguration(it), it) }
    }
}
