package com.ruizurraca.logrobus.android.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruizurraca.logrobus.feature.settings.domain.ClearCache
import com.ruizurraca.logrobus.feature.settings.domain.ObserveLastSyncs
import com.ruizurraca.logrobus.feature.settings.domain.ObservePreferences
import com.ruizurraca.logrobus.feature.settings.domain.SetEtaFormat
import com.ruizurraca.logrobus.feature.settings.domain.SetTheme
import com.ruizurraca.logrobus.feature.settings.presentation.CacheMessage
import com.ruizurraca.logrobus.feature.settings.presentation.SettingsEvent
import com.ruizurraca.logrobus.feature.settings.presentation.SettingsUiMapper
import com.ruizurraca.logrobus.feature.settings.presentation.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Settings (LB-403). It orchestrates only: the shared use cases and mapper build the state (ADR-0007). */
class SettingsViewModel(
    observePreferences: ObservePreferences,
    observeLastSyncs: ObserveLastSyncs,
    private val setTheme: SetTheme,
    private val setEtaFormat: SetEtaFormat,
    private val clearCache: ClearCache,
    private val languages: AppLanguages,
) : ViewModel() {
    val state: StateFlow<SettingsUiState> =
        combine(observePreferences(), observeLastSyncs(), languages.current) { preferences, syncs, language ->
            SettingsUiMapper.state(preferences, syncs, language)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_DELAY_MILLIS), SettingsUiState.Loading)

    private val message = MutableStateFlow<CacheMessage?>(null)

    /**
     * What a "Clear cache" leaves to say, until the screen says it has shown it: state rather than an effect, so a
     * message that outlives a configuration change is shown once, as the favourite limit's is (LB-110).
     */
    val cacheMessage: StateFlow<CacheMessage?> = message.asStateFlow()

    fun onEvent(event: SettingsEvent) {
        when (event) {
            // Android recreates the activity in the new language; this ViewModel survives it.
            is SettingsEvent.SetLanguage -> {
                languages.set(event.language)
            }

            // The setting shows through the store's flow. One that cannot be saved, which the store has logged, leaves
            // Settings as it was.
            is SettingsEvent.SetTheme -> {
                viewModelScope.launch { setTheme(event.theme) }
            }

            is SettingsEvent.SetEtaFormat -> {
                viewModelScope.launch { setEtaFormat(event.format) }
            }

            SettingsEvent.ClearCache -> {
                viewModelScope.launch { message.value = SettingsUiMapper.cacheMessage(clearCache()) }
            }

            SettingsEvent.CacheMessageShown -> {
                message.value = null
            }
        }
    }

    private companion object {
        // The reads outlive a configuration change, a new language's included, and stop this long after the screen
        // leaves.
        const val STOP_DELAY_MILLIS = 5_000L
    }
}
