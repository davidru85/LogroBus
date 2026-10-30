package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.database.LogroBusDatabase
import com.ruizurraca.logrobus.transit.domain.StaticSyncEngine

/** The static sync of architecture.md §4.4. */
@Suppress("UnusedPrivateProperty") // A stub until its green commit.
internal class DefaultStaticSyncEngine(
    private val source: StaticSource,
    private val normaliser: StaticNormaliser,
    private val database: LogroBusDatabase,
    private val clock: AppClock,
    private val dispatchers: AppDispatchers,
    private val logger: Logger,
) : StaticSyncEngine {
    override suspend fun syncIfStale(): AppResult<Unit> = AppResult.Success(Unit)
}
