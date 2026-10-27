package com.ruizurraca.logrobus.core.network

import io.ktor.client.engine.HttpClientEngine

/** This device's [ClientIdentity]: its platform name and OS version, with the app's [appVersion] (spec §8.7). */
expect fun clientIdentity(appVersion: String): ClientIdentity

/** The HTTP engine of this platform (ADR-0012). */
internal expect fun platformEngine(): HttpClientEngine
