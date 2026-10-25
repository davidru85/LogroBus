package com.ruizurraca.logrobus.core.common

/**
 * Why a call to the source failed. Shared mappers turn each case into a standard UX state (spec §7.3), and a
 * failure affects only the resource it belongs to (NFR-REL-02).
 */
sealed interface AppError {
    /** No connection, a timeout or another transport failure. */
    data object Network : AppError

    /** A response with a non-2xx [status]. */
    data class Server(
        val status: Int,
    ) : AppError

    /** A body that is not JSON, misses its envelope key or cannot be decoded. */
    data object Data : AppError

    /** The requested resource does not exist. */
    data object NotFound : AppError
}

/** The outcome of a repository call: a [Success] value or a typed [Failure] (AGENTS.md §5.11). */
sealed interface AppResult<out T> {
    data class Success<out T>(
        val value: T,
    ) : AppResult<T>

    data class Failure(
        val error: AppError,
    ) : AppResult<Nothing>
}
