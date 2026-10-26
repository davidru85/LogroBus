package com.ruizurraca.logrobus.core.common

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * AGENTS.md §5.11: repositories return `AppResult` with a typed `AppError`, which shared mappers turn into the
 * standard UX states.
 */
class AppResultTest {
    // No `else` branches: these only compile while the matches are exhaustive.
    private fun describe(error: AppError): String =
        when (error) {
            AppError.Network -> "network"
            is AppError.Server -> "server ${error.status}"
            AppError.Data -> "data"
            AppError.NotFound -> "not found"
        }

    private fun <T> describe(result: AppResult<T>): String =
        when (result) {
            is AppResult.Success -> "success ${result.value}"
            is AppResult.Failure -> "failure ${describe(result.error)}"
        }

    @Test
    fun lb004_appErrorCoversNetworkServerDataAndNotFound() {
        assertEquals(
            listOf("network", "server 503", "data", "not found"),
            listOf(AppError.Network, AppError.Server(503), AppError.Data, AppError.NotFound).map(::describe),
        )
    }

    @Test
    fun lb004_appResultIsExhaustivelyMatchable() {
        assertEquals("success 42", describe(AppResult.Success(42)))
        assertEquals("failure server 500", describe(AppResult.Failure(AppError.Server(500))))
    }
}
