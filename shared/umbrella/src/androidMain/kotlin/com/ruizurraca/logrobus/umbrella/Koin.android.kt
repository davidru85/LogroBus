package com.ruizurraca.logrobus.umbrella

import android.content.Context
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication

/** Starts the dependency graph of the Android app (ADR-0014). */
fun initKoin(context: Context): KoinApplication = koinApplication { }
