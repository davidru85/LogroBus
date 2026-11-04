package com.ruizurraca.logrobus.architecture

import com.ruizurraca.logrobus.architecture.UiTextRules.hardcodedKotlinUiText
import com.ruizurraca.logrobus.architecture.UiTextRules.hardcodedSwiftUiText
import com.ruizurraca.logrobus.architecture.UiTextRules.swiftCatalogueKeys
import com.ruizurraca.logrobus.architecture.UiTextRules.untranslatedCatalogueEntries
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
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
            (swiftSources("iosApp/iosApp") + swiftSources("iosApp/Packages/LogroBusUI/Sources")).flatMap { file ->
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
                val used = swiftSources(target.relativePath()).flatMap { swiftCatalogueKeys(it.readText()) }
                val catalogued = catalogueOf(target)?.let(::keysOf).orEmpty()
                (used.toSet() - catalogued).sorted().map { "${target.relativePath()}: $it" }
            }

        assertEquals(emptyList<String>(), missing, "Add the key to the target's Localizable.xcstrings")
    }

    /** The app target and each target of the Swift package, whose strings live in a catalogue of their own. */
    private fun iosTargets(): List<File> =
        listOf(root.resolve("iosApp/iosApp")) +
            root
                .resolve("iosApp/Packages/LogroBusUI/Sources")
                .listFiles(File::isDirectory)
                .orEmpty()
                .sortedBy { it.name }

    private fun catalogueOf(target: File): File? =
        listOf(
            "Localizable.xcstrings",
            "Resources/Localizable.xcstrings",
        ).map(target::resolve).firstOrNull(File::isFile)

    /** A catalogue's keys without their format arguments: `lines.towards %@` is `lines.towards`. */
    private fun keysOf(catalogue: File): Set<String> =
        Json
            .parseToJsonElement(catalogue.readText())
            .jsonObject["strings"]
            ?.jsonObject
            ?.keys
            .orEmpty()
            .map { it.substringBefore(' ') }
            .toSet()

    private fun swiftSources(directory: String): List<File> =
        root
            .resolve(directory)
            .walkTopDown()
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
}
