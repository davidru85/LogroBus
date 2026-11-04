package com.ruizurraca.logrobus.architecture

/**
 * A call whose arguments hold user-facing text (LB-405): the unlabelled ones at [positions], counted from 0 among all
 * the arguments, and those labelled with one of [labels].
 */
internal class TextCall(
    val callees: List<String>,
    val positions: Set<Int> = emptySet(),
    val labels: Set<String> = emptySet(),
)

/** An argument of a call: its [label], if it has one, and the span of its [value] in the code. */
internal class Argument(
    val label: String?,
    val value: IntRange,
)

/** A string literal where the UI shows or speaks it, and the [call] and [label] it was given to, if any. */
internal class TextSink(
    val literal: StringLiteral,
    val call: String? = null,
    val label: String? = null,
    val callArguments: List<Argument> = emptyList(),
)

/**
 * The literals given to [calls] in [lexed] code: every literal at the top level of an argument that holds text, so
 * `Text(name ?: "Unknown")` counts, and so do the literals inside a counted literal's interpolations. A literal
 * inside a nested call or lambda belongs to that call. [labelled] reads an argument's label from its start.
 */
internal fun textCallSinks(
    lexed: Lexed,
    calls: List<TextCall>,
    labelled: Regex,
): List<TextSink> {
    val byCallee = calls.flatMap { call -> call.callees.map { it to call } }.toMap()
    val callee = Regex("""\b(${byCallee.keys.joinToString("|")})\s*\(""")
    return callee
        .findAll(lexed.code)
        .flatMap { match ->
            val call = byCallee.getValue(match.groupValues[1])
            val arguments = arguments(lexed.code, match.range.last, labelled)
            arguments.withIndex().flatMap { (index, argument) ->
                val holdsText =
                    if (argument.label == null) index in call.positions else argument.label in call.labels
                if (holdsText) {
                    lexed.topLevelLiterals(argument.value).map {
                        TextSink(it, match.groupValues[1], argument.label, arguments)
                    }
                } else {
                    emptyList()
                }
            }
        }.toList()
}

/** The literals of the expression that follows each match of [property], such as `contentDescription =`. */
internal fun textPropertySinks(
    lexed: Lexed,
    property: Regex,
): List<TextSink> =
    property
        .findAll(lexed.code)
        .flatMap { match -> lexed.topLevelLiterals(expressionAfter(lexed.code, match.range.last + 1)) }
        .map { TextSink(it) }
        .toList()

/**
 * The arguments of the call whose opening parenthesis is at [open], split at its top-level commas. [labelled] reads
 * a label (`text =` in Kotlin, `verbatim:` in Swift) from the start of an argument.
 */
internal fun arguments(
    code: String,
    open: Int,
    labelled: Regex,
): List<Argument> {
    val close = closing(code, open) ?: return emptyList()
    val pieces = mutableListOf<IntRange>()
    var start = open + 1
    var depth = 0
    for (index in open + 1 until close) {
        when (code[index]) {
            '(', '[', '{' -> {
                depth++
            }

            ')', ']', '}' -> {
                depth--
            }

            ',' -> {
                if (depth == 0) {
                    pieces += start until index
                    start = index + 1
                }
            }
        }
    }
    if (code.substring(start, close).isNotBlank()) pieces += start until close
    return pieces.map { piece ->
        val label = labelled.find(code.substring(piece.first, piece.last + 1))
        Argument(label?.groupValues?.get(1), (piece.first + (label?.range?.last?.plus(1) ?: 0))..piece.last)
    }
}

/** The span of the expression that starts at [from]: up to a top-level comma, semicolon, line end or closing bracket. */
internal fun expressionAfter(
    code: String,
    from: Int,
): IntRange {
    var start = from
    while (start < code.length && code[start].isWhitespace()) start++
    var depth = 0
    var end = start
    while (end < code.length) {
        val char = code[end]
        val closesTheExpression = char in ")]}" && depth == 0
        val endsAtTheTopLevel = depth == 0 && (char == ',' || char == ';' || char == '\n')
        if (closesTheExpression || endsAtTheTopLevel) break
        if (char in "([{") depth++
        if (char in ")]}") depth--
        end++
    }
    return start until end
}

/**
 * The literals at the top level of [span], outside nested calls and lambdas, with those in their interpolations,
 * whose code is blanked with them. Brackets do not nest here, so the items of an array literal count.
 */
internal fun Lexed.topLevelLiterals(span: IntRange): List<StringLiteral> {
    val inSpan = literals.filter { it.start in span }.sortedBy { it.start }
    var depth = 0
    var next = span.first
    return inSpan.filter { literal ->
        while (next < literal.start) {
            when (code[next]) {
                '(', '{' -> depth++
                ')', '}' -> depth--
            }
            next++
        }
        depth == 0
    }
}

/** The index of the bracket that closes the one at [open], in code whose comments and strings are blanked. */
internal fun closing(
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
