package com.ruizurraca.logrobus.core.database

/**
 * The values of `favourite.type`. They are stored on devices, so they never change. The favourites module writes them,
 * and the static sync reads [STOP] to count the syncs a saved stop has been missing from (FR-FAV-04). A saved stop's
 * `ref_id` is its normalised id in decimal, with no leading zeros (DD-01): the sync compares it with `stop.id` as text.
 */
object FavouriteTypes {
    const val STOP = "stop"
}
