package com.ruizurraca.logrobus.architecture

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A user-facing [text] written into UI code at its 1-based [line], rather than taken from the string resources. */
data class UiTextFinding(
    val line: Int,
    val text: String,
)

/** LB-405 (NFR-I18N-01): finds user-facing text written into UI code instead of the platforms' string resources. */
object UiTextRules {
    /**
     * The hard-coded user-facing text in a Kotlin [source]. A string literal with a letter is user-facing where
     * Compose shows or speaks it: anywhere at the top level of the text of `Text` and its kin, of a spoken label or
     * description, of a snackbar, toast or notification. Template expressions and format specifiers are not text,
     * and previews are exempt.
     */
    fun hardcodedKotlinUiText(source: String): List<UiTextFinding> {
        val lexed = KotlinLexer(source).lex()
        val previews = kotlinPreviewRanges(lexed.code)
        val sinks =
            textCallSinks(lexed, KOTLIN_TEXT_CALLS, KOTLIN_LABEL) + textPropertySinks(lexed, KOTLIN_TEXT_PROPERTY)
        return sinks
            .map { it.literal }
            .distinctBy { it.start }
            .sortedBy { it.start }
            .filter { literal -> previews.none { literal.start in it } }
            .filter { it.text.replace(FORMAT_SPECIFIER, "").any(Char::isLetter) }
            .map { UiTextFinding(source.lineAt(it.start), it.text) }
    }

    /**
     * The hard-coded user-facing text in a Swift [source]. A string literal with a letter is user-facing where SwiftUI
     * shows or speaks it: the title of `Text`, `Button` and their kin, a navigation title, an accessibility label, an
     * alert or a search prompt. There it must be a catalogue key such as `lines.title`, which interpolations may
     * follow, and never verbatim text. Previews are exempt.
     */
    fun hardcodedSwiftUiText(source: String): List<UiTextFinding> {
        val lexed = SwiftLexer(source).lex()
        val previews = swiftPreviewRanges(lexed.code)
        return lexed.literals
            .filter { literal -> previews.none { literal.start in it } }
            .filter { literal ->
                val sink = SWIFT_UI_TEXT.find(lexed.codeBefore(literal.start))
                sink != null &&
                    literal.text.any(Char::isLetter) &&
                    (sink.groups[VERBATIM] != null || !isCatalogueKey(literal.text))
            }.map { UiTextFinding(source.lineAt(it.start), it.text) }
    }

    /**
     * The catalogue keys a Swift [source] uses: those it passes where SwiftUI shows or speaks text, and those its
     * members typed LocalizedStringKey or LocalizedStringResource return. Interpolations are left out, so
     * `"lines.towards \(headsign)"` uses `lines.towards`.
     */
    fun swiftCatalogueKeys(source: String): Set<String> {
        val lexed = SwiftLexer(source).lex()
        val localizedMembers =
            SWIFT_LOCALIZED_MEMBER
                .findAll(lexed.code)
                .mapNotNull { member -> closing(lexed.code, member.range.last)?.let { member.range.last..it } }
                .toList()
        return lexed.literals
            .filter { literal ->
                val sink = SWIFT_UI_TEXT.find(lexed.codeBefore(literal.start))
                (sink != null && sink.groups[VERBATIM] == null) || localizedMembers.any { literal.start in it }
            }.map { it.text.trim() }
            .filter(CATALOGUE_KEY::matches)
            .toSet()
    }

    /**
     * The entries of a string catalogue's [json] without a translated value in Spanish or English (NFR-I18N-01), as
     * "key (language)": every string unit of the language, each plural form included, must be translated and not
     * empty. An entry marked not to translate needs only the source language.
     */
    fun untranslatedCatalogueEntries(json: String): List<String> {
        val catalogue = Json.parseToJsonElement(json).jsonObject
        val sourceLanguage = catalogue["sourceLanguage"]?.jsonPrimitive?.content
        return catalogue["strings"]
            ?.jsonObject
            .orEmpty()
            .flatMap { (key, entry) ->
                val localizations = entry.jsonObject["localizations"]?.jsonObject.orEmpty()
                val translatable = entry.jsonObject["shouldTranslate"]?.jsonPrimitive?.booleanOrNull != false
                APP_LANGUAGES
                    .filter { language -> translatable || language == sourceLanguage }
                    .filterNot { language -> localizations[language]?.let(::isTranslated) == true }
                    .map { language -> "$key ($language)" }
            }.sorted()
    }

