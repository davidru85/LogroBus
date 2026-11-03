package com.ruizurraca.logrobus.android.feature.settings

import androidx.lifecycle.ViewModel
import com.ruizurraca.logrobus.feature.settings.domain.ClearCache
import com.ruizurraca.logrobus.feature.settings.domain.ObserveLastSyncs
import com.ruizurraca.logrobus.feature.settings.domain.ObservePreferences
import com.ruizurraca.logrobus.feature.settings.domain.SetEtaFormat
import com.ruizurraca.logrobus.feature.settings.domain.SetTheme
import com.ruizurraca.logrobus.feature.settings.presentation.CacheMessage
import com.ruizurraca.logrobus.feature.settings.presentation.SettingsEvent
import com.ruizurraca.logrobus.feature.settings.presentation.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Settings (LB-403). It orchestrates only: the shared use cases and mapper build the state (ADR-0007). */
@Suppress("UnusedPrivateProperty", "unused", "LongParameterList")
class SettingsViewModel(
    private val observePreferences: ObservePreferences,
    private val observeLastSyncs: ObserveLastSyncs,
    private val setTheme: SetTheme,
    private val setEtaFormat: SetEtaFormat,
    private val clearCache: ClearCache,
    private val languages: AppLanguages,
) : ViewModel() {
    val state: StateFlow<SettingsUiState> = MutableStateFlow(SettingsUiState.Loading)

    /** What a "Clear cache" leaves to say, until the screen says it has shown it. */
    val cacheMessage: StateFlow<CacheMessage?> = MutableStateFlow(null)

    fun onEvent(event: SettingsEvent) = Unit
}
