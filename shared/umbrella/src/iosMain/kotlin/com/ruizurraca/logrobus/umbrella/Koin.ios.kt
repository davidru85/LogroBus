package com.ruizurraca.logrobus.umbrella

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

/** Starts the dependency graph of the iOS app (ADR-0014). */
fun initKoin(): KoinApplication = startKoin { }
