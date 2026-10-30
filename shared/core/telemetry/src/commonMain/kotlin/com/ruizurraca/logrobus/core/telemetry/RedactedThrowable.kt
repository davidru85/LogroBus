package com.ruizurraca.logrobus.core.telemetry

/**
 * An error as the reporter behind the consent gate receives it: each error of the chain with its type and stack
 * frames, and none of its messages, which can quote a request URL with what the user typed or a payload with
 * coordinates (NFR-PRIV-02). On Android it also carries the original stack trace, which vendors read from the
 * Throwable itself; on iOS, a vendor implementation reads [frames].
 */
class RedactedThrowable internal constructor(
    /** The original error's class name. */
    val type: String,
    /** The original error's own stack frames, as text, without its message. */
    val frames: List<String>,
    cause: RedactedThrowable?,
) : Throwable(type, cause)

/** This error and its causes, redacted. A cause chain is cut after [MAX_CAUSES] errors, which also stops a cycle. */
internal fun Throwable.redacted(): RedactedThrowable = redacted(depth = 1)

private fun Throwable.redacted(depth: Int): RedactedThrowable {
    if (this is RedactedThrowable) return this
    val redactedCause = cause?.takeIf { depth < MAX_CAUSES }?.redacted(depth + 1)
    val type = this::class.qualifiedName ?: this::class.simpleName ?: "Throwable"
    return RedactedThrowable(type, stackFrames(), redactedCause).also { it.adoptStackOf(this) }
}

/** The types of this error and its causes, `IllegalStateException <- IOException`, never their messages. */
internal fun Throwable.typeChain(): String =
    generateSequence(this) { it.cause }
        .take(MAX_CAUSES)
        .joinToString(" <- ") { error ->
            (error as? RedactedThrowable)?.type?.substringAfterLast('.') ?: error::class.simpleName ?: "Throwable"
        }

/** The frames of this error's own stack, as text, without its message. */
internal expect fun Throwable.stackFrames(): List<String>

/** Gives this error the stack of [original], where the platform can. */
internal expect fun RedactedThrowable.adoptStackOf(original: Throwable)

private const val MAX_CAUSES = 8