    /** A key of a string catalogue, such as `lines.title`, once the interpolations that follow it are left out. */
    fun isCatalogueKey(text: String): Boolean = CATALOGUE_KEY.matches(text.trim())
}

// What comes right before a literal that SwiftUI shows or speaks: the title of a view or modifier that takes a
// LocalizedStringKey, verbatim text, a String(localized:) key, or a search prompt.
private val SWIFT_UI_TEXT =
    Regex(
        """(?:\b(?:Text|Button|Label|Toggle|Picker|Section|LabeledContent|Link|Menu|TextField|SecureField|Stepper|""" +
            """ProgressView|GroupBox|DisclosureGroup|NavigationLink|ContentUnavailableView|Tab|LocalizedStringKey|""" +
            """LocalizedStringResource|navigationTitle|accessibilityLabel|accessibilityHint|accessibilityValue|""" +
            """accessibilityAction|alert|confirmationDialog|help|badge)\(\s*(verbatim\s*:\s*)?(?:named\s*:\s*)?""" +
            """|\bString\(\s*localized\s*:\s*""" +
            """|\bprompt\s*:\s*)$""",
    )

private const val VERBATIM = 1

// A member that returns catalogue keys: `var title: LocalizedStringKey {` or `-> LocalizedStringResource {`.
private val SWIFT_LOCALIZED_MEMBER = Regex("""(?::|->)\s*(?:LocalizedStringKey|LocalizedStringResource)\s*\{""")

// NFR-I18N-01: Spanish, the source language, and English.
private val APP_LANGUAGES = listOf("es", "en")

private fun isTranslated(localization: JsonElement): Boolean {
    val units = stringUnits(localization)
    return units.isNotEmpty() &&
        units.all { unit ->
            unit["state"]?.jsonPrimitive?.content == "translated" &&
                !unit["value"]?.jsonPrimitive?.content.isNullOrEmpty()
        }
}

/** Every string unit in a localisation: its own, its plural or device variations', and its substitutions'. */
private fun stringUnits(element: JsonElement): List<JsonObject> =
    when (element) {
        is JsonObject -> {
            element.flatMap { (name, value) ->
                if (name ==
                    "stringUnit"
                ) {
                    listOf(value.jsonObject)
                } else {
                    stringUnits(value)
                }
            }
        }

        is JsonArray -> {
            element.flatMap(::stringUnits)
        }

        else -> {
            emptyList()
        }
    }

private val CATALOGUE_KEY = Regex("""[a-z0-9_]+(?:\.[a-z0-9_]+)+""")

private val SWIFT_PREVIEW_MACRO = Regex("""#Preview\b""")

private val SWIFT_TYPE = Regex("""\b(?:struct|class|enum|extension)\s+(\w+)([^{]*)\{""")

/** The previews: `#Preview` blocks, and types named after a preview or that are a PreviewProvider. */
private fun swiftPreviewRanges(code: String): List<IntRange> {
    val macros =
        SWIFT_PREVIEW_MACRO.findAll(code).mapNotNull { macro ->
            var body = macro.range.last + 1
            while (body < code.length && code[body].isWhitespace()) body++
            if (code.getOrNull(body) == '(') body = (closing(code, body) ?: return@mapNotNull null) + 1
            body = code.indexOf('{', body).takeIf { it >= 0 } ?: return@mapNotNull null
            closing(code, body)?.let { macro.range.first..it }
        }
    val types =
        SWIFT_TYPE
            .findAll(code)
            .filter { type ->
                val (name, inheritance) = type.destructured
                name.endsWith("Preview") || name.endsWith("Previews") || "PreviewProvider" in inheritance
            }.mapNotNull { type -> closing(code, type.range.last)?.let { type.range.first..it } }
    return (macros + types).toList()
}

