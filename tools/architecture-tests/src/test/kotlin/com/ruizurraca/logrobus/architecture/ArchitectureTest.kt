package com.ruizurraca.logrobus.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.declaration.KoPropertyDeclaration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Module layout and package boundaries of ADR-0007 (docs/architecture.md §3), applied to the repository.
 * The rules themselves are unit-tested in ArchitectureRulesTest; Gradle-level dependency rules live in
 * build-logic (ModuleRules).
 */
class ArchitectureTest {
    private val root: File = repositoryRoot

    private val sharedModules =
        listOf(
            ":shared:core:common",
            ":shared:core:config",
            ":shared:core:telemetry",
            ":shared:core:network",
            ":shared:core:database",
            ":shared:core:location",
            ":shared:core:polling",
            ":shared:core:presentation",
            ":shared:core:testing",
            ":shared:transit:domain",
            ":shared:transit:data",
            ":shared:favourites:domain",
            ":shared:favourites:data",
            ":shared:feature:lines",
            ":shared:feature:stops",
            ":shared:feature:nearme",
            ":shared:feature:planner",
            ":shared:feature:settings",
            ":shared:umbrella",
        )

    private val androidModules =
        listOf(
            ":android:app",
            ":android:core:designsystem",
            ":android:core:map",
            ":android:core:navigation",
            ":android:feature:lines",
            ":android:feature:stops",
            ":android:feature:nearme",
            ":android:feature:planner",
            ":android:feature:settings",
        )

    private val toolModules = listOf(":tools:architecture-tests")

    // architecture.md §3.1: the targets of the local Swift package iosApp/Packages/LogroBusUI.
    private val iosPackageTargets =
        listOf(
            "DesignSystem",
            "MapUI",
            "FeatureLines",
            "FeatureStops",
            "FeatureNearMe",
            "FeaturePlanner",
            "FeatureSettings",
        )

    private val ignoredDirectories = setOf("build", ".gradle", ".git", ".kotlin", ".idea")

    @Test
    fun lb001_settingsIncludeExactlyTheArchitectureModules() {
        val included = ArchitectureRules.includedModules(File(root, "settings.gradle.kts").readText())
        assertEquals((sharedModules + androidModules + toolModules).toSortedSet(), included.toSortedSet())
    }

