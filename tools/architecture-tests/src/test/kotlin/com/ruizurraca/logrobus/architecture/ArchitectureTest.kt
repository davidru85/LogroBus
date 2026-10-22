package com.ruizurraca.logrobus.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Module layout and package boundaries of ADR-0007 (docs/architecture.md §3).
 * Gradle-level dependency rules live in build-logic (ModuleRules).
 */
class ArchitectureTest {

    private val root: File = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").isFile && File(it, "gradlew").isFile }

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

    /** Import prefixes that domain code must not use: networking, persistence and platform APIs. */
    private val forbiddenInDomain = listOf(
        "io.ktor.",
        "app.cash.sqldelight.",
        "android.",
        "androidx.",
        "platform.",
        "kotlinx.cinterop.",
    )

    @Test
    fun lb001_settingsIncludeExactlyTheArchitectureModules() {
        val included = Regex("""include\("([^"]+)"\)""")
            .findAll(File(root, "settings.gradle.kts").readText())
            .map { it.groupValues[1] }
            .toSortedSet()
        assertEquals((sharedModules + toolModules).toSortedSet(), included)
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
    fun lb001_buildScriptsDeclareNoVersionLiterals() {
        val coordinateWithVersion = Regex(""""[\w.-]+:[\w.-]+:\d[^"]*"""")
        val versionCall = Regex("""\bversion\s*\(?\s*"\d""")
        val ignored = setOf("build", ".gradle", ".git", ".kotlin", ".idea")
        val offenders = root.walkTopDown()
            .onEnter { it.name !in ignored }
            .filter { it.isFile && it.name.endsWith(".gradle.kts") }
            .filter { file ->
                val text = file.readText()
                coordinateWithVersion.containsMatchIn(text) || versionCall.containsMatchIn(text)
            }
            .map { it.relativeTo(root).path }
            .toList()
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun lb001_sharedCodeDoesNotImportAnotherFeature() {
        val offenders = sharedFiles().flatMap { file ->
            val ownFeature = featureOf(file.packagee?.name)
            file.imports
                .map { it.name }
                .filter { imported -> featureOf(imported)?.let { it != ownFeature } == true }
                .map { "${file.relativePath()}: $it" }
        }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun lb001_domainCodeDoesNotImportFrameworksPlatformApisOrData() {
        val dataPackage = Regex("""^com\.ruizurraca\.logrobus\..*\.data(\.|$)""")
        val offenders = sharedFiles()
            .filter { file -> file.packagee?.name?.let { ".domain" in it } == true }
            .flatMap { file ->
                file.imports
                    .map { it.name }
                    .filter { imported ->
                        forbiddenInDomain.any(imported::startsWith) || dataPackage.containsMatchIn(imported)
                    }
                    .map { "${file.relativePath()}: $it" }
            }
        assertEquals(emptyList<String>(), offenders)
    }

    @Test
    fun lb001_sharedKotlinFilesLiveInTheirModuleNamespace() {
        val offenders = sharedFiles().mapNotNull { file ->
            val segments = file.relativePath().split('/')
            val moduleSegments = segments.drop(1).takeWhile { it != "src" }
            val expectedPrefix = (listOf("com", "ruizurraca", "logrobus") + moduleSegments).joinToString(".")
            val actual = file.packagee?.name.orEmpty()
            val inNamespace = actual == expectedPrefix || actual.startsWith("$expectedPrefix.")
            if (inNamespace) null else "${file.relativePath()}: $actual"
        }
        assertEquals(emptyList<String>(), offenders)
    }

    private fun sharedFiles(): List<KoFileDeclaration> =
        Konsist.scopeFromProject().files.filter { it.relativePath().startsWith("shared/") }

    private fun KoFileDeclaration.relativePath(): String = File(path).relativeTo(root).path

    private fun featureOf(name: String?): String? =
        name?.let { Regex("""^com\.ruizurraca\.logrobus\.feature\.(\w+)""").find(it)?.groupValues?.get(1) }
}
