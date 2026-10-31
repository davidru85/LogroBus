package com.ruizurraca.logrobus.transit.data

/** Spec §8.5: the content hash of a static payload, the SHA-256 of its UTF-8 bytes in lower-case hex. */
internal fun sha256Hex(text: String): String = sha256(text.encodeToByteArray()).toHexString()

/** SHA-256 from the platform, which Kotlin's common library lacks. */
internal expect fun sha256(bytes: ByteArray): ByteArray
