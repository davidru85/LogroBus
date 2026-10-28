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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Clock

/** The scope of work that lives as long as the process, such as the polling engine's loops. */
private val APPLICATION_SCOPE = named("application")

/**
 * The graph of the shared core (ADR-0014): one instance of each service for the whole process. [platform] provides
 * what only a platform can create: the [Logger], the app lifecycle, the location provider, the client identity for the
 * `User-Agent` and the SQLite driver.
 */
internal fun coreModules(platform: Module): List<Module> = listOf(platform, coreModule)

/** Starts the graph of [modules] for the process (ADR-0014). */
internal fun startGraph(modules: List<Module>) {
    startKoin { modules(modules) }
}

private val coreModule =
    module {
        // One clock for the whole app: the source client corrects it, and everything else reads it (spec §8.6).
        single { ServerCorrectedClock(deviceClock = Clock.System) } bind AppClock::class
        single { AppDispatchers() }
        single<RemoteConfig> { CompiledDefaultsRemoteConfig() }
        single(APPLICATION_SCOPE) { CoroutineScope(SupervisorJob() + get<AppDispatchers>().default) }
        single<PollingEngine> { DefaultPollingEngine(get(APPLICATION_SCOPE), get(), get<AppClock>(), get()) }
        single {
            val remoteConfig = get<RemoteConfig>()
            SourceClient(get(), get(), { remoteConfig.get(RemoteConfigKey.ApiBaseUrl) }, get())
        }
        single { logroBusDatabase(get()) }
    }

/** `architecture.md` §7: debug builds log from the debug level, release builds errors only. */
internal fun appLogger(
    debug: Boolean,
    output: (String) -> Unit = ::println,
): Logger = ConsoleLogger(minLevel = if (debug) LogLevel.Debug else LogLevel.Error, output = output)
