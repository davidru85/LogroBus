package com.ruizurraca.logrobus.transit.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

// CommonCrypto ships with iOS, so no dependency is needed.
@OptIn(ExperimentalForeignApi::class)
internal actual fun sha256(bytes: ByteArray): ByteArray {
    val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)
    bytes.usePinned { input ->
        digest.usePinned { output ->
            // An empty array has no element to point at; CommonCrypto takes null for zero bytes.
            val data = if (bytes.isEmpty()) null else input.addressOf(0)
            CC_SHA256(data, bytes.size.convert(), output.addressOf(0).reinterpret())
        }
    }
    return digest
}
