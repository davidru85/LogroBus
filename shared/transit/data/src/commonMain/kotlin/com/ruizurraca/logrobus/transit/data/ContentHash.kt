package com.ruizurraca.logrobus.transit.data

/** Spec §8.5: the content hash of a static payload, the SHA-256 of its UTF-8 bytes in lower-case hex. */
internal expect fun sha256Hex(text: String): String
