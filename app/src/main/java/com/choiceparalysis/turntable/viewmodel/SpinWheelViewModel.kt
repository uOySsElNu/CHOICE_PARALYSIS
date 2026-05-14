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

    private val _isSpinning = MutableStateFlow(false)
    val isSpinning: StateFlow<Boolean> = _isSpinning.asStateFlow()

    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result.asStateFlow()

    private val _rotationDegrees = MutableStateFlow(0f)
    val rotationDegrees: StateFlow<Float> = _rotationDegrees.asStateFlow()

    private val _optionsEditorOpen = MutableStateFlow(false)
    val optionsEditorOpen: StateFlow<Boolean> = _optionsEditorOpen.asStateFlow()

    private val _options = MutableStateFlow(listOf("Yes", "No"))
    val options: StateFlow<List<String>> = _options.asStateFlow()

    // Settings flows
    val dynamicColorEnabled: StateFlow<Boolean> = settingsRepository.dynamicColorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val selectedPresetName: StateFlow<String> = settingsRepository.selectedPreset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "CLASSIC_RAINBOW")

    val customColors: StateFlow<List<Int>> = settingsRepository.customColors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        SharingStarted.WhileSubscribed(5000),
        TimeBasedColorGenerator.generateColorScheme(2)
    )

    init {
        // Load persisted options
        viewModelScope.launch {
            settingsRepository.currentOptions.collect { loaded ->
                _options.value = loaded
            }
        }
        // Refresh dynamic colors every minute
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                if (dynamicColorEnabled.value) {
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
        custom.isNotEmpty() -> WheelColorScheme(
            segmentColors = custom.take(optionCount).map { Color(it) },
            borderColor = Color.White,
            textColor = Color.White,
            indicatorColor = Color(custom.firstOrNull() ?: 0xFF6200EA.toInt()),
        )
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

    fun updateOptions(newOptions: List<String>) {
        _options.value = newOptions
        viewModelScope.launch { settingsRepository.setCurrentOptions(newOptions) }
    }

    fun addOption() {
        val current = _options.value
        if (current.size < 10) {
            updateOptions(current + "选项${current.size + 1}")
        }
    }

    fun removeOption(index: Int) {
        val current = _options.value.toMutableList()
        if (current.size > 2 && index in current.indices) {
            current.removeAt(index)
            updateOptions(current)
        }
    }

    fun updateOptionName(index: Int, name: String) {
        val current = _options.value.toMutableList()
        if (index in current.indices) {
            current[index] = name
            updateOptions(current)
        }
    }

    fun saveOptionGroup(name: String) {
        viewModelScope.launch {
            settingsRepository.saveOptionGroup(
                OptionGroup(name = name, options = _options.value)
            )
        }
    }

    fun loadOptionGroup(group: OptionGroup) {
        updateOptions(group.options)
    }

    fun deleteOptionGroup(id: String) {
        viewModelScope.launch { settingsRepository.deleteOptionGroup(id) }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        settingsRepository.setDynamicColorEnabled(enabled)
    }

    suspend fun setSelectedPreset(preset: String) {
        settingsRepository.setSelectedPreset(preset)
    }

    suspend fun setCustomColors(colors: List<Int>) {
        settingsRepository.setCustomColors(colors)
    }

    fun spin() {
        if (_isSpinning.value || _options.value.isEmpty()) return

        _isSpinning.value = true
        _result.value = null

        val randomDegrees = (720..1440).random().toFloat() + (0..360).random().toFloat()
        _rotationDegrees.value = _rotationDegrees.value + randomDegrees

        viewModelScope.launch {
            delay(3000)
            val currentOptions = _options.value
            if (currentOptions.isNotEmpty()) {
                val segmentAngle = 360f / currentOptions.size
                val normalizedRotation = ((_rotationDegrees.value % 360f) + 360f) % 360f
                val selectedIndex = (normalizedRotation / segmentAngle).toInt() % currentOptions.size

                _result.value = currentOptions[selectedIndex]
                _isSpinning.value = false

                historyRepository.addEntry(
                    HistoryEntry(
                        method = DecisionMethod.SPIN_WHEEL,
                        options = currentOptions,
                        result = currentOptions[selectedIndex],
                    )
                )
            } else {
                _isSpinning.value = false
            }
        }
    }

    fun clearResult() {
        _result.value = null
    }
}
