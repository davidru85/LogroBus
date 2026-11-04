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
     * The hard-coded user-facing text in a Swift [source]. A string literal with a letter is user-facing where SwiftUI,
     * UIKit or a widget shows or speaks it: anywhere at the top level of the title of `Text`, `Button` and their kin,
     * of a navigation title, an accessibility label or announcement, an alert, a search prompt, a map pin, and in what
     * a member typed LocalizedStringKey or LocalizedStringResource holds. There it must be a catalogue key such as
     * `lines.title`, which interpolations may follow, and never verbatim text. Previews are exempt.
     */
    fun hardcodedSwiftUiText(source: String): List<UiTextFinding> {
        val lexed = SwiftLexer(source).lex()
        val previews = swiftPreviewRanges(lexed.code)
        return swiftTexts(lexed)
            .filter { text -> previews.none { text.literal.start in it } }
            .filter { it.literal.text.any(Char::isLetter) && (it.verbatim || !isCatalogueKey(it.literal.text)) }
            .map { UiTextFinding(source.lineAt(it.literal.start), it.literal.text) }
    }

    /**
     * The catalogue keys that a package target's Swift [source] looks up in the app's bundle instead of its own.
     * SwiftUI looks a key up in the app's bundle unless the call names one, which only `Text`, `String(localized:)`
     * and their kin can, with `bundle:`. A member typed LocalizedStringKey hands its keys to such a `Text`.
     */
    fun unbundledSwiftKeys(source: String): List<UiTextFinding> {
        val lexed = SwiftLexer(source).lex()
        val previews = swiftPreviewRanges(lexed.code)
        return swiftTexts(lexed)
            .filter { text -> previews.none { text.literal.start in it } }
            .filter { !it.verbatim && !it.bundled && isCatalogueKey(it.literal.text) }
            .map { UiTextFinding(source.lineAt(it.literal.start), it.literal.text.trim()) }
    }

    /**
     * The catalogue keys a Swift [source] uses: those it passes where SwiftUI shows or speaks text, and those its
     * members typed LocalizedStringKey or LocalizedStringResource hold. Each interpolation stands as [ARGUMENT], so
     * `"lines.towards \(headsign)"` uses `lines.towards ` followed by one argument.
     */
    fun swiftCatalogueKeys(source: String): Set<String> =
        swiftTexts(SwiftLexer(source).lex())
            .filter { !it.verbatim && isCatalogueKey(it.literal.text) }
            .map { it.literal.text.trim() }
            .toSet()

    /** The keys of a string catalogue's [json], each format argument (`%@`, `%lld`, `%1$@` …) as [ARGUMENT]. */
    fun catalogueKeys(json: String): Set<String> =
        Json
            .parseToJsonElement(json)
            .jsonObject["strings"]
            ?.jsonObject
            ?.keys
            .orEmpty()
            .map { it.replace(SWIFT_FORMAT_ARGUMENT, ARGUMENT.toString()) }
            .toSet()

    /**
     * The entries of a string catalogue's [json] without a translated value in Spanish or English (NFR-I18N-01), as
     * "key (language)": every string unit of the language, each plural form included, must be translated and not
     * empty. An entry marked not to translate needs only a value in the source language, whatever its state: Xcode
     * leaves the value it extracts for one marked new.
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
                    .filterNot { language ->
                        localizations[language]?.let { isTranslated(it, finalOnly = translatable) } == true
                    }.map { language -> "$key ($language)" }
            }.sorted()
    }

    /** A key of a string catalogue, such as `lines.title`, once the arguments that follow it are left out. */
    fun isCatalogueKey(text: String): Boolean = CATALOGUE_KEY.matches(text.replace(ARGUMENT, ' ').trim())
}

/** A literal that SwiftUI shows or speaks: given as [verbatim] text, or as a key looked up in a [bundled] bundle. */
private class SwiftText(
    val literal: StringLiteral,
    val verbatim: Boolean,
    val bundled: Boolean,
)

/** Every literal of a lexed Swift source that is shown or spoken, once, with how it is given. */
private fun swiftTexts(lexed: Lexed): List<SwiftText> {
    val calls =
        textCallSinks(lexed, SWIFT_TEXT_CALLS, SWIFT_LABEL).map { sink ->
            val bundled = sink.call in BUNDLE_CALLS && sink.callArguments.any { it.label == "bundle" }
            SwiftText(sink.literal, verbatim = sink.call == "Text" && sink.label == "verbatim", bundled = bundled)
        }
    val properties = textPropertySinks(lexed, SWIFT_TEXT_PROPERTY).map { SwiftText(it.literal, false, false) }
    return (calls + properties + localizedDeclarationTexts(lexed))
        .groupBy { it.literal.start }
        .map { (_, texts) ->
            SwiftText(texts.first().literal, texts.any { it.verbatim }, texts.any { it.bundled })
        }.sortedBy { it.literal.start }
}

/**
 * What members, constants and parameters typed LocalizedStringKey or LocalizedStringResource hold: every literal
 * of a computed member's body, but a comment or a table name, and the top-level literals of an initial value. A
 * LocalizedStringKey goes to a `Text` that names the bundle; a LocalizedStringResource literal names none.
 */
