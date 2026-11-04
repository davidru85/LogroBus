package com.ruizurraca.logrobus.architecture

/** A user-facing [text] written into UI code at its 1-based [line], rather than taken from the string resources. */
data class UiTextFinding(
    val line: Int,
    val text: String,
)

/** LB-405 (NFR-I18N-01): finds user-facing text written into UI code instead of the platforms' string resources. */
object UiTextRules {
    /**
     * The hard-coded user-facing text in a Kotlin [source]. A string literal with a letter is user-facing where
     * Compose shows or speaks it: the text of `Text` and its kin, a spoken label or description, a snackbar or toast
     * message. Template expressions are not text, and previews are exempt.
     */
    fun hardcodedKotlinUiText(source: String): List<UiTextFinding> {
        val lexed = KotlinLexer(source).lex()
        val previews = kotlinPreviewRanges(lexed.code)
        return lexed.literals
            .filter { literal -> previews.none { literal.start in it } }
            .filter { it.text.any(Char::isLetter) && KOTLIN_UI_TEXT.containsMatchIn(lexed.codeBefore(it.start)) }
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

// What comes right before a literal that Compose shows or speaks: the first argument of a text call, a toast's
// message, or a parameter or semantics property that holds text.
private val KOTLIN_UI_TEXT =
    Regex(
        """(?:\b(?:Text|BasicText|AnnotatedString|CustomAccessibilityAction|showSnackbar)\(\s*""" +
            """(?:(?:label|message)\s*=\s*)?""" +
            """|\bmakeText\([^,()]*,\s*""" +
            """|\b(?:text|contentDescription|stateDescription|onClickLabel|onLongClickLabel|paneTitle|actionLabel)""" +
            """\s*=\s*)$""",
    )

private val KOTLIN_PREVIEW_ANNOTATION = Regex("""@\w*Preview\w*""")

// After a preview annotation: its arguments, more annotations and modifiers, then a function.
private val KOTLIN_ANNOTATED_FUNCTION =
    Regex(
        """\G(?:\s*\([^()]*(?:\([^()]*\)[^()]*)*\))?(?:\s*@[\w.]+(?:\s*\([^()]*(?:\([^()]*\)[^()]*)*\))?)*\s*""" +
            """(?:(?:private|internal|public|protected|inline|override|open|actual|expect)\s+)*fun\b""",
    )

private val KOTLIN_PREVIEW_FUNCTION = Regex("""\bfun\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?\w*Previews?\s*\(""")

/** The functions that are previews: annotated with a preview annotation, or named after one. */
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

/** The end of the block body of the function whose declaration starts at [from], or null without one. */
private fun functionEnd(
    code: String,
    from: Int,
): Int? {
    val parameters = code.indexOf('(', from).takeIf { it >= 0 } ?: return null
    val afterParameters = closing(code, parameters) ?: return null
    val body = code.indexOf('{', afterParameters).takeIf { it >= 0 } ?: return null
    return closing(code, body)
}

/** The index of the bracket that closes the one at [open], in code whose comments and strings are blanked. */
private fun closing(
    code: String,
    open: Int,
): Int? {
    val opening = code[open]
    val close = if (opening == '(') ')' else '}'
    var depth = 0
    for (index in open until code.length) {
        when (code[index]) {
            opening -> depth++
            close -> if (--depth == 0) return index
        }
    }
    return null
}

private fun String.lineAt(offset: Int): Int = 1 + (0 until offset).count { this[it] == '\n' }
