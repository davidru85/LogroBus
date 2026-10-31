package com.ruizurraca.logrobus.transit.data

import kotlin.test.Test
import kotlin.test.assertEquals

/** Spec §8.5: an unchanged payload is recognised by the SHA-256 of its body. */
class ContentHashTest {
    @Test
    fun lb008_theContentHashIsTheSha256OfTheBody() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", sha256Hex(""))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256Hex("abc"))
        // More than one 64-byte block.
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            sha256Hex("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"),
        )
    }

    @Test
    fun lb008_theContentHashReadsTheBodyAsUtf8() {
        assertEquals("c08e43ac4a131e9760623b82648824b68a9d94f524a7fa477309f3d75c823927", sha256Hex("Logroño"))
    }
}
