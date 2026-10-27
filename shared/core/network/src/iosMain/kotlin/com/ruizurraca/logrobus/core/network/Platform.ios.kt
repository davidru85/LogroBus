package com.ruizurraca.logrobus.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData
import platform.UIKit.UIDevice

actual fun clientIdentity(appVersion: String): ClientIdentity =
    ClientIdentity(appVersion, platform = "iOS", osVersion = UIDevice.currentDevice.systemVersion)

// NSURLSession caches responses and stores cookies by default. A cached response would carry stale arrivals and a
// stale Date (spec §8.6), and spec §8.7 sends no cookies.
internal actual fun platformEngine(): HttpClientEngine =
    Darwin.create {
        configureSession {
            setURLCache(null)
            setRequestCachePolicy(NSURLRequestReloadIgnoringLocalCacheData)
            setHTTPCookieStorage(null)
            setHTTPShouldSetCookies(false)
        }
    }
