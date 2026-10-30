package com.ruizurraca.logrobus.architecture

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Unit tests for the pure rules behind ArchitectureTest, with known good and bad samples. */
class ArchitectureRulesTest {
    private val root = "com.ruizurraca.logrobus"

    private fun crossFeature(
        packageName: String,
        vararg imports: String,
    ) = ArchitectureRules.crossFeatureImports(packageName, imports.toList())

    @Test
    fun lb001_includesAreParsedFromMultiArgumentAndMultilineCalls() {
        val settings =
            """
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
        val samples =
            listOf(
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
        val clean =
            """
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
        val imports =
            listOf(
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
        val workflow =
            """
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
        val workflow =
            """
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
    fun lb002_jobNeedsAreParsedFromSingleAndListForms() {
        val workflow =
            """
            jobs:
              changes:
                runs-on: ubuntu-latest
              graph:
                needs: changes
              review:
                needs: [changes, graph]
                steps:
                  - run: echo done
            """.trimIndent()
        assertEquals(emptySet<String>(), ArchitectureRules.jobNeeds(workflow, job = "changes"))
        assertEquals(setOf("changes"), ArchitectureRules.jobNeeds(workflow, job = "graph"))
        assertEquals(setOf("changes", "graph"), ArchitectureRules.jobNeeds(workflow, job = "review"))
    }

    @Test
    fun lb001_iosFeatureTargetsMayNotDependOnOtherFeatures() {
        val manifest =
            """
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
        val workflow =
            """
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

    @Test
    fun lb011_expressiveApisAreFlaggedOutsideTheDesignSystem() {
        val optIn = "@OptIn(ExperimentalMaterial3ExpressiveApi::class)\nfun Screen() {}"
        val import = "import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi"
        val feature = "com.ruizurraca.logrobus.android.feature.lines"

        assertTrue(ArchitectureRules.usesExpressiveApiOutsideDesignSystem(feature, optIn))
        assertTrue(ArchitectureRules.usesExpressiveApiOutsideDesignSystem(feature, import))
        assertTrue(ArchitectureRules.usesExpressiveApiOutsideDesignSystem("com.ruizurraca.logrobus.android.app", optIn))
    }

    @Test
    fun lb011_expressiveApisAreAllowedInTheDesignSystemOnly() {
        val optIn = "@OptIn(ExperimentalMaterial3ExpressiveApi::class)"
        val designSystem = "com.ruizurraca.logrobus.android.core.designsystem"

        assertFalse(ArchitectureRules.usesExpressiveApiOutsideDesignSystem(designSystem, optIn))
        assertFalse(ArchitectureRules.usesExpressiveApiOutsideDesignSystem("$designSystem.component", optIn))
        // A package that only starts with the same letters is not the design system.
        assertTrue(ArchitectureRules.usesExpressiveApiOutsideDesignSystem("${designSystem}extra", optIn))
        assertFalse(
            ArchitectureRules.usesExpressiveApiOutsideDesignSystem("com.ruizurraca.logrobus.android.app", "fun A() {}"),
        )
    }

    @Test
    fun lb011_aJobsMatrixModulesAreReadFromThatJobOnly() {
        val workflow =
            """
            jobs:
              unit-tests:
                strategy:
                  matrix:
                    module:
                      - ":android:app"
                      - ":android:core:designsystem"
              screenshots:
                strategy:
                  matrix:
                    module:
                      - ":android:core:designsystem"
                      - :android:feature:settings
                steps:
                  - run: ./gradlew "${'$'}MODULE:verifyRoborazziDebug"
            """.trimIndent()

        assertEquals(
            setOf(":android:core:designsystem", ":android:feature:settings"),
            ArchitectureRules.jobMatrixModules(workflow, "screenshots"),
        )
        assertEquals(
            setOf(":android:app", ":android:core:designsystem"),
            ArchitectureRules.jobMatrixModules(workflow, "unit-tests"),
        )
    }

    @Test
    fun lb011_aSwiftTargetsDependenciesAreReadFromItsLine() {
        val manifest =
            """
            .target(name: "DesignSystem", dependencies: ["Shared"], linkerSettings: linksShared),
            .target(name: "MapUI", dependencies: ["DesignSystem"]),
            .target(name: "FeatureLines", dependencies: ["DesignSystem", "MapUI", "Shared"], linkerSettings: x)
            """.trimIndent()

        assertEquals(listOf("Shared"), ArchitectureRules.swiftTargetDependencies(manifest, "DesignSystem"))
        assertEquals(
            listOf("DesignSystem", "MapUI", "Shared"),
            ArchitectureRules.swiftTargetDependencies(manifest, "FeatureLines"),
        )
    }

    @Test
    fun lb011_swiftPackagesPinnedTwiceMustAgree() {
        val manifest =
            """package.dependencies.append(.package(url: "https://github.com/pointfreeco/swift-snapshot-testing", exact: "1.19.7"))"""
        val agreeing =
            """
            packages:
              SnapshotTesting:
                url: https://github.com/pointfreeco/swift-snapshot-testing
                exactVersion: 1.19.7
            """.trimIndent()

        assertEquals(emptyList<String>(), ArchitectureRules.swiftPackageVersionMismatches(manifest, agreeing))
        assertEquals(
            listOf("swift-snapshot-testing: 1.19.7 in Package.swift, 1.20.0 in project.yml"),
            ArchitectureRules.swiftPackageVersionMismatches(manifest, agreeing.replace("1.19.7", "1.20.0")),
        )
    }

    @Test
    fun lb017_telemetryPropertiesMayBeIdsCountsFlagsAndTelemetryEnums() {
        val properties =
            mapOf(
                "StopView.stopId" to "Int",
                "Sample.total" to "kotlin.Long",
                "PlanRequest.originIsCurrentLocation" to "Boolean",
                "PlanResult.rejection" to "Rejection?",
                "FavouriteAdd.kind" to "AnalyticsEvent.FavouriteAdd.Kind",
            )

        assertEquals(
            emptyList<String>(),
            ArchitectureRules.forbiddenTelemetryProperties(properties, telemetryEnums = setOf("Rejection", "Kind")),
        )
    }

    @Test
    fun lb008_databasesAndSeedFilesAreSeedData() {
        val seeds =
            listOf(
                "android/app/src/main/assets/logrobus.db",
                "iosApp/iosApp/Resources/catalogue.sqlite",
                "shared/core/database/src/commonMain/resources/Seed-lines.json",
                "iosApp/iosApp/stops.sqlite3",
            )
        assertEquals(seeds, seeds.filter(ArchitectureRules::isSeedData))
    }

    @Test
    fun lb008_otherResourcesAreNotSeedData() {
        val others =
            listOf(
                "android/app/src/main/res/values/strings.xml",
                "fixtures/lines_2026-10-22.json",
                "shared/core/database/src/commonMain/sqldelight/com/ruizurraca/logrobus/core/database/Stop.sq",
                "iosApp/iosAppTests/__Snapshots__/LineBadgeTests/dark.png",
                "android/core/designsystem/src/test/screenshots/seedling.png",
            )
        assertEquals(emptyList<String>(), others.filter(ArchitectureRules::isSeedData))
    }

    @Test
    fun lb017_freeTextAndCoordinatesAreFlaggedInTelemetry() {
        // ADR-0009, NFR-OBS-03, NFR-PRIV-02: no free text and no coordinates, whatever their type.
        val properties =
            mapOf(
                "Search.query" to "String",
                "Search.typed" to "String?",
                "Search.chars" to "CharSequence",
                "Near.latitude" to "Double",
                "Near.longitude" to "Float?",
                "Near.point" to "GeoPoint",
                "Near.position" to "LatLng",
                "Near.stops" to "List<Int>",
                "Other.reason" to "Reason",
            )

        assertEquals(
            properties.map { (name, type) -> "$name: $type" },
            ArchitectureRules.forbiddenTelemetryProperties(properties, telemetryEnums = setOf("Rejection")),
        )
    }

    @Test
    fun lb017_vendorTelemetryPackagesAreFoundInASwiftManifest() {
        val manifest =
            """
            dependencies: [
                .package(url: "https://github.com/pointfreeco/swift-snapshot-testing", exact: "1.19.7"),
                .package(url: "https://github.com/firebase/firebase-ios-sdk.git", exact: "12.4.0"),
                .package(url: "https://github.com/getsentry/sentry-cocoa", from: "8.0.0"),
                .package(url: "https://github.com/PostHog/posthog-ios.git", exact: "3.0.0"),
            ]
            """.trimIndent()

        assertEquals(
            listOf(
                "https://github.com/firebase/firebase-ios-sdk.git",
                "https://github.com/getsentry/sentry-cocoa",
                "https://github.com/PostHog/posthog-ios.git",
            ),
            ArchitectureRules.vendorTelemetryPackages(manifest),
        )
    }
}
