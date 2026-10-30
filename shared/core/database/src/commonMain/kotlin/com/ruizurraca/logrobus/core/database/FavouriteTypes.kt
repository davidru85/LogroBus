package com.ruizurraca.logrobus.core.database

/**
 * The values of `favourite.type`. They are stored on devices, so they never change. The favourites module writes them,
 * and the static sync reads [STOP] to count the syncs a saved stop has been missing from (FR-FAV-04).
 */
object FavouriteTypes {
    const val STOP = "stop"
}
