package com.ruizurraca.logrobus.architecture

import com.ruizurraca.logrobus.architecture.UiTextRules.catalogueKeys
import com.ruizurraca.logrobus.architecture.UiTextRules.hardcodedKotlinUiText
import com.ruizurraca.logrobus.architecture.UiTextRules.hardcodedSwiftUiText
import com.ruizurraca.logrobus.architecture.UiTextRules.isCatalogueKey
import com.ruizurraca.logrobus.architecture.UiTextRules.swiftCatalogueKeys
import com.ruizurraca.logrobus.architecture.UiTextRules.unbundledSwiftKeys
import com.ruizurraca.logrobus.architecture.UiTextRules.untranslatedCatalogueEntries
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

/**
 * LB-405 (NFR-I18N-01): every user-facing string comes from the platforms' string resources. The scanners are
 * unit-tested in UiTextRulesTest.
 */
class LocalisationTest {
    private val root: File = repositoryRoot

    @Test
    fun lb405_androidUiCodeHasNoHardcodedUserFacingText() {
        val findings =
            sources("android", "kt").flatMap { file ->
                hardcodedKotlinUiText(file.readText()).map { "${file.relativePath()}:${it.line}: \"${it.text}\"" }
            }

        assertEquals(emptyList<String>(), findings, "Take user-facing text from the string resources")
    }

    @Test
    fun lb405_iosUiCodeHasNoHardcodedUserFacingText() {
        val findings =
            iosTargets().flatMap(::swiftSources).flatMap { file ->
                hardcodedSwiftUiText(file.readText()).map { "${file.relativePath()}:${it.line}: \"${it.text}\"" }
            }

        assertEquals(emptyList<String>(), findings, "Take user-facing text from the string catalogues, by key")
    }

    @Test
    fun lb405_everyIosCatalogueEntryIsInSpanishAndEnglish() {
        val untranslated =
            iosTargets().mapNotNull(::catalogueOf).flatMap { catalogue ->
                untranslatedCatalogueEntries(catalogue.readText()).map { "${catalogue.relativePath()}: $it" }
            }

        assertEquals(emptyList<String>(), untranslated, "Translate every entry into Spanish and English")
    }

    @Test
    fun lb405_everyKeyTheIosCodeUsesIsInItsTargetsCatalogue() {
        val missing =
            iosTargets().flatMap { target ->
                val used = swiftSources(target).flatMap { swiftCatalogueKeys(it.readText()) }
                val catalogued = catalogueOf(target)?.let(::keysOf).orEmpty()
                (used.toSet() - catalogued).sorted().map { "${target.relativePath()}: $it" }
            }

        assertEquals(emptyList<String>(), missing, "Add the key to the target's Localizable.xcstrings")
    }

    @Test
    fun lb405_packageTargetsLookTheirKeysUpInTheirOwnBundles() {
        val unbundled =
            packageTargets().flatMap(::swiftSources).flatMap { file ->
                unbundledSwiftKeys(file.readText()).map { "${file.relativePath()}:${it.line}: \"${it.text}\"" }
            }

        assertEquals(emptyList<String>(), unbundled, "Name the target's bundle: Text(\"key\", bundle: .module)")
    }

    @Test
    fun lb405_everyIosCatalogueKeyIsInUse() {
        // Xcode never marks a key added by hand as stale, so an unused one would stay, and need translating, for ever.
        val unused =
            iosTargets().flatMap { target ->
                val used = swiftSources(target).flatMap { swiftCatalogueKeys(it.readText()) }.toSet()
                val catalogued = catalogueOf(target)?.let(::keysOf).orEmpty().filter(::isCatalogueKey)
                (catalogued - used).sorted().map { "${target.relativePath()}: $it" }
            }

        assertEquals(emptyList<String>(), unused, "Remove the key from the catalogue, or use it")
    }

    /**
     * Every target with Swift code of its own, each with its own catalogue: the app and any extension beside it, and
     * each target of the Swift packages. Test targets are left out.
     */
    private fun iosTargets(): List<File> =
        root
            .resolve("iosApp")
            .listFiles(File::isDirectory)
            .orEmpty()
            .filter { it.name !in IOS_NON_TARGETS && !it.name.endsWith("Tests") && swiftSources(it).isNotEmpty() }
            .sortedBy { it.name } + packageTargets()

    private fun packageTargets(): List<File> =
        root
            .resolve("iosApp/Packages")
            .listFiles(File::isDirectory)
            .orEmpty()
            .flatMap {
                it
                    .resolve("Sources")
                    .listFiles(File::isDirectory)
                    .orEmpty()
                    .toList()
            }.sortedBy { it.path }

    private fun catalogueOf(target: File): File? =
        listOf(
            "Localizable.xcstrings",
            "Resources/Localizable.xcstrings",
        ).map(target::resolve).firstOrNull(File::isFile)

    private fun keysOf(catalogue: File): Set<String> = catalogueKeys(catalogue.readText())

    private fun swiftSources(directory: File): List<File> =
        directory
            .walkTopDown()
            .onEnter { it.name != "build" && !it.name.startsWith(".") }
            .filter { it.isFile && it.extension == "swift" }
            .sortedBy { it.path }
            .toList()

    /** The main source files with [extension] under [directory], without build outputs. */
    private fun sources(
        directory: String,
        extension: String,
    ): List<File> =
        root
            .resolve(directory)
            .walkTopDown()
            .onEnter { it.name != "build" && !it.name.startsWith(".") }
            .filter { it.isFile && it.extension == extension && "/src/main/" in it.invariantSeparatorsPath }
            .sortedBy { it.path }
            .toList()

    private fun File.relativePath(): String = relativeTo(root).invariantSeparatorsPath

    private companion object {
        // iosApp's folders that are not targets: the packages, the generated project and build outputs.
        val IOS_NON_TARGETS = setOf("Packages", "Configuration", "iosApp.xcodeproj", "build", "DerivedData")
    }
}
