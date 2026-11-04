package com.ruizurraca.logrobus.architecture

/** A user-facing [text] written into UI code at its 1-based [line], rather than taken from the string resources. */
data class UiTextFinding(
    val line: Int,
    val text: String,
)

/** LB-405 (NFR-I18N-01): the hard-coded user-facing text in a Kotlin [source]. */
@Suppress("UNUSED_PARAMETER") // TDD: red.
fun hardcodedKotlinUiText(source: String): List<UiTextFinding> = emptyList()
