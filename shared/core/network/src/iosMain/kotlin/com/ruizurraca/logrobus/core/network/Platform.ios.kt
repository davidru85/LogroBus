package com.ruizurraca.logrobus.core.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData

actual fun clientIdentity(appVersion: String): ClientIdentity =
    ClientIdentity(appVersion, platform = "iOS", osVersion = osVersion())

/**
 * The OS version as `UIDevice.systemVersion` writes it: "26.0", or "18.6.2" when there is a patch. Read from
 * NSProcessInfo, which any thread may use, unlike UIDevice, which UIKit confines to the main thread.
 */
@OptIn(ExperimentalForeignApi::class) // useContents on the NSOperatingSystemVersion struct.
private fun osVersion(): String =
    NSProcessInfo.processInfo.operatingSystemVersion.useContents {
        val parts = listOf(majorVersion, minorVersion, patchVersion)
        (if (patchVersion == 0L) parts.dropLast(1) else parts).joinToString(".")
    }

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
