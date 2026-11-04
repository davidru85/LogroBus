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

/** A string literal: where its opening quote is, and its [text] without its template expressions. */
private class StringLiteral(
    val start: Int,
    val text: String,
)

/**
 * A source ready to scan: [code] is the source with every comment and the content of every string literal blanked
 * out, so patterns match code only and offsets stay those of the source; [literals] are the string literals.
 */
private class Lexed(
    val code: String,
    val literals: List<StringLiteral>,
) {
    fun codeBefore(offset: Int): String = code.substring(maxOf(0, offset - LOOK_BEHIND), offset)

    private companion object {
        const val LOOK_BEHIND = 200
    }
}

/** Kotlin's comments (nested block comments too), strings, raw strings, templates and characters. */
private class KotlinLexer(
    private val source: String,
) {
    private val code = source.toCharArray()
    private val literals = mutableListOf<StringLiteral>()
    private var at = 0

    fun lex(): Lexed {
        code(nested = false, record = true)
        return Lexed(String(code), literals)
    }

    /** Code up to the end, or up to the brace that closes a template expression when [nested]. */
    private fun code(
        nested: Boolean,
        record: Boolean,
    ) {
        var depth = 0
        while (at < source.length) {
            when {
                source.startsWith("//", at) -> {
                    lineComment()
                }

                source.startsWith("/*", at) -> {
                    blockComment()
                }

                source.startsWith(RAW_QUOTE, at) -> {
                    string(raw = true, record = record)
                }

                source[at] == '"' -> {
                    string(raw = false, record = record)
                }

                source[at] == '\'' -> {
                    character()
                }

                source[at] == '{' -> {
                    depth++
                    at++
                }

                source[at] == '}' && nested && depth == 0 -> {
                    at++
                    return
                }

                source[at] == '}' -> {
                    depth--
                    at++
                }

                else -> {
                    at++
                }
            }
        }
    }

    private fun lineComment() {
        val end = source.indexOf('\n', at).takeIf { it >= 0 } ?: source.length
        blank(at, end)
        at = end
    }

    private fun blockComment() {
        val start = at
        var depth = 0
        while (at < source.length) {
            when {
                source.startsWith("/*", at) -> {
                    depth++
                    at += 2
                }

                source.startsWith("*/", at) -> {
                    depth--
                    at += 2
                }

                else -> {
                    at++
                }
            }
            if (depth == 0) break
        }
        blank(start, at)
    }

    private fun string(
        raw: Boolean,
        record: Boolean,
    ) {
        val start = at
        val quote = if (raw) RAW_QUOTE else "\""
        at += quote.length
        val text = StringBuilder()
        while (at < source.length && !source.startsWith(quote, at)) {
            val char = source[at]
            val next = source.getOrNull(at + 1)
            when {
                !raw && char == '\\' -> {
                    escape(text)
                }

                char == '$' && next == '{' -> {
                    at += 2
                    code(nested = true, record = false)
                }

                char == '$' && next != null && (next.isLetter() || next == '_') -> {
                    templateName()
                }

                else -> {
                    text.append(char)
                    at++
                }
            }
        }
        // A raw string closes on the last three quotes of a longer run.
        while (raw && source.startsWith("$RAW_QUOTE\"", at)) text.append('"').also { at++ }
        blank(start + quote.length, at)
        at = minOf(at + quote.length, source.length)
        if (record) literals += StringLiteral(start, text.toString())
    }

    private fun escape(text: StringBuilder) {
        val escaped = source.getOrNull(at + 1)
        val unicode =
            source
                .substring(minOf(at + 2, source.length), minOf(at + UNICODE_END, source.length))
                .takeIf { escaped == 'u' && it.length == UNICODE_DIGITS }
                ?.toIntOrNull(HEX)
        when {
            unicode != null -> {
                text.append(unicode.toChar())
                at += UNICODE_END
            }

            else -> {
                // \n, \t and the like are whitespace, not letters; the rest stand for themselves.
                text.append(if (escaped in WHITESPACE_ESCAPES) ' ' else escaped ?: ' ')
                at += 2
            }
        }
    }

    private fun templateName() {
        at++
        while (at < source.length && (source[at].isLetterOrDigit() || source[at] == '_')) at++
    }

    private fun character() {
        val start = at
        at++
        if (source.getOrNull(at) == '\\') at += 2
        while (at < source.length && source[at] != '\'' && source[at] != '\n') at++
        at = minOf(at + 1, source.length)
        blank(start, at)
    }

    private fun blank(
        from: Int,
        until: Int,
    ) {
        for (index in from until minOf(until, source.length)) {
            if (code[index] != '\n') code[index] = ' '
        }
    }

    private companion object {
        const val RAW_QUOTE = "\"\"\""
        const val UNICODE_DIGITS = 4
        const val UNICODE_END = 6
        const val HEX = 16
        val WHITESPACE_ESCAPES = setOf('n', 't', 'r', 'b')
    }
}

private fun String.lineAt(offset: Int): Int = 1 + (0 until offset).count { this[it] == '\n' }
