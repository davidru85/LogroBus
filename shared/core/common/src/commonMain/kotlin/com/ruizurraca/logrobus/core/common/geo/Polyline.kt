package com.ruizurraca.logrobus.core.common.geo

import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Google's Encoded Polyline Algorithm Format, at any precision. Pattern shapes are stored at precision 6 (spec §8.8),
 * and other sources often use precision 5.
 */
object Polyline {
    private const val CHUNK_BITS = 5
    private const val CHUNK_MASK = 0x1f
    private const val CONTINUATION_BIT = 0x20
    private const val ASCII_OFFSET = 63
    private const val DECIMAL_BASE = 10.0

    fun encode(
        points: List<GeoPoint>,
        precision: Int,
    ): String {
        val factor = factor(precision)
        val encoded = StringBuilder()
        var previousLat = 0L
        var previousLng = 0L
        points.forEach { point ->
            val lat = (point.lat * factor).roundToLong()
            val lng = (point.lng * factor).roundToLong()
            encoded.appendValue(lat - previousLat)
            encoded.appendValue(lng - previousLng)
            previousLat = lat
            previousLng = lng
        }
        return encoded.toString()
    }

    fun decode(
        encoded: String,
        precision: Int,
    ): List<GeoPoint> {
        val factor = factor(precision)
        val points = mutableListOf<GeoPoint>()
        var index = 0
        var lat = 0L
        var lng = 0L
        while (index < encoded.length) {
            val (dLat, afterLat) = encoded.readValue(index)
            val (dLng, afterLng) = encoded.readValue(afterLat)
            lat += dLat
            lng += dLng
            points += GeoPoint(lat / factor, lng / factor)
            index = afterLng
        }
        return points
    }

    private fun factor(precision: Int): Double {
        require(precision >= 0) { "precision must not be negative: $precision" }
        return DECIMAL_BASE.pow(precision)
    }

    /** Zig-zag encodes [value] (so small negatives stay short), then writes it in 5-bit chunks, lowest first. */
    private fun StringBuilder.appendValue(value: Long) {
        var remaining = if (value < 0) (value shl 1).inv() else value shl 1
        while (remaining >= CONTINUATION_BIT) {
            append(((CONTINUATION_BIT or (remaining and CHUNK_MASK.toLong()).toInt()) + ASCII_OFFSET).toChar())
            remaining = remaining shr CHUNK_BITS
        }
        append((remaining + ASCII_OFFSET).toInt().toChar())
    }

    /** Reads one value starting at [start]; returns it with the index just after it. */
    private fun String.readValue(start: Int): Pair<Long, Int> {
        var result = 0L
        var shift = 0
        var index = start
        do {
            val chunk = (this[index++].code - ASCII_OFFSET).toLong()
            result = result or ((chunk and CHUNK_MASK.toLong()) shl shift)
            shift += CHUNK_BITS
        } while (chunk >= CONTINUATION_BIT)
        val value = if (result and 1L != 0L) (result shr 1).inv() else result shr 1
        return value to index
    }
}
