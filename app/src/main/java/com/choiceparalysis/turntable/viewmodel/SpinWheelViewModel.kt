package com.choiceparalysis.turntable.viewmodel

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.data.model.DecisionMethod
import com.choiceparalysis.turntable.data.model.HistoryEntry
import com.choiceparalysis.turntable.data.model.OptionGroup
import com.choiceparalysis.turntable.data.repository.HistoryRepository
import com.choiceparalysis.turntable.data.repository.SettingsRepository
import com.choiceparalysis.turntable.ui.home.TimeBasedColorGenerator
import com.choiceparalysis.turntable.ui.home.WheelColorScheme
import com.choiceparalysis.turntable.ui.home.WheelDesign
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SpinWheelViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val historyRepository = HistoryRepository(application)

    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result.asStateFlow()

    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

    private val _optionsEditorOpen = MutableStateFlow(false)
    val optionsEditorOpen: StateFlow<Boolean> = _optionsEditorOpen.asStateFlow()

    private val _options = MutableStateFlow(listOf("Yes", "No"))
    val options: StateFlow<List<String>> = _options.asStateFlow()

    private val _weights = MutableStateFlow(listOf(1, 1))
    val weights: StateFlow<List<Int>> = _weights.asStateFlow()

    // Settings flows
    private val _dynamicColorEnabled = MutableStateFlow(true)
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    val selectedPresetName: StateFlow<String> = settingsRepository.selectedPreset
        .stateIn(viewModelScope, SharingStarted.Eagerly, "CLASSIC_RAINBOW")

    private val _customColors = MutableStateFlow<List<Int>>(emptyList())
    val customColors: StateFlow<List<Int>> = _customColors.asStateFlow()

    val optionGroups: StateFlow<List<OptionGroup>> = settingsRepository.optionGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Computed color scheme based on settings + time
    val colorScheme: StateFlow<WheelColorScheme> = combine(
        dynamicColorEnabled,
        selectedPresetName,
        customColors,
        _options,
    ) { dynamic, preset, custom, opts ->
        buildColorScheme(dynamic, preset, custom, opts.size)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        TimeBasedColorGenerator.generateColorScheme(2)
    )

    init {
        viewModelScope.launch {
            settingsRepository.currentOptions.collect { loaded ->
                _options.value = loaded
            }
        }
        viewModelScope.launch {
            settingsRepository.currentWeights.collect { loaded ->
                _weights.value = loaded
            }
        }
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
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                if (_dynamicColorEnabled.value) {
                    _options.value = _options.value.toList()
                }
            }
        }
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

    fun toggleOptionsEditor() {
        _optionsEditorOpen.value = !_optionsEditorOpen.value
    }

    fun setAnimating(value: Boolean) {
        _isAnimating.value = value
    }

    fun updateOptions(newOptions: List<String>) {
        _options.value = newOptions
        viewModelScope.launch { settingsRepository.setCurrentOptions(newOptions) }
    }

    fun addOption() {
        val current = _options.value
        if (current.size < 10) {
            updateOptions(current + "选项${current.size + 1}")
            _weights.value = _weights.value + 1
            viewModelScope.launch { settingsRepository.setCurrentWeights(_weights.value) }
        }
    }

    fun removeOption(index: Int) {
        val current = _options.value.toMutableList()
        if (current.size > 2 && index in current.indices) {
            current.removeAt(index)
            updateOptions(current)
            val w = _weights.value.toMutableList()
            if (index in w.indices && w.size > 2) {
                w.removeAt(index)
                _weights.value = w
                viewModelScope.launch { settingsRepository.setCurrentWeights(w) }
            }
        }
    }

    fun updateOptionName(index: Int, name: String) {
        val current = _options.value.toMutableList()
        if (index in current.indices) {
            current[index] = name
            updateOptions(current)
        }
    }

    fun updateWeight(index: Int, weight: Int) {
        val current = _weights.value.toMutableList()
        while (current.size <= index) current.add(1)
        current[index] = weight.coerceIn(1, 10)
        _weights.value = current
        viewModelScope.launch { settingsRepository.setCurrentWeights(current) }
    }

    fun saveOptionGroup(name: String) {
        viewModelScope.launch {
            settingsRepository.saveOptionGroup(
                OptionGroup(name = name, options = _options.value, weights = _weights.value)
            )
        }
    }

    fun loadOptionGroup(group: OptionGroup) {
        updateOptions(group.options)
        val w = group.weights.ifEmpty { group.options.map { 1 } }
        _weights.value = w
        viewModelScope.launch { settingsRepository.setCurrentWeights(w) }
    }

    fun deleteOptionGroup(id: String) {
        viewModelScope.launch { settingsRepository.deleteOptionGroup(id) }
    }

    fun onSpinResult(selectedOption: String) {
        _result.value = selectedOption
        viewModelScope.launch {
            historyRepository.addEntry(
                HistoryEntry(
                    method = DecisionMethod.SPIN_WHEEL,
                    options = _options.value,
                    result = selectedOption,
                )
            )
        }
    }

    fun clearResult() {
        _result.value = null
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
}

private fun Color.toArgb(): Int {
    val a = (alpha * 255).roundToInt()
    val r = (red * 255).roundToInt()
    val g = (green * 255).roundToInt()
    val b = (blue * 255).roundToInt()
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}
