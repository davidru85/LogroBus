package com.ruizurraca.logrobus.android.feature.nearme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruizurraca.logrobus.core.common.AppResult
import com.ruizurraca.logrobus.feature.nearme.domain.ObserveMyStops
import com.ruizurraca.logrobus.feature.nearme.domain.ObserveNearYou
import com.ruizurraca.logrobus.feature.nearme.presentation.MyStopsUi
import com.ruizurraca.logrobus.feature.nearme.presentation.NearMeEvent
import com.ruizurraca.logrobus.feature.nearme.presentation.NearMeUiMapper
import com.ruizurraca.logrobus.feature.nearme.presentation.NearMeUiState
import com.ruizurraca.logrobus.feature.nearme.presentation.NearYouUi
import com.ruizurraca.logrobus.feature.nearme.presentation.SavedStopsUiMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Near Me (LB-201). It orchestrates only: the shared use case and mapper build the state (ADR-0007). The location
 * updates run while the screen collects [state] (FR-LOC-03).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NearMeViewModel(
    observeNearYou: ObserveNearYou,
    observeMyStops: ObserveMyStops,
) : ViewModel() {
    // Each check bumps it, which reads location access again.
    private val checks = MutableStateFlow(0)

    val state: StateFlow<NearMeUiState> =
        combine(
            checks.flatMapLatest { observeNearYou() },
            // FR-HOME-02: "My stops" is its own collection, so a save from another screen shows here at once.
            observeMyStops(),
        ) { section, saved ->
            NearMeUiMapper.state(section.access, section.nearby).copy(
                myStops =
                    when (saved) {
                        is AppResult.Failure -> {
                            MyStopsUi()
                        }

                        is AppResult.Success -> {
                            SavedStopsUiMapper.myStops(
                                saved = saved.value.stops,
                                names = saved.value.names,
                                arrivals = saved.value.arrivals,
                                failures = saved.value.failed,
                            )
                        }
                    },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_DELAY_MILLIS), NearMeUiState())

    fun onEvent(event: NearMeEvent) {
        when (event) {
            // Only without location: until access is known, the read under way gets the latest permission, and
            // while location is on, its flow ends by itself if location goes, so reading again would only restart
            // the updates.
            NearMeEvent.PermissionMayHaveChanged -> {
                if (state.value.nearYou == NearYouUi.TurnOnLocation) checks.update { it + 1 }
            }
        }
    }

    private companion object {
        // The updates outlive a configuration change, and stop this long after the screen leaves.
        const val STOP_DELAY_MILLIS = 5_000L
    }
}
