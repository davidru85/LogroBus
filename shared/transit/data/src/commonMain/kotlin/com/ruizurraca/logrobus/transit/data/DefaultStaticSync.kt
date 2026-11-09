package com.ruizurraca.logrobus.transit.data

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.config.RemoteConfig
import com.ruizurraca.logrobus.core.config.RemoteConfigKey
import com.ruizurraca.logrobus.core.database.LogroBusDatabase
import com.ruizurraca.logrobus.core.network.SourceClient
import com.ruizurraca.logrobus.transit.domain.StaticSyncEngine

/**
 * The static sync over the real source, assembled for the composition root (LB-008 part 2, architecture.md §4.4).
 *
 * The engine, its source and its normaliser are all `internal` to this module: they are wiring, not a contract any
 * other module should reach for. The umbrella gets the one thing it needs, a [StaticSyncEngine], and keeps the rest
 * out of its graph. The Koin graph builds one of these per process, so the engine's mutex runs one sync at a time.
 */
class DefaultStaticSync(
    client: SourceClient,
    remoteConfig: RemoteConfig,
    database: LogroBusDatabase,
    clock: AppClock,
    dispatchers: AppDispatchers,
    logger: Logger,
) {
    /**
     * The engine of architecture.md §4.4. DD-11's overrides come from remote config on every construction, so a
     * changed override set changes the normaliser's fingerprint and an unchanged payload is renormalised, which is
     * what [StaticSyncEngine] and the sync rely on.
     */
    val engine: StaticSyncEngine =
        DefaultStaticSyncEngine(
            source = SourceStaticSource(client),
            normaliser = DefaultStaticNormaliser(remoteConfig.get(RemoteConfigKey.NameOverrides), logger),
            database = database,
            clock = clock,
            dispatchers = dispatchers,
            logger = logger,
        )
}
