package com.ruizurraca.logrobus.core.network

import android.os.Build
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun clientIdentity(appVersion: String): ClientIdentity =
    ClientIdentity(appVersion, platform = "Android", osVersion = Build.VERSION.RELEASE)

// OkHttp keeps no cache and no cookies unless configured to.
internal actual fun platformEngine(): HttpClientEngine = OkHttp.create()
