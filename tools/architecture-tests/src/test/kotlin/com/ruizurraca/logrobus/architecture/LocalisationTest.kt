package com.ruizurraca.logrobus.architecture

import com.ruizurraca.logrobus.architecture.UiTextRules.hardcodedKotlinUiText
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
