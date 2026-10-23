package com.ruizurraca.logrobus.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.jupiter.api.Assertions.assertEquals
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

    private val toolModules = listOf(":tools:architecture-tests")

    private val ignoredDirectories = setOf("build", ".gradle", ".git", ".kotlin", ".idea")

    @Test
    fun lb001_settingsIncludeExactlyTheArchitectureModules() {
        val included = ArchitectureRules.includedModules(File(root, "settings.gradle.kts").readText())
        assertEquals((sharedModules + toolModules).toSortedSet(), included.toSortedSet())
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
    fun lb002_kotlinWorkflowRunsUnitTestsForEverySharedModule() {
        val workflow = File(root, ".github/workflows/kotlin-checks.yml")
        val modules = if (workflow.isFile) ArchitectureRules.workflowMatrixModules(workflow.readText()) else emptySet()
        // Other matrices in the workflow, such as the Android feature tests, list their own modules.
        assertEquals(sharedModules.toSortedSet(), modules.filter { it.startsWith(":shared:") }.toSortedSet())
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

    private fun sharedFiles(): List<KoFileDeclaration> =
        Konsist.scopeFromProject().files.filter { it.relativePath().startsWith("shared/") }

    private fun KoFileDeclaration.importNames(): List<String> = imports.map { it.name }

    private fun KoFileDeclaration.relativePath(): String = File(path).relativePath()

    private fun File.relativePath(): String = relativeTo(root).invariantSeparatorsPath

    private fun File.isBuildLogicSource(): Boolean =
        name.endsWith(".kt") && relativePath().let { it.startsWith("build-logic/") && "/src/main/" in it }
}
