package com.ruizurraca.logrobus.core.common.geo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

/** LB-004: encoded polylines round-trip at precision 5 and at precision 6, which pattern shapes use (spec §8.8). */
class PolylineTest {
    // The example in Google's "Encoded Polyline Algorithm Format" documentation, at precision 5.
    private val googlePoints = listOf(GeoPoint(38.5, -120.2), GeoPoint(40.7, -120.95), GeoPoint(43.252, -126.453))
    private val googleEncoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@"

    // A path through Logroño with six decimals, the precision of stored pattern shapes.
    private val logronoPath =
        listOf(GeoPoint(42.465123, -2.445678), GeoPoint(42.466001, -2.444321), GeoPoint(42.463987, -2.441002))

    @Test
    fun lb004_polylineEncodesAndDecodesGooglesExampleAtPrecision5() {
        assertEquals(googleEncoded, Polyline.encode(googlePoints, precision = 5))
        assertEquals(googlePoints, Polyline.decode(googleEncoded, precision = 5))
    }

    @Test
    fun lb004_polylineRoundTripsAtPrecision6() {
        val encoded = Polyline.encode(logronoPath, precision = 6)

        assertEquals(logronoPath, Polyline.decode(encoded, precision = 6))
    }

    @Test
    fun lb004_precision5RoundsTheSixthDecimal() {
        val roundTripped = Polyline.decode(Polyline.encode(logronoPath, precision = 5), precision = 5)

        assertNotEquals(logronoPath, roundTripped)
        assertEquals(GeoPoint(42.46512, -2.44568), roundTripped.first())
    }

    @Test
    fun lb004_aTruncatedPolylineIsRejected() {
        // Cut inside a value, and between a latitude and its longitude.
        listOf("_p~iF~ps|U_", "_p~iF~ps|U_ulL").forEach { truncated ->
            assertFailsWith<IllegalArgumentException>(truncated) { Polyline.decode(truncated, precision = 5) }
        }
    }

    @Test
    fun lb004_charactersOutsideThePolylineAlphabetAreRejected() {
        assertFailsWith<IllegalArgumentException> { Polyline.decode("_p~iF ps|U", precision = 5) }
    }

    @Test
    fun lb004_precisionMustBeBetween0And10() {
        assertFailsWith<IllegalArgumentException> { Polyline.encode(logronoPath, precision = -1) }
        assertFailsWith<IllegalArgumentException> { Polyline.encode(logronoPath, precision = 11) }
        assertFailsWith<IllegalArgumentException> { Polyline.decode(googleEncoded, precision = 11) }
    }

    @Test
    fun lb004_anEmptyPathIsAnEmptyString() {
        assertEquals("", Polyline.encode(emptyList(), precision = 6))
        assertEquals(emptyList(), Polyline.decode("", precision = 6))
    }
}