private fun localizedDeclarationTexts(lexed: Lexed): List<SwiftText> =
    SWIFT_LOCALIZED_DECLARATION
        .findAll(lexed.code)
        .flatMap { declaration ->
            val (type, opening) = declaration.destructured
            val bundled = type == "LocalizedStringKey"
            val literals =
                if (opening == "{") {
                    val body =
                        declaration.range.last..(closing(lexed.code, declaration.range.last) ?: declaration.range.last)
                    lexed.literals.filter {
                        it.start in body &&
                            !SWIFT_NOT_TEXT.containsMatchIn(lexed.codeBefore(it.start))
                    }
                } else {
                    lexed.topLevelLiterals(expressionAfter(lexed.code, declaration.range.last + 1))
                }
            literals.map { SwiftText(it, verbatim = false, bundled = bundled) }
        }.toList()

// SwiftUI's, UIKit's and WidgetKit's calls that show or speak their text, and where the text goes (LB-405).
private val SWIFT_TEXT_CALLS =
    listOf(
        TextCall(listOf("Text"), positions = setOf(0), labels = setOf("verbatim")),
        TextCall(
            listOf(
                "Button",
                "Label",
                "Toggle",
                "Picker",
                "Section",
                "LabeledContent",
                "Link",
                "Menu",
                "TextField",
                "SecureField",
                "Stepper",
                "ProgressView",
                "GroupBox",
                "DisclosureGroup",
                "NavigationLink",
                "ContentUnavailableView",
                "Tab",
                "ShareLink",
                "ControlGroup",
                "DatePicker",
                "Marker",
                "Annotation",
                "LocalizedStringKey",
                "LocalizedStringResource",
                "NSLocalizedString",
                "Announcement",
                "IntentDescription",
                "navigationTitle",
                "navigationSubtitle",
                "accessibilityLabel",
                "accessibilityHint",
                "accessibilityValue",
                "accessibilityInputLabels",
                "help",
                "badge",
                "alert",
                "confirmationDialog",
                "configurationDisplayName",
                "description",
            ),
            positions = setOf(0),
        ),
        TextCall(listOf("String", "AttributedString"), labels = setOf("localized")),
        TextCall(listOf("accessibilityAction"), labels = setOf("named")),
        TextCall(listOf("accessibilityCustomContent"), positions = setOf(0, 1)),
        TextCall(listOf("post"), labels = setOf("argument")),
        TextCall(listOf("Parameter", "DisplayRepresentation"), labels = setOf("title")),
        TextCall(listOf("searchable"), labels = setOf("prompt")),
    )

// The calls that can name the bundle to look a key up in.
private val BUNDLE_CALLS = setOf("Text", "String", "AttributedString", "LocalizedStringResource", "NSLocalizedString")

private val SWIFT_LABEL = Regex("""^\s*(\w+)\s*:(?!:)""")

// UIKit's text properties: `label.accessibilityLabel = …`.
private val SWIFT_TEXT_PROPERTY =
    Regex("""\.(?:accessibilityLabel|accessibilityHint|accessibilityValue|text|title|placeholder)\s*=(?!=)""")

// `var title: LocalizedStringKey {`, `-> LocalizedStringResource {`, `let title: LocalizedStringKey = `.
private val SWIFT_LOCALIZED_DECLARATION =
    Regex("""(?::|->)\s*(LocalizedStringKey|LocalizedStringResource)\??\s*([{=])(?!=)""")

// The arguments of a localized value that are not text.
private val SWIFT_NOT_TEXT = Regex("""\b(?:comment|table|tableName)\s*:\s*$""")

// A format argument of a catalogue key: `%@`, `%lld`, `%1$@`, `%.1f` …
private val SWIFT_FORMAT_ARGUMENT =
    Regex("""%(\d+\$)?[-+ 0#']*\d*(\.\d+)?(lld|llu|ld|lu|hhd|hd|d|i|u|o|x|X|@|f|lf|e|g|s|c)""")

// NFR-I18N-01: Spanish, the source language, and English.
private val APP_LANGUAGES = listOf("es", "en")

/**
 * Whether every string unit of a [localization] has a value, and, when [finalOnly], whether each is marked
 * translated rather than new or needing review.
 */
private fun isTranslated(
    localization: JsonElement,
    finalOnly: Boolean,
): Boolean {
    val units = stringUnits(localization)
    return units.isNotEmpty() &&
        units.all { unit ->
            (!finalOnly || unit["state"]?.jsonPrimitive?.content == "translated") &&
                !unit["value"]?.jsonPrimitive?.content.isNullOrEmpty()
        }
}

/** Every string unit in a localisation: its own, its plural or device variations', and its substitutions'. */
private fun stringUnits(element: JsonElement): List<JsonObject> =
    when (element) {
        is JsonObject -> element.flatMap { (name, value) -> unitsOf(name, value) }
        is JsonArray -> element.flatMap(::stringUnits)
        else -> emptyList()
    }

private fun unitsOf(
    name: String,
    value: JsonElement,
): List<JsonObject> = if (name == "stringUnit") listOf(value.jsonObject) else stringUnits(value)

private val CATALOGUE_KEY = Regex("""[a-z0-9_]+(?:\.[a-z0-9_]+)+""")

private val SWIFT_PREVIEW_MACRO = Regex("""#Preview\b""")

private val SWIFT_TYPE =
    Regex("""\b(?:(private|fileprivate)\s+)?(?:final\s+)?(?:struct|class|enum|extension)\s+(\w+)([^{]*)\{""")

/** The previews: `#Preview` blocks, PreviewProvider types, and private types named after a preview. */
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
                val (access, name, inheritance) = type.destructured
                val namedAfterAPreview = name.endsWith("Preview") || name.endsWith("Previews")
                "PreviewProvider" in inheritance || (access.isNotEmpty() && namedAfterAPreview)
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
