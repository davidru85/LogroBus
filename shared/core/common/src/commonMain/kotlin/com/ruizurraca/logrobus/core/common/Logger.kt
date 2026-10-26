package com.ruizurraca.logrobus.core.common

enum class LogLevel(
    internal val letter: Char,
) {
    Debug('D'),
    Info('I'),
    Warn('W'),
    Error('E'),
}

/**
 * The only way shared code logs (`architecture.md` §7). Messages never contain coordinates, search text or personal
 * data (NFR-PRIV-02): log ids, counts and outcomes instead.
 */
interface Logger {
    fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
    )
}

fun Logger.debug(
    tag: String,
    message: String,
) = log(LogLevel.Debug, tag, message)

fun Logger.info(
    tag: String,
    message: String,
) = log(LogLevel.Info, tag, message)

fun Logger.warn(
    tag: String,
    message: String,
    throwable: Throwable? = null,
) = log(LogLevel.Warn, tag, message, throwable)

fun Logger.error(
    tag: String,
    message: String,
    throwable: Throwable? = null,
) = log(LogLevel.Error, tag, message, throwable)

/** Logs nothing. */
object NoOpLogger : Logger {
    override fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = Unit
}

/**
 * Prints `L/tag: message` lines and drops entries below [minLevel]. A throwable is printed as the types of its cause
 * chain, `[Outer <- Cause]`, never with its messages or stack trace: a message can quote a request URL with search
 * text, or a payload with coordinates (NFR-PRIV-02). [output] is the standard output by default: Logcat on Android,
 * the Xcode console on iOS.
 */
@Suppress("ForbiddenMethodCall") // println is forbidden in favour of Logger; this is the Logger that prints.
class ConsoleLogger(
    private val minLevel: LogLevel = LogLevel.Debug,
    private val output: (String) -> Unit = ::println,
) : Logger {
    override fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        if (level < minLevel) return
        val line = "${level.letter}/$tag: $message"
        output(if (throwable == null) line else "$line [${throwable.typeChain()}]")
    }
}

/** How many causes are printed at most, which also stops a cause cycle. */
private const val MAX_CAUSES = 8

private fun Throwable.typeChain(): String =
    generateSequence(this) { it.cause }
        .take(MAX_CAUSES)
        .joinToString(" <- ") { it::class.simpleName ?: "Throwable" }
