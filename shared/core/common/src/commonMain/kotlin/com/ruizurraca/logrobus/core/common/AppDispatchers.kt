package com.ruizurraca.logrobus.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * The dispatchers shared code runs on, injected rather than referenced (AGENTS.md §8), so that tests can run on a
 * test dispatcher and virtual time.
 */
class AppDispatchers(
    val main: CoroutineDispatcher = Dispatchers.Main,
    val default: CoroutineDispatcher = Dispatchers.Default,
    val io: CoroutineDispatcher = Dispatchers.IO,
) {
    /** One dispatcher for all three, typically a test dispatcher. */
    constructor(all: CoroutineDispatcher) : this(main = all, default = all, io = all)
}
