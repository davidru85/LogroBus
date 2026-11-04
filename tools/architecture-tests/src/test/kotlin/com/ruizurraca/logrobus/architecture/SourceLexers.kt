package com.ruizurraca.logrobus.architecture

/** What a string literal's text holds for each interpolation or template: one argument, and not a letter. */
internal const val ARGUMENT = '\uFFFC'

/**
 * A string literal: where it opens (its first quote or raw-string delimiter) and where it [end]s, after its closing
 * delimiter, and its [text], with an [ARGUMENT] for each interpolation.
 */
internal class StringLiteral(
    val start: Int,
    val end: Int,
    val text: String,
)

/**
 * A source ready to scan: [code] is the source with every comment and the content of every string literal blanked
 * out, so patterns match code only and offsets stay those of the source; [literals] are the string literals, those in
 * interpolations and templates included.
 */
internal class Lexed(
    val code: String,
    val literals: List<StringLiteral>,
) {
    fun codeBefore(offset: Int): String = code.substring(maxOf(0, offset - LOOK_BEHIND), offset)

    private companion object {
        const val LOOK_BEHIND = 200
    }
}

/** Splits a C-like source into code, comments (nested block comments too) and string literals. */
internal abstract class Lexer(
    protected val source: String,
) {
    private val code = source.toCharArray()
    private val literals = mutableListOf<StringLiteral>()
    protected var at = 0

    fun lex(): Lexed {
        code(closing = null, record = true)
        return Lexed(String(code), literals)
    }

    /** Reads the string (or character) literal that starts at [at], if one does, and says whether it did. */
    protected abstract fun literal(record: Boolean): Boolean

    /** Code up to the end, or up to the [closing] bracket that ends an interpolation; [record] keeps its strings. */
    protected fun code(
        closing: Char?,
        record: Boolean,
    ) {
        val opening = OPENING[closing]
        var depth = 0
        while (at < source.length) {
            val char = source[at]
            when {
                source.startsWith("//", at) -> {
                    lineComment()
                }

                source.startsWith("/*", at) -> {
                    blockComment()
                }

                literal(record) -> {
                    Unit
                }

                char == closing && depth == 0 -> {
                    at++
                    return
                }

                else -> {
                    if (char == opening) depth++
                    if (char == closing) depth--
                    at++
                }
            }
        }
    }

    protected fun record(
        start: Int,
        text: String,
    ) {
        literals += StringLiteral(start, at, text)
    }

    protected fun blank(
        from: Int,
        until: Int,
    ) {
        for (index in from until minOf(until, source.length)) {
            if (code[index] != '\n') code[index] = ' '
        }
    }

    /** The escape at [at], whose letter follows a prefix of [length]: \n and the like read as a space, not a letter. */
    protected fun escape(
        text: StringBuilder,
        length: Int,
    ) {
        val escaped = source.getOrNull(at + length)
        at += length + 1
        when (escaped) {
            'u' -> text.appendCodePoint(unicode())
            null, in WHITESPACE_ESCAPES -> text.append(' ')
            else -> text.append(escaped)
        }
    }

    /** The code point of a Unicode escape after its `u`: Kotlin's four hex digits, or Swift's `{…}`. */
    private fun unicode(): Int {
        val braced = source.getOrNull(at) == '{'
        val digits =
            if (braced) {
                source.substring(at + 1, source.indexOf('}', at).takeIf { it >= 0 } ?: at + 1)
            } else {
                source.substring(at, minOf(at + UNICODE_DIGITS, source.length))
            }
        at += if (braced) digits.length + 2 else digits.length
        return digits.toIntOrNull(HEX) ?: ' '.code
    }

    private fun lineComment() {
        val end = source.indexOf('\n', at).takeIf { it >= 0 } ?: source.length
        blank(at, end)
        at = end
    }

    private fun blockComment() {
        val start = at
        var depth = 0
        do {
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
        } while (depth > 0 && at < source.length)
        blank(start, at)
    }

    protected companion object {
        const val TRIPLE_QUOTE = "\"\"\""
        private val OPENING = mapOf('}' to '{', ')' to '(')
        private val WHITESPACE_ESCAPES = setOf('n', 't', 'r', 'b', '0')
        private const val UNICODE_DIGITS = 4
        private const val HEX = 16
    }
}

/** Kotlin: strings, raw strings, `$name` and `${…}` templates, and characters. */
internal class KotlinLexer(
    source: String,
) : Lexer(source) {
    override fun literal(record: Boolean): Boolean {
        when {
            source.startsWith(TRIPLE_QUOTE, at) -> string(raw = true, record = record)
            source[at] == '"' -> string(raw = false, record = record)
            source[at] == '\'' -> character()
            else -> return false
        }
        return true
    }

    private fun string(
        raw: Boolean,
        record: Boolean,
    ) {
        val start = at
        val quote = if (raw) TRIPLE_QUOTE else "\""
        at += quote.length
        val text = StringBuilder()
        // A plain string ends with its line, so that a stray quote never hides the rest of the file.
        while (at < source.length && !source.startsWith(quote, at) && (raw || source[at] != '\n')) {
            val char = source[at]
            val next = source.getOrNull(at + 1)
            when {
                !raw && char == '\\' -> {
                    escape(text, 1)
                }

                char == '$' && next == '{' -> {
                    at += 2
                    code(closing = '}', record = record)
                    text.append(ARGUMENT)
                }

                char == '$' && next != null && (next.isLetter() || next == '_') -> {
                    templateName()
                    text.append(ARGUMENT)
                }

                else -> {
                    text.append(char)
                    at++
                }
            }
        }
        // A raw string closes on the last three quotes of a longer run.
        while (raw && source.startsWith("$TRIPLE_QUOTE\"", at)) {
            text.append('"')
            at++
        }
        blank(start + quote.length, at)
        at = minOf(at + quote.length, source.length)
        if (record) record(start, text.toString())
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
}

/** Swift: strings, multi-line strings, raw strings with `#` delimiters, and `\(…)` interpolations. */
internal class SwiftLexer(
    source: String,
) : Lexer(source) {
    override fun literal(record: Boolean): Boolean {
        var quote = at
        while (quote < source.length && source[quote] == '#') quote++
        if (source.getOrNull(quote) != '"') return false
        string(hashes = quote - at, record = record)
        return true
    }

    private fun string(
        hashes: Int,
        record: Boolean,
    ) {
        val start = at
        val delimiter = "#".repeat(hashes)
        at += hashes
        val multiline = source.startsWith(TRIPLE_QUOTE, at)
        val close = (if (multiline) TRIPLE_QUOTE else "\"") + delimiter
        val escape = "\\" + delimiter
        at += if (multiline) TRIPLE_QUOTE.length else 1
        val contentStart = at
        val text = StringBuilder()
        while (at < source.length && !source.startsWith(close, at) && (multiline || source[at] != '\n')) {
            when {
                source.startsWith("$escape(", at) -> {
                    at += escape.length + 1
                    code(closing = ')', record = record)
                    text.append(ARGUMENT)
                }

                source.startsWith(escape, at) -> {
                    escape(text, escape.length)
                }

                else -> {
                    text.append(source[at])
                    at++
                }
            }
        }
        blank(contentStart, at)
        at = minOf(at + close.length, source.length)
        if (record) record(start, text.toString())
    }
}
