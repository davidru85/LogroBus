package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.common.AppError
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.core.preferences.EtaFormat
import com.ruizurraca.logrobus.core.preferences.ThemePreference
import com.ruizurraca.logrobus.transit.domain.Arrival
import com.ruizurraca.logrobus.transit.domain.StaticDataset
import kotlin.time.Instant

// The scripts of `SharedDependencies.preview()`, as extensions so Swift chains them and so `PreviewUseCases` keeps the
// use cases rather than the states tests set. Each one replaces the last, except [favouriteLine], [settings], the
// location ones and [timePasses], which stack.

/** Scripts a first load that never ends: the use cases emit nothing. */
fun PreviewUseCases.loading(): PreviewUseCases = script(lastSuccesses = null, lines = emptyList(), sync = null)

/** Scripts an app that has never synced, and a sync that stored no lines. */
fun PreviewUseCases.neverSynced(): PreviewUseCases = script(lastSuccesses = emptyMap(), lines = emptyList())

/** Scripts a first launch with no network: nothing has synced, and syncing fails with a network error. */
fun PreviewUseCases.offline(): PreviewUseCases =
    script(lastSuccesses = emptyMap(), lines = emptyList(), sync = AppResult.Failure(AppError.Network))

/** Scripts the default again: every dataset synced, with the sample lines. Retry then loads them. */
fun PreviewUseCases.synced(): PreviewUseCases =
    script(lastSuccesses = StaticDataset.entries.associateWith { PreviewArrivals.SAMPLE_SYNC }, lines = SampleLines.all)

/**
 * LB-105: scripts two calls of line 10, one in 4 min and one low-confidence, so a preview or a snapshot of the line
 * detail has an ETA to show. An extension, as the other scripts are, so Swift chains it: `preview().withArrivals()`.
 */
fun PreviewUseCases.withArrivals(): PreviewUseCases =
    calls(PreviewArrivals.A_RIDE_AWAY, PreviewArrivals.A_ROUGHER_PREDICTION)

/**
 * LB-105: scripts one call only, at line 10's outbound first stop, so a detail has a stop with a call and a stop
 * without one: the criterion's dash needs a row that exists. An extension, so Swift chains it.
 */
fun PreviewUseCases.withOneArrival(): PreviewUseCases = calls(PreviewArrivals.A_RIDE_AWAY)

/** LB-105: scripts the calls a preview's arrivals use case emits, replacing the last script. */
fun PreviewUseCases.calls(vararg calls: Arrival): PreviewUseCases =
    apply {
        arrivalsPreview.script(calls.toList())
    }

/**
 * Scripts the user's settings (LB-403), as if they had chosen them before. An extension, so Swift chains it like the
 * other scripts: `preview().settings(theme: .dark, etaFormat: .clockTime)`.
 */
fun PreviewUseCases.settings(
    theme: ThemePreference,
    etaFormat: EtaFormat,
): PreviewUseCases = apply { settingsPreview.script(theme, etaFormat) }

/**
 * Scripts location turned on (LB-201), as a granted prompt would: the next read of location access finds it on. An
 * extension, so Swift chains it like the other scripts: `preview().locationOn()`.
 */
fun PreviewUseCases.locationOn(): PreviewUseCases = apply { locationPreview.turnOn() }

/** Scripts location turned off (LB-201), as a revoked permission would: the running updates end. */
fun PreviewUseCases.locationOff(): PreviewUseCases = apply { locationPreview.turnOff() }

/**
 * Scripts [minutes] passing on the clock (LB-303), past midnight say, so a test sees what a screen kept on view makes
 * of it. An extension, so Swift chains it like the other scripts: `preview().timePasses(minutes: 1440)`.
 */
fun PreviewUseCases.timePasses(minutes: Int): PreviewUseCases = apply { plannerPreview.timePasses(minutes) }

/** The clock a scripted screen reads, stopped where the preview stands unless [timePasses] moved it. */
internal val ScriptClock: Instant
    get() = PreviewArrivals.SAMPLE_SYNC
