package com.choiceparalysis.turntable.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.data.repository.SettingsRepository
import com.choiceparalysis.turntable.ui.home.TimeBasedColorGenerator
import com.choiceparalysis.turntable.ui.home.WheelColorScheme
import com.choiceparalysis.turntable.ui.home.WheelDesign
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class ColorSchemeVM @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _dynamicColorEnabled = MutableStateFlow(true)
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    val selectedPresetName: StateFlow<String> = settingsRepository.selectedPreset
        .stateIn(viewModelScope, SharingStarted.Eagerly, "CLASSIC_RAINBOW")

    private val _customColors = MutableStateFlow<List<Int>>(emptyList())
    val customColors: StateFlow<List<Int>> = _customColors.asStateFlow()

    // Options count is fed from OptionsVM via setOptionsCount()
    private val _optionsCount = MutableStateFlow(2)

    // Computed color scheme based on settings + time
    val colorScheme: StateFlow<WheelColorScheme> = combine(
        _dynamicColorEnabled,
        selectedPresetName,
        _customColors,
        _optionsCount,
    ) { dynamic, preset, custom, count ->
        buildColorScheme(dynamic, preset, custom, count)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        TimeBasedColorGenerator.generateColorScheme(2)
    )

    init {
        viewModelScope.launch {
            settingsRepository.dynamicColorEnabled.collect { enabled ->
                _dynamicColorEnabled.value = enabled
            }
        }
        viewModelScope.launch {
            settingsRepository.customColors.collect { colors ->
                _customColors.value = colors
            }
        }
        // 60-second refresh for dynamic color mode
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                if (_dynamicColorEnabled.value) {
                    _optionsCount.value = _optionsCount.value
                }
            }
        }
    }

    /** Called by the screen when options list changes, so colorScheme re-computes. */
    fun setOptionsCount(count: Int) {
        _optionsCount.value = count
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        _dynamicColorEnabled.value = enabled
        settingsRepository.setDynamicColorEnabled(enabled)
    }

    suspend fun setSelectedPreset(preset: String) {
        settingsRepository.setSelectedPreset(preset)
    }

    suspend fun setCustomColors(colors: List<Int>) {
        _customColors.value = colors
        settingsRepository.setCustomColors(colors)
    }

    fun updateOptionColor(index: Int, color: Int) {
        val current = _customColors.value.toMutableList()
        val design = runCatching { WheelDesign.valueOf(selectedPresetName.value) }.getOrNull()
            ?: WheelDesign.CLASSIC_RAINBOW
        while (current.size <= index) {
            current.add(design.getColorForIndex(current.size).toArgb())
        }
        current[index] = color
        _customColors.value = current
        viewModelScope.launch { settingsRepository.setCustomColors(current) }
    }

    private fun buildColorScheme(
        dynamic: Boolean,
        preset: String,
        custom: List<Int>,
        optionCount: Int,
    ): WheelColorScheme = when {
        dynamic -> TimeBasedColorGenerator.generateColorScheme(optionCount)
        custom.isNotEmpty() -> {
            val design = runCatching { WheelDesign.valueOf(preset) }.getOrNull()
                ?: WheelDesign.CLASSIC_RAINBOW
            WheelColorScheme(
                segmentColors = custom.take(optionCount).map { Color(it) },
                borderColor = Color.White,
                textColor = Color.White,
                indicatorColor = design.indicatorColor,
            )
        }
        else -> {
            val design = runCatching { WheelDesign.valueOf(preset) }.getOrNull()
                ?: WheelDesign.CLASSIC_RAINBOW
            WheelColorScheme(
                segmentColors = design.colors.take(optionCount.coerceAtMost(10)),
                borderColor = design.borderColor,
                textColor = design.textColor,
                indicatorColor = design.indicatorColor,
            )
        }
    }
}

private fun Color.toArgb(): Int {
    val a = (alpha * 255).roundToInt()
    val r = (red * 255).roundToInt()
    val g = (green * 255).roundToInt()
    val b = (blue * 255).roundToInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}