// Compose's calls that show or speak their text, and where the text goes (LB-405).
private val KOTLIN_TEXT_CALLS =
    listOf(
        TextCall(listOf("Text", "BasicText", "AnnotatedString"), positions = setOf(0), labels = setOf("text")),
        TextCall(listOf("Icon", "Image"), positions = setOf(1), labels = setOf("contentDescription")),
        TextCall(listOf("CustomAccessibilityAction"), positions = setOf(0), labels = setOf("label")),
        TextCall(listOf("onClick", "onLongClick"), labels = setOf("label")),
        TextCall(listOf("showSnackbar"), positions = setOf(0, 1), labels = setOf("message", "actionLabel")),
        TextCall(listOf("makeText"), positions = setOf(1), labels = setOf("text")),
        TextCall(listOf("append", "setContentTitle", "setContentText", "setTicker"), positions = setOf(0)),
    )

private val KOTLIN_LABEL = Regex("""^\s*(\w+)\s*=(?!=)""")

// Semantics properties and parameters that hold spoken text, wherever they are set: `contentDescription = …`.
private val KOTLIN_TEXT_PROPERTY =
    Regex("""\b(?:contentDescription|stateDescription|onClickLabel|onLongClickLabel|paneTitle)\s*=(?!=)""")

private val FORMAT_SPECIFIER = Regex("""%(\d+\$)?[-#+ 0,(]*\d*(\.\d+)?[a-zA-Z%]""")

private val KOTLIN_PREVIEW_ANNOTATION = Regex("""@\w*Preview\w*""")

// After a preview annotation: its arguments, more annotations and modifiers, then a function.
private val KOTLIN_ANNOTATED_FUNCTION =
    Regex(
        """\G(?:\s*\([^()]*(?:\([^()]*\)[^()]*)*\))?(?:\s*@[\w.]+(?:\s*\([^()]*(?:\([^()]*\)[^()]*)*\))?)*\s*""" +
            """(?:(?:private|internal|public|protected|inline|override|open|actual|expect)\s+)*fun\b""",
    )

// A private function named after a preview; a function the app uses is never one.
private val KOTLIN_PREVIEW_FUNCTION =
    Regex("""\bprivate\s+(?:\w+\s+)*fun\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?\w*Previews?\s*\(""")

/** The functions that are previews: annotated with a preview annotation, or private and named after one. */
private fun kotlinPreviewRanges(code: String): List<IntRange> {
    val annotated =
        KOTLIN_PREVIEW_ANNOTATION.findAll(code).mapNotNull { annotation ->
            KOTLIN_ANNOTATED_FUNCTION.find(code, annotation.range.last + 1)?.let { function ->
                functionEnd(code, function.range.last + 1)?.let { annotation.range.first..it }
            }
        }
    val named =
        KOTLIN_PREVIEW_FUNCTION.findAll(code).mapNotNull { function ->
            functionEnd(code, function.range.first)?.let { function.range.first..it }
        }
    return (annotated + named).toList()
}

/**
 * The end of the block body of the function whose declaration starts at [from]. Null for an expression body, whose
 * end the code does not mark, so that the exemption never runs on into the next declaration.
 */
private fun functionEnd(
    code: String,
    from: Int,
): Int? {
    val parameters = code.indexOf('(', from).takeIf { it >= 0 } ?: return null
    val afterParameters = closing(code, parameters) ?: return null
    val body = code.indexOfAny(charArrayOf('{', '='), afterParameters).takeIf { it >= 0 } ?: return null
    return if (code[body] == '{') closing(code, body) else null
}

private fun String.lineAt(offset: Int): Int = 1 + (0 until offset).count { this[it] == '\n' }
