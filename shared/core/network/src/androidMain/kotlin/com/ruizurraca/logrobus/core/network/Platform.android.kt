package com.ruizurraca.logrobus.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun clientIdentity(appVersion: String): ClientIdentity = ClientIdentity(appVersion, "", "")

internal actual fun platformEngine(): HttpClientEngine = OkHttp.create()
