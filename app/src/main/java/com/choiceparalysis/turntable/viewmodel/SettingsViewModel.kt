package com.choiceparalysis.turntable.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.choiceparalysis.turntable.data.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)

    val dynamicColorEnabled: Flow<Boolean> = repository.dynamicColorEnabled
    val selectedPreset: Flow<String> = repository.selectedPreset
    val customColors: Flow<List<Int>> = repository.customColors
    val currentOptions: Flow<List<String>> = repository.currentOptions

    suspend fun setDynamicColorEnabled(enabled: Boolean) =
        repository.setDynamicColorEnabled(enabled)

    suspend fun setSelectedPreset(preset: String) =
        repository.setSelectedPreset(preset)

    suspend fun setCustomColors(colors: List<Int>) =
        repository.setCustomColors(colors)
}
