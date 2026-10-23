package com.ruizurraca.logrobus.architecture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for the pure rules behind ArchitectureTest, with known good and bad samples. */
class ArchitectureRulesTest {

    private val root = "com.ruizurraca.logrobus"

    private fun crossFeature(packageName: String, vararg imports: String) =
        ArchitectureRules.crossFeatureImports(packageName, imports.toList())

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
        val own = "$root.feature.lines.presentation"
        assertEquals(emptyList<String>(), crossFeature(own, "$root.feature.lines.domain.ObserveLines"))
        assertEquals(1, crossFeature(own, "$root.feature.stops.domain.X").size)
    }

    @Test
    fun lb001_umbrellaMayImportFeaturesButCoreDomainAndDataMayNot() {
        val featureImport = "$root.feature.lines.domain.ObserveLines"
        assertEquals(emptyList<String>(), crossFeature("$root.umbrella", featureImport))
        assertEquals(1, crossFeature("$root.core.common", featureImport).size)
        assertEquals(1, crossFeature("$root.transit.domain", featureImport).size)
        assertEquals(1, crossFeature("$root.transit.data", featureImport).size)
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
            "$root.transit.data.LinesApi",
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
    fun lb002_workflowMatrixModulesAreParsedFromQuotedAndPlainItems() {
        val workflow = """
            jobs:
              shared-unit-tests:
                strategy:
                  matrix:
                    module:
                      - ":shared:core:common"
                      - :shared:feature:lines
                steps:
                  - run: ./gradlew "${'$'}MODULE:ciUnitTest"
                  - uses: actions/checkout@v7
        """.trimIndent()
        assertEquals(
            setOf(":shared:core:common", ":shared:feature:lines"),
            ArchitectureRules.workflowMatrixModules(workflow),
        )
    }

    @Test
    fun lb002_gateMustWaitForEveryOtherJob() {
        val workflow = """
            jobs:
              changes:
                runs-on: ubuntu-latest
              tests:
                needs: changes
              build:
                needs: changes
              ci-gate:
                needs: [changes, tests]
                steps:
                  - run: echo done
        """.trimIndent()
        assertEquals(listOf("build"), ArchitectureRules.jobsMissingFromGate(workflow, gate = "ci-gate"))
        val complete = workflow.replace("needs: [changes, tests]", "needs: [changes, tests, build]")
        assertEquals(emptyList<String>(), ArchitectureRules.jobsMissingFromGate(complete, gate = "ci-gate"))
    }

    @Test
    fun lb001_iosFeatureTargetsMayNotDependOnOtherFeatures() {
        val manifest = """
            targets: [
                .binaryTarget(name: "Shared", path: "Frameworks/Shared.xcframework"),
                .target(name: "DesignSystem"),
                .target(name: "FeatureLines", dependencies: ["DesignSystem", "Shared", "FeatureStops"]),
                .target(name: "FeatureStops", dependencies: ["DesignSystem", .target(name: "FeatureNearMe")]),
                .target(name: "FeatureNearMe", dependencies: ["DesignSystem", "Shared"]),
            ]
        """.trimIndent()
        assertEquals(
            listOf("FeatureLines -> FeatureStops", "FeatureStops -> FeatureNearMe"),
            ArchitectureRules.crossFeatureSwiftDependencies(manifest),
        )
    }

    @Test
    fun lb002_jobNamesWithExpressionsAreFound() {
        val workflow = """
            jobs:
              unit:
                name: "Shared unit tests: ${'$'}{{ matrix.module }}"
                steps:
                  - name: Unit tests ${'$'}{{ matrix.module }}
              lint:
                name: Android lint
        """.trimIndent()
        assertEquals(
            listOf("\"Shared unit tests: ${'$'}{{ matrix.module }}\""),
            ArchitectureRules.jobNamesWithExpressions(workflow),
        )
    }

    @Test
    fun lb001_expectedNamespaceFollowsTheModulePath() {
        assertEquals(
            "$root.feature.lines",
            ArchitectureRules.expectedNamespace("shared/feature/lines/src/commonMain/kotlin/com/x/A.kt"),
        )
        assertEquals("$root.umbrella", ArchitectureRules.expectedNamespace("shared/umbrella/src/iosMain/kotlin/A.kt"))
    }

    @Test
    fun lb001_androidNamespacesKeepThePlatformSegment() {
        assertEquals("$root.android.app", ArchitectureRules.expectedNamespace("android/app/src/main/kotlin/A.kt"))
        assertEquals(
            "$root.android.feature.lines",
            ArchitectureRules.expectedNamespace("android/feature/lines/src/test/kotlin/A.kt"),
        )
    }
}
