package com.ruizurraca.logrobus.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
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

    private val sharedModules = listOf(
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

    private val androidModules = listOf(
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
    private val iosPackageTargets = listOf(
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
        val missing = sharedModules.filterNot { path ->
            val buildFile = File(root, path.removePrefix(":").replace(':', '/') + "/build.gradle.kts")
            buildFile.isFile && "id(\"logrobus.kmp.library\")" in buildFile.readText()
        }
        assertEquals(emptyList<String>(), missing)
    }

    @Test
    fun lb001_everyAndroidModuleAppliesItsConvention() {
        val missing = androidModules.filterNot { path ->
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
        val appTarget = Regex("""(?ms)^ {2}iosApp:\n(.*?)(?=^ {2}\S|\z)""")
            .find(spec.substringAfter("\ntargets:\n"))?.groupValues?.get(1).orEmpty()
        val required = mapOf(
            // OQ-14: the store identifier, shared with Android.
            "PRODUCT_BUNDLE_IDENTIFIER: com.ruizurraca.logrobus" to appTarget,
            // NFR-PLAT-03: iPhone only in the MVP.
            "TARGETED_DEVICE_FAMILY: \"1\"" to appTarget,
            // ADR-0003: iOS 18 deployment target.
            "iOS: \"18.0\"" to spec,
        )
        val missing = required.filterNot { (setting, scope) ->
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
    fun lb002_jobNamesReadWellWhenSkipped() {
        // A skipped job shows its name unexpanded; GitHub appends the matrix values to a plain name by itself.
        val offenders = File(root, ".github/workflows").listFiles { file -> file.extension == "yml" }.orEmpty()
            .flatMap { file -> ArchitectureRules.jobNamesWithExpressions(file.readText()).map { "${file.name}: $it" } }
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
        val offenders = root.walkTopDown()
            .onEnter { it.name !in ignoredDirectories }
            .filter { it.isFile && (it.name.endsWith(".gradle.kts") || it.isBuildLogicSource()) }
            .flatMap { file ->
                ArchitectureRules.versionLiterals(file.readText()).map { "${file.relativePath()}: $it" }
            }
            .toList()
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun lb001_sharedCodeDoesNotImportAnotherFeature() {
        val offenders = sharedFiles().flatMap { file ->
            ArchitectureRules.crossFeatureImports(file.packagee?.name, file.importNames())
                .map { "${file.relativePath()}: $it" }
        }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun lb001_domainCodeDoesNotImportFrameworksPlatformApisOrData() {
        val offenders = sharedFiles()
            .filter { file -> file.packagee?.name?.let { ".domain" in it } == true }
            .flatMap { file ->
                ArchitectureRules.forbiddenDomainImports(file.importNames()).map { "${file.relativePath()}: $it" }
            }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun lb001_presentationCodeDoesNotImportInfrastructureOrData() {
        val offenders = sharedFiles()
            .filter { file -> file.packagee?.name?.let { ".presentation" in it } == true }
            .flatMap { file ->
                ArchitectureRules.forbiddenPresentationImports(file.importNames()).map { "${file.relativePath()}: $it" }
            }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun lb001_sharedKotlinFilesLiveInTheirModuleNamespace() {
        val offenders = sharedFiles().mapNotNull { file ->
            val expected = ArchitectureRules.expectedNamespace(file.relativePath())
            val actual = file.packagee?.name.orEmpty()
            val inNamespace = actual == expected || actual.startsWith("$expected.")
            if (inNamespace) null else "${file.relativePath()}: $actual"
        }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun lb001_androidKotlinFilesLiveInTheirModuleNamespace() {
        val offenders = androidFiles().mapNotNull { file ->
            val expected = ArchitectureRules.expectedNamespace(file.relativePath())
            val actual = file.packagee?.name.orEmpty()
            val inNamespace = actual == expected || actual.startsWith("$expected.")
            if (inNamespace) null else "${file.relativePath()}: $actual"
        }
        assertEquals(emptyList<String>(), offenders)
    }

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
