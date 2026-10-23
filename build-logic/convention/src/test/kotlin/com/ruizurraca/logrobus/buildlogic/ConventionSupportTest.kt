package com.ruizurraca.logrobus.buildlogic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Pure helpers used by the KMP convention plugin (namespaces, dependency buckets checked by ModuleRules). */
class ConventionSupportTest {

    @Test
    fun lb001_namespaceIsDerivedFromModulePath() {
        assertEquals("com.ruizurraca.logrobus.core.common", moduleNamespace(":shared:core:common"))
        assertEquals("com.ruizurraca.logrobus.feature.lines", moduleNamespace(":shared:feature:lines"))
        assertEquals("com.ruizurraca.logrobus.umbrella", moduleNamespace(":shared:umbrella"))
    }

    @Test
    fun lb001_androidNamespacesKeepThePlatformSegment() {
        // Otherwise :android:feature:lines would clash with the Android target of :shared:feature:lines.
        assertEquals("com.ruizurraca.logrobus.android.feature.lines", moduleNamespace(":android:feature:lines"))
        assertEquals("com.ruizurraca.logrobus.android.core.designsystem", moduleNamespace(":android:core:designsystem"))
        assertEquals("com.ruizurraca.logrobus.android.app", moduleNamespace(":android:app"))
    }

    @Test
    fun lb001_productionBucketsIncludeEveryMainDeclarationKind() {
        listOf(
            "commonMainApi",
            "commonMainImplementation",
            "commonMainCompileOnly",
            "commonMainRuntimeOnly",
            "iosMainImplementation",
            "androidMainRuntimeOnly",
            "api",
            "implementation",
            "compileOnly",
            "runtimeOnly",
        ).forEach { assertTrue(isProductionDependencyBucket(it), it) }
    }

    @Test
    fun lb001_testAndToolingBucketsAreNotProduction() {
        listOf(
            "commonTestImplementation",
            "androidHostTestImplementation",
            "androidDeviceTestRuntimeOnly",
            "iosTestApi",
            "testImplementation",
            "testRuntimeOnly",
            "kotlinCompilerPluginClasspath",
            "commonMainImplementationDependenciesMetadata",
        ).forEach { assertFalse(isProductionDependencyBucket(it), it) }
    }
}