    @Test
    fun lb001_everySharedModuleAppliesTheKmpConvention() {
        val missing =
            sharedModules.filterNot { path ->
                val buildFile = File(root, path.removePrefix(":").replace(':', '/') + "/build.gradle.kts")
                buildFile.isFile && "id(\"logrobus.kmp.library\")" in buildFile.readText()
            }
        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun lb001_everyAndroidModuleAppliesItsConvention() {
        val missing =
            androidModules.filterNot { path ->
                val plugin = if (path == ":android:app") "logrobus.android.application" else "logrobus.android.library"
                val buildFile = File(root, path.removePrefix(":").replace(':', '/') + "/build.gradle.kts")
                buildFile.isFile && "id(\"$plugin\")" in buildFile.readText()
            }
        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun lb001_iosProjectIsIphoneOnlyWithIos18AndTheStoreBundleId() {
        val spec = File(root, "iosApp/project.yml").takeIf { it.isFile }?.readText().orEmpty()
        // Settings of the app target itself: XcodeGen's application preset overrides project-level values.
        val appTarget =
            Regex("""(?ms)^ {2}iosApp:\n(.*?)(?=^ {2}\S|\z)""")
                .find(spec.substringAfter("\ntargets:\n"))
                ?.groupValues
                ?.get(1)
                .orEmpty()
        val required =
            mapOf(
                // OQ-14: the store identifier, shared with Android.
                "PRODUCT_BUNDLE_IDENTIFIER: com.ruizurraca.logrobus" to appTarget,
                // NFR-PLAT-03: iPhone only in the MVP.
                "TARGETED_DEVICE_FAMILY: \"1\"" to appTarget,
                // ADR-0003: iOS 18 deployment target.
                "iOS: \"18.0\"" to spec,
            )
        val missing =
            required.filterNot { (setting, scope) ->
                Regex("""(?m)^\s+${Regex.escape(setting)}\s*$""").containsMatchIn(scope)
            }
        assertEquals(emptyList<String>(), missing.keys.toList())
    }

    @Test
    fun lb001_logroBusUiPackageHasTheArchitectureTargets() {
        val manifest = File(root, "iosApp/Packages/LogroBusUI/Package.swift").takeIf { it.isFile }?.readText().orEmpty()
        val targets = iosPackageTargets.map { ".target(name: \"$it\"" } + ".binaryTarget(name: \"Shared\""
        assertEquals(emptyList<String>(), targets.filterNot { it in manifest })
    }

    @Test
    fun lb001_iosFeatureTargetsDoNotDependOnOtherFeatures() {
        val manifest = File(root, "iosApp/Packages/LogroBusUI/Package.swift").readText()
        assertEquals(emptyList<String>(), ArchitectureRules.crossFeatureSwiftDependencies(manifest))
    }

    @Test
    fun lb001_generatedXcodeProjectIsNotCommitted() {
        val ignored = File(root, ".gitignore").readLines().map { it.trim() }
        assertEquals(true, "iosApp/iosApp.xcodeproj/" in ignored)
    }

    @Test
    fun lb002_kotlinWorkflowRunsUnitTestsForEverySharedModule() {
        val workflow = File(root, ".github/workflows/kotlin-checks.yml")
        val modules = if (workflow.isFile) ArchitectureRules.workflowMatrixModules(workflow.readText()) else emptySet()
        // Other matrices in the workflow, such as the Android feature tests, list their own modules.
        assertEquals(sharedModules.toSortedSet(), modules.filter { it.startsWith(":shared:") }.toSortedSet())
    }

    @Test
    fun lb002_kotlinWorkflowRunsUnitTestsForEveryAndroidModule() {
        val workflow = File(root, ".github/workflows/kotlin-checks.yml")
        val modules = if (workflow.isFile) ArchitectureRules.workflowMatrixModules(workflow.readText()) else emptySet()
        assertEquals(androidModules.toSortedSet(), modules.filter { it.startsWith(":android:") }.toSortedSet())
    }

    @Test
    fun lb002_kotlinGateWaitsForEveryKotlinJob() {
        val workflow = File(root, ".github/workflows/kotlin-checks.yml").readText()
        assertEquals(emptyList<String>(), ArchitectureRules.jobsMissingFromGate(workflow, gate = "ci-gate"))
    }

    @Test
    fun lb002_dependencyReviewWaitsForTheGradleDependencyGraph() {
        // GitHub sees Gradle dependencies only once the graph job submits them. A review running alongside that job
        // would compare without them and pass (docs/ci_cd.md §3).
        val workflow = File(root, ".github/workflows/kotlin-checks.yml").readText()
        assertTrue("dependency-graph" in ArchitectureRules.jobNeeds(workflow, job = "dependency-review"))
    }

    @Test
    fun lb002_jobNamesReadWellWhenSkipped() {
        // A skipped job shows its name unexpanded; GitHub appends the matrix values to a plain name by itself.
        val offenders =
            File(root, ".github/workflows")
                .listFiles { file -> file.extension == "yml" }
                .orEmpty()
                .flatMap { file ->
                    ArchitectureRules.jobNamesWithExpressions(file.readText()).map { "${file.name}: $it" }
                }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun lb002_iosGateWaitsForEveryIosJob() {
        val workflow = File(root, ".github/workflows/ios-checks.yml")
        assertTrue(workflow.isFile, "ios-checks.yml exists")
        assertEquals(emptyList<String>(), ArchitectureRules.jobsMissingFromGate(workflow.readText(), gate = "ci-gate"))
    }

    @Test
    fun lb001_buildScriptsAndBuildLogicDeclareNoVersionLiterals() {
        val offenders =
            root
                .walkTopDown()
                .onEnter { it.name !in ignoredDirectories }
                .filter { it.isFile && (it.name.endsWith(".gradle.kts") || it.isBuildLogicSource()) }
                .flatMap { file ->
                    ArchitectureRules.versionLiterals(file.readText()).map { "${file.relativePath()}: $it" }
                }.toList()
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun lb001_sharedCodeDoesNotImportAnotherFeature() {
        val offenders =
            sharedFiles().flatMap { file ->
                ArchitectureRules
                    .crossFeatureImports(file.packagee?.name, file.importNames())
                    .map { "${file.relativePath()}: $it" }
            }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun lb001_domainCodeDoesNotImportFrameworksPlatformApisOrData() {
        val offenders =
            sharedFiles()
                .filter { file -> file.packagee?.name?.let { ".domain" in it } == true }
                .flatMap { file ->
                    ArchitectureRules.forbiddenDomainImports(file.importNames()).map { "${file.relativePath()}: $it" }
                }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun lb001_presentationCodeDoesNotImportInfrastructureOrData() {
        val offenders =
            sharedFiles()
                .filter { file -> file.packagee?.name?.let { ".presentation" in it } == true }
                .flatMap { file ->
                    ArchitectureRules
                        .forbiddenPresentationImports(
                            file.importNames(),
                        ).map { "${file.relativePath()}: $it" }
                }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun lb001_sharedKotlinFilesLiveInTheirModuleNamespace() {
        val offenders =
            sharedFiles().mapNotNull { file ->
                val expected = ArchitectureRules.expectedNamespace(file.relativePath())
                val actual = file.packagee?.name.orEmpty()
                val inNamespace = actual == expected || actual.startsWith("$expected.")
                if (inNamespace) null else "${file.relativePath()}: $actual"
            }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun lb001_androidKotlinFilesLiveInTheirModuleNamespace() {
        val offenders =
            androidFiles().mapNotNull { file ->
                val expected = ArchitectureRules.expectedNamespace(file.relativePath())
                val actual = file.packagee?.name.orEmpty()
                val inNamespace = actual == expected || actual.startsWith("$expected.")
                if (inNamespace) null else "${file.relativePath()}: $actual"
            }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun lb011_kotlinWorkflowVerifiesTheScreenshotsOfEveryModuleThatHasThem() {
        // A module's reference images are checked only if its module is in the screenshot job (docs/ci_cd.md §3).
        val withScreenshots =
            androidModules.filter { module ->
                File(root, module.removePrefix(":").replace(':', '/') + "/src/test/screenshots")
                    .listFiles { file -> file.extension == "png" }
                    .orEmpty()
                    .isNotEmpty()
            }
        val workflow = File(root, ".github/workflows/kotlin-checks.yml").readText()
        assertEquals(
            withScreenshots.toSortedSet(),
            ArchitectureRules.jobMatrixModules(workflow, job = "android-screenshots").toSortedSet(),
        )
    }

    @Test
    fun lb011_theIosDesignSystemDependsOnSharedOnly() {
        // Like :android:core:designsystem, it draws shared models and never reaches maps or features (ADR-0007).
        val manifest = File(root, "iosApp/Packages/LogroBusUI/Package.swift").readText()
        assertEquals(listOf("Shared"), ArchitectureRules.swiftTargetDependencies(manifest, "DesignSystem"))
    }

    @Test
    fun lb011_swiftPackagesPinnedInBothThePackageAndTheProjectAgree() {
        val manifest = File(root, "iosApp/Packages/LogroBusUI/Package.swift").readText()
        val projectSpec = File(root, "iosApp/project.yml").readText()
        assertEquals(emptyList<String>(), ArchitectureRules.swiftPackageVersionMismatches(manifest, projectSpec))
    }

    @Test
    fun lb008_noModuleBundlesSeedData() {
        // ADR-0008: the first use downloads lines and stops; the MVP bundles no seed in either app or shared code.
        val skipped = ignoredDirectories + setOf(".build", "DerivedData", "Frameworks", "iosApp.xcodeproj")
        val offenders =
            listOf("android", "iosApp", "shared")
                .flatMap { area ->
                    File(root, area)
                        .walkTopDown()
                        .onEnter { it.name !in skipped }
                        .filter { it.isFile }
                        .toList()
                }.map { it.relativePath() }
                .filter(ArchitectureRules::isSeedData)
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun lb017_theSwiftPackageHasNoVendorTelemetrySdk() {
        // ADR-0009: as on Android, only the app may link a vendor SDK; the design system and features never do.
        val manifest = File(root, "iosApp/Packages/LogroBusUI/Package.swift").readText()
        assertEquals(emptyList<String>(), ArchitectureRules.vendorTelemetryPackages(manifest))
    }

    @Test
    fun lb011_expressiveApisStayInTheDesignSystem() {
        // ADR-0005: an expressive API change then touches one package.
        val offenders =
            androidFiles()
                .filter { ArchitectureRules.usesExpressiveApiOutsideDesignSystem(it.packagee?.name, it.text) }
                .map { it.relativePath() }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun lb017_analyticsEventsAndBreadcrumbsCarryNoFreeTextOrCoordinates() {
        // ADR-0009: only ids, counts, flags and enums, so no event or breadcrumb can carry what a user typed or where
        // they are (NFR-OBS-03, NFR-PRIV-02). Every type under the two roots counts, intermediate ones and the roots
        // included, and each property is keyed by its type's full name, so no two can hide each other.
        val properties =
            telemetryPayloads()
                .flatMap { payload -> payload.properties.map { (name, type) -> "${payload.name}.$name" to type } }
                .toMap()
        val telemetryEnums =
            Konsist
                .scopeFromProject()
                .classes()
                .filter { it.hasEnumModifier && it.resideInPackage("$TELEMETRY_PACKAGE..") }
                .map { it.name }
                .toSet()

        assertEquals(emptyList<String>(), ArchitectureRules.forbiddenTelemetryProperties(properties, telemetryEnums))
    }

    @Test
    fun lb017_theAnalyticsCatalogueHasTheMvpEventsOfNfrObs03() {
        // app_open, stop_view, line_view, plan_request, plan_result and favourite_add; widget_add and notice_open
        // arrive with their features in v1.0. The concrete events count, at any depth under AnalyticsEvent.
        val events = telemetryPayloads().filter { it.isConcrete && "AnalyticsEvent" in it.ancestors }

        assertEquals(
            sortedSetOf("AppOpen", "FavouriteAdd", "LineView", "PlanRequest", "PlanResult", "StopView"),
            events.map { it.name.substringAfterLast('.') }.toSortedSet(),
        )
    }

    /**
     * An analytics event or crash breadcrumb, or a type between one and its root: its full name, all its ancestors,
     * whether it can be instantiated, and its properties' declared types.
     */
    private data class TelemetryPayload(
        val name: String,
        val ancestors: Set<String>,
        val isConcrete: Boolean,
        val properties: Map<String, String>,
    )

    /** A type of the telemetry package as declared: its direct parents by simple name. */
    private data class TelemetryType(
        val name: String,
        val parents: List<String>,
        val isConcrete: Boolean,
        val properties: Map<String, String>,
    )

    /**
     * Every class, object and interface under `AnalyticsEvent` or `Breadcrumb`, the two sealed telemetry roots, directly
     * or through intermediate types, and the roots themselves. Both roots are sealed, so every such type lives in the
     * telemetry package. Ancestors are followed here by simple name, because Konsist does not resolve the parents of
     * nested types; two types sharing a simple name share their parents, which can only widen the check.
     */
    private fun telemetryPayloads(): List<TelemetryPayload> {
        val scope = Konsist.scopeFromProject()
        val classes =
            scope.classes().map { declaration ->
                // Constructor properties are listed with the declared ones, or as `val` parameters, by Konsist version.
                val inConstructor =
                    declaration.primaryConstructor
                        ?.parameters
                        .orEmpty()
                        .filter { it.isVal || it.isVar }
                        .associate { it.name to it.type.text }
                TelemetryType(
                    declaration.fullyQualifiedName ?: declaration.name,
                    declaration.parents().map { it.name.substringAfterLast('.') },
                    isConcrete = !declaration.hasAbstractModifier && !declaration.hasSealedModifier,
                    inConstructor + declaration.properties(includeNested = false).associate { it.name to it.typeText },
                )
            }
        val objects =
            scope.objects().map { declaration ->
                TelemetryType(
                    declaration.fullyQualifiedName ?: declaration.name,
                    declaration.parents().map { it.name.substringAfterLast('.') },
                    isConcrete = true,
                    declaration.properties(includeNested = false).associate { it.name to it.typeText },
                )
            }
        val interfaces =
            scope.interfaces().map { declaration ->
                TelemetryType(
                    declaration.fullyQualifiedName ?: declaration.name,
                    declaration.parents().map { it.name.substringAfterLast('.') },
                    isConcrete = false,
                    declaration.properties(includeNested = false).associate { it.name to it.typeText },
                )
            }
        val types = (classes + objects + interfaces).filter { it.name.startsWith("$TELEMETRY_PACKAGE.") }
        val parentsBySimpleName =
            types
                .groupBy { it.name.substringAfterLast('.') }
                .mapValues { (_, sameName) -> sameName.flatMap { it.parents }.toSet() }

        fun ancestorsOf(type: TelemetryType): Set<String> {
            val found = mutableSetOf(type.name.substringAfterLast('.'))
            var frontier = type.parents.toSet()
            while (frontier.isNotEmpty()) {
                val next = frontier - found
                found += next
                frontier = next.flatMap { parentsBySimpleName[it].orEmpty() }.toSet()
            }
            return found
        }
        return types
            .map { TelemetryPayload(it.name, ancestorsOf(it), it.isConcrete, it.properties) }
            .filter { payload -> payload.ancestors.any { it in TELEMETRY_PAYLOADS } }
    }

    private val KoPropertyDeclaration.typeText: String
        get() = type?.text.orEmpty()

    private fun sharedFiles(): List<KoFileDeclaration> =
        Konsist.scopeFromProject().files.filter { it.relativePath().startsWith("shared/") }

    private fun androidFiles(): List<KoFileDeclaration> =
        Konsist.scopeFromProject().files.filter { it.relativePath().startsWith("android/") }

    private fun KoFileDeclaration.importNames(): List<String> = imports.map { it.name }

    private fun KoFileDeclaration.relativePath(): String = File(path).relativePath()

    private fun File.relativePath(): String = relativeTo(root).invariantSeparatorsPath

    private fun File.isBuildLogicSource(): Boolean =
        name.endsWith(".kt") && relativePath().let { it.startsWith("build-logic/") && "/src/main/" in it }
}

private const val TELEMETRY_PACKAGE = "com.ruizurraca.logrobus.core.telemetry"
private val TELEMETRY_PAYLOADS = setOf("AnalyticsEvent", "Breadcrumb")
