package com.choiceparalysis.turntable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.data.model.OptionGroup
import com.choiceparalysis.turntable.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OptionsVM @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _optionsEditorOpen = MutableStateFlow(false)
    val optionsEditorOpen: StateFlow<Boolean> = _optionsEditorOpen.asStateFlow()

    private val _options = MutableStateFlow(listOf("Yes", "No"))
    val options: StateFlow<List<String>> = _options.asStateFlow()

    private val _weights = MutableStateFlow(listOf(1, 1))
    val weights: StateFlow<List<Int>> = _weights.asStateFlow()

    val optionGroups: StateFlow<List<OptionGroup>> = settingsRepository.optionGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
}
