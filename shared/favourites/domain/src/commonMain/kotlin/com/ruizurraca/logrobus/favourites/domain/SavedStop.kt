package com.ruizurraca.logrobus.favourites.domain

import com.ruizurraca.logrobus.core.common.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * A stop the user saved (FR-STOP-08, FR-HOME-02), with the alias they gave it and its place in their order.
 *
 * [alias] is what "My stops" shows instead of the stop's name, and is `null` when the user kept the name. It never
 * exceeds [SavedStopAlias.MAX_LENGTH] characters.
 */
data class SavedStop(
    val stopId: Int,
    val alias: String?,
    val position: Int,
)

/**
 * FR-STOP-08: the alias a user types for a saved stop, cut to what the screen allows.
 *
 * The cut counts code points, not UTF-16 units, so a name written in a script whose glyphs need two units each is
 * never cut in half; and a blank alias is no alias, which keeps the stop's own name.
 */
object SavedStopAlias {
    /** The longest alias the field takes, in characters. */
    const val MAX_LENGTH = 30

    /** [text] as an alias, or `null` when it holds nothing but spaces. */
    fun of(text: String): String? = truncated(text.trim(), MAX_LENGTH).ifEmpty { null }

    /**
     * [text] cut to [limit] characters. The cut counts code points, never UTF-16 units, so a glyph that needs two units
     * (an emoji, a letter of some scripts) is never split into a lone surrogate.
     */
    private fun truncated(
        text: String,
        limit: Int,
    ): String {
        // A surrogate pair is one glyph: walk the string by code point rather than by char.
        var taken = 0
        var index = 0
        while (index < text.length && taken < limit) {
            index += text.codePointWidthAt(index)
            taken++
        }
        return text.substring(0, index)
    }

    /** How many UTF-16 units the code point at [index] takes: two for a surrogate pair. */
    @Suppress("MagicNumber") // The surrogate range of UTF-16, which Kotlin's String is.
    private fun String.codePointWidthAt(index: Int): Int {
        val unit = this[index].code
        val isHighSurrogate = unit in HIGH_SURROGATE_START..HIGH_SURROGATE_END
        val next = getOrNull(index + 1)?.code
        val hasLow = next != null && next in LOW_SURROGATE_START..LOW_SURROGATE_END
        return if (isHighSurrogate && hasLow) 2 else 1
    }

    // The two halves of UTF-16's surrogate range, as code units.
    private const val HIGH_SURROGATE_START = 0xD800
    private const val HIGH_SURROGATE_END = 0xDBFF
    private const val LOW_SURROGATE_START = 0xDC00
    private const val LOW_SURROGATE_END = 0xDFFF
}

/**
 * The user's saved stops (FR-STOP-08, FR-FAV-01, FR-FAV-02, FR-FAV-05), kept on the device.
 *
 * They are stored by stop id, so a stop that leaves the catalogue stays saved, with no row, until it comes back or the
 * user removes it; it still counts towards [FAVOURITES_PER_TYPE]. Failures are values, never exceptions (AGENTS.md §5).
 */
interface SavedStopRepository {
    /** The saved stops in the user's order (FR-FAV-02), each with its alias and its place. */
    fun observeSavedStops(): Flow<AppResult<List<SavedStop>>>

    /**
     * Saves [stopId] last, under [alias] (FR-STOP-08). A stop already saved keeps its alias and its place; with
     * [FAVOURITES_PER_TYPE] stops saved, another one is not saved (FR-FAV-05).
     */
    suspend fun add(
        stopId: Int,
        alias: String?,
    ): AppResult<FavouriteAddition>

    /** FR-STOP-08: gives a saved stop a new alias, keeping its place. An unsaved stop is left alone. */
    suspend fun rename(
        stopId: Int,
        alias: String?,
    ): AppResult<Unit>

    suspend fun remove(stopId: Int): AppResult<Unit>

    /** FR-FAV-02: puts the saved stops in the order of [stopIds]; one it leaves out keeps its order after them. */
    suspend fun reorder(stopIds: List<Int>): AppResult<Unit>
}
