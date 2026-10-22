package com.ruizurraca.logrobus.architecture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for the pure rules behind ArchitectureTest, with known good and bad samples. */
class ArchitectureRulesTest {

    @Test
    fun lb001_includesAreParsedFromMultiArgumentAndMultilineCalls() {
        val settings = """
            include(":a")
            include(":b", ":c")
            include(
                ":d",
                ":e",
            )
            include ( ":f" )
        """.trimIndent()
        assertEquals(setOf(":a", ":b", ":c", ":d", ":e", ":f"), ArchitectureRules.includedModules(settings))
    }

    @Test
    fun lb001_versionLiteralsAreDetectedInEveryForm() {
        val samples = listOf(
            """implementation("io.ktor:ktor-client-core:3.3.1")""",
            """implementation("g:n:+")""",
            """implementation(group = "g", name = "n", version = "1.0")""",
            """implementation("g:n") { version { strictly("1.0") } }""",
            """id("x") version "1.0"""",
            "val v = \"1.0\"\nimplementation(\"io.ktor:ktor-client-core:\$v\")",
        )
        samples.forEach { assertTrue(ArchitectureRules.versionLiterals(it).isNotEmpty(), it) }
    }

    @Test
    fun lb001_versionLiteralsIgnoreCatalogAndProjectReferences() {
        val clean = """
            implementation(libs.konsist)
            api(projects.shared.core.common)
            jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
            implementation(kotlin("test"))
        """.trimIndent()
        assertEquals(emptyList<String>(), ArchitectureRules.versionLiterals(clean))
    }

    @Test
    fun lb001_featureMayImportItselfButNotAnotherFeature() {
        val own = "com.ruizurraca.logrobus.feature.lines.presentation"
        assertEquals(
            emptyList<String>(),
            ArchitectureRules.crossFeatureImports(own, listOf("com.ruizurraca.logrobus.feature.lines.domain.ObserveLines")),
        )
        assertEquals(
            1,
            ArchitectureRules.crossFeatureImports(own, listOf("com.ruizurraca.logrobus.feature.stops.domain.X")).size,
        )
    }

    @Test
    fun lb001_umbrellaMayImportFeaturesButCoreDomainAndDataMayNot() {
        val featureImport = listOf("com.ruizurraca.logrobus.feature.lines.domain.ObserveLines")
        assertEquals(emptyList<String>(), ArchitectureRules.crossFeatureImports("com.ruizurraca.logrobus.umbrella", featureImport))
        assertEquals(1, ArchitectureRules.crossFeatureImports("com.ruizurraca.logrobus.core.common", featureImport).size)
        assertEquals(1, ArchitectureRules.crossFeatureImports("com.ruizurraca.logrobus.transit.domain", featureImport).size)
        assertEquals(1, ArchitectureRules.crossFeatureImports("com.ruizurraca.logrobus.transit.data", featureImport).size)
    }

    @Test
    fun lb001_domainMayNotImportJvmPlatformInfrastructureOrData() {
        val imports = listOf(
            "java.io.File",
            "javax.inject.Inject",
            "dalvik.system.DexFile",
            "android.content.Context",
            "platform.Foundation.NSDate",
            "io.ktor.client.HttpClient",
            "com.ruizurraca.logrobus.transit.data.LinesApi",
            "kotlin.time.Instant",
        )
        assertEquals(7, ArchitectureRules.forbiddenDomainImports(imports).size)
    }

    @Test
    fun lb001_presentationMayNotImportInfrastructure() {
        val imports = listOf("io.ktor.client.HttpClient", "app.cash.sqldelight.db.SqlDriver", "kotlin.time.Instant")
        assertEquals(2, ArchitectureRules.forbiddenPresentationImports(imports).size)
    }

    @Test
    fun lb001_expectedNamespaceFollowsTheModulePath() {
        assertEquals(
            "com.ruizurraca.logrobus.feature.lines",
            ArchitectureRules.expectedNamespace("shared/feature/lines/src/commonMain/kotlin/com/x/A.kt"),
        )
        assertEquals("com.ruizurraca.logrobus.umbrella", ArchitectureRules.expectedNamespace("shared/umbrella/src/iosMain/kotlin/A.kt"))
    }
}
