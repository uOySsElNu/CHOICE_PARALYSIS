package com.choiceparalysis.turntable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.audio.AudioHapticManager
import com.choiceparalysis.turntable.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val audioHapticManager: AudioHapticManager
) : ViewModel() {

    fun playHapticTick() = audioHapticManager.playHapticTick()

    val followSystemTheme = settingsRepository.followSystemTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val darkMode = settingsRepository.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val soundEnabled = settingsRepository.soundEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val hapticEnabled = settingsRepository.hapticEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val appLocale = settingsRepository.appLocale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    fun setFollowSystemTheme(value: Boolean) {
        viewModelScope.launch { settingsRepository.setFollowSystemTheme(value) }
    }

    fun setDarkMode(value: Boolean) {
        viewModelScope.launch { settingsRepository.setDarkMode(value) }
    }

    fun setSoundEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.setSoundEnabled(value) }
    }

    fun setHapticEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.setHapticEnabled(value) }
    }

    fun setAppLocale(value: String) {
        viewModelScope.launch { settingsRepository.setAppLocale(value) }
    }
}
