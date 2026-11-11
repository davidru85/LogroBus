package com.ruizurraca.logrobus.android.feature.nearme

import com.ruizurraca.logrobus.feature.nearme.presentation.NearMeUiState
import com.ruizurraca.logrobus.feature.nearme.presentation.NearYouUi
import com.ruizurraca.logrobus.feature.nearme.presentation.NearbyStopRowUi

/** Near Me's states for previews and screenshot tests. */
internal object NearMePreviewData {
    val turnOnLocation = NearMeUiState(NearYouUi.TurnOnLocation)
    val locationOn = NearMeUiState(NearYouUi.LocationOn())

    /** LB-203: the section with a walk answered, and one stop the walk has not reached yet. */
    val nearby =
        NearMeUiState(
            NearYouUi.LocationOn(
                stops =
                    listOf(
                        NearbyStopRowUi(
                            stopId = 38,
                            name = "Gonzalo de Berceo",
                            distanceMeters = 120,
                            walkingMinutes = 2,
                        ),
                        NearbyStopRowUi(stopId = 61, name = "Varea", distanceMeters = 400, walkingMinutes = 5),
                        NearbyStopRowUi(stopId = 92, name = "El Cubo", distanceMeters = 620),
                    ),
                isRefined = true,
            ),
        )
}
