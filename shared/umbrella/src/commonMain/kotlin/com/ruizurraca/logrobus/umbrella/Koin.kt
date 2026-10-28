package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.AppClock
import com.ruizurraca.logrobus.core.common.AppDispatchers
import com.ruizurraca.logrobus.core.common.ConsoleLogger
import com.ruizurraca.logrobus.core.common.LogLevel
import com.ruizurraca.logrobus.core.common.Logger
import com.ruizurraca.logrobus.core.common.ServerCorrectedClock
import com.ruizurraca.logrobus.core.config.CompiledDefaultsRemoteConfig
import com.ruizurraca.logrobus.core.config.RemoteConfig
import com.ruizurraca.logrobus.core.config.RemoteConfigKey
import com.ruizurraca.logrobus.core.database.logroBusDatabase
import com.ruizurraca.logrobus.core.network.SourceClient
import com.ruizurraca.logrobus.core.polling.DefaultPollingEngine
import com.ruizurraca.logrobus.core.polling.PollingEngine
import com.ruizurraca.logrobus.feature.settings.domain.DefaultObserveLastSyncs
import com.ruizurraca.logrobus.feature.settings.domain.ObserveLastSyncs
import com.ruizurraca.logrobus.transit.data.SyncStatusRepositoryImpl
import com.ruizurraca.logrobus.transit.domain.SyncStatusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose
import kotlin.time.Clock

/** The scope of work that lives as long as the process, such as the polling engine's loops. */
internal val APPLICATION_SCOPE = named("application")

/**
 * The shared graph (ADR-0014): the core, with one instance of each service for the whole process, then one module per
 * data and feature module. [platform] provides what only a platform can create: the [Logger], the app lifecycle, the
 * location provider, the client identity for the `User-Agent` and the SQLite driver.
 */
internal fun sharedModules(platform: Module): List<Module> =
    listOf(platform, coreModule, transitDataModule, settingsModule)

/**
 * Starts the graph of [modules] for the process (ADR-0014). A second definition of a type fails here, instead of
 * quietly replacing the first: a replaced AppClock, say, would be one the source client never corrects.
 */
internal fun startGraph(modules: List<Module>) {
    startKoin {
        allowOverride(false)
        modules(modules)
    }
}

private val coreModule =
    module {
        // One clock for the whole app: the source client corrects it, and everything else reads it (spec §8.6).
        single { ServerCorrectedClock(deviceClock = Clock.System) } bind AppClock::class
        single { AppDispatchers() }
        single<RemoteConfig> { CompiledDefaultsRemoteConfig() }
        // Stopping the graph cancels the work it started, such as polling loops.
        single(APPLICATION_SCOPE) {
            CoroutineScope(SupervisorJob() + get<AppDispatchers>().default)
        } onClose { it?.cancel() }
        single<PollingEngine> { DefaultPollingEngine(get(APPLICATION_SCOPE), get(), get<AppClock>(), get()) }
        single {
            val remoteConfig = get<RemoteConfig>()
            SourceClient(get(), get(), { remoteConfig.get(RemoteConfigKey.ApiBaseUrl) }, get())
        }
        single { logroBusDatabase(get()) }
    }

private val transitDataModule =
    module {
        single<SyncStatusRepository> { SyncStatusRepositoryImpl(get(), get(), get()) }
    }

private val settingsModule =
    module {
        factory<ObserveLastSyncs> { DefaultObserveLastSyncs(get()) }
    }

/** `architecture.md` §7: debug builds log from the debug level, release builds errors only. */
internal fun appLogger(debug: Boolean): Logger = ConsoleLogger(minLevel(debug))

/** The same, printing to [output], for tests. */
internal fun appLogger(
    debug: Boolean,
    output: (String) -> Unit,
): Logger = ConsoleLogger(minLevel(debug), output)

private fun minLevel(debug: Boolean): LogLevel = if (debug) LogLevel.Debug else LogLevel.Error
