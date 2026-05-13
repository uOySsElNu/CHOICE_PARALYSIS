package com.choiceparalysis.turntable.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.data.model.DecisionMethod
import com.choiceparalysis.turntable.data.model.HistoryEntry
import com.choiceparalysis.turntable.data.model.OptionList
import com.choiceparalysis.turntable.data.repository.HistoryRepository
import com.choiceparalysis.turntable.data.repository.OptionListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SpinWheelViewModel(application: Application) : AndroidViewModel(application) {
    private val optionListRepository = OptionListRepository(application)
    private val historyRepository = HistoryRepository(application)

    private val _options = MutableStateFlow<List<String>>(listOf("选项1", "选项2", "选项3", "选项4"))
    val options: StateFlow<List<String>> = _options.asStateFlow()

    private val _isSpinning = MutableStateFlow(false)
    val isSpinning: StateFlow<Boolean> = _isSpinning.asStateFlow()

    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result.asStateFlow()

    private val _rotationDegrees = MutableStateFlow(0f)
    val rotationDegrees: StateFlow<Float> = _rotationDegrees.asStateFlow()

    private val _selectedListId = MutableStateFlow<String?>(null)

    fun loadOptions(listId: String?) {
        _selectedListId.value = listId
        if (listId != null) {
            viewModelScope.launch {
                optionListRepository.optionLists.collect { lists ->
                    val list = lists.find { it.id == listId }
                    if (list != null) {
                        _options.value = list.options
                    }
                }
            }
        }
    }

    fun updateOptions(newOptions: List<String>) {
        _options.value = newOptions
    }

    fun spin() {
        if (_isSpinning.value || _options.value.isEmpty()) return

        _isSpinning.value = true
        _result.value = null

        val randomDegrees = (720..1440).random().toFloat() + (0..360).random().toFloat()
        _rotationDegrees.value = _rotationDegrees.value + randomDegrees

        viewModelScope.launch {
            kotlinx.coroutines.delay(3000) // Wait for animation
            val selectedIndex = (_options.value.indices).random()
            _result.value = _options.value[selectedIndex]
            _isSpinning.value = false

            historyRepository.addEntry(
                HistoryEntry(
                    method = DecisionMethod.SPIN_WHEEL,
                    options = _options.value,
                    result = _options.value[selectedIndex],
                )
            )
        }
    }

    fun clearResult() {
        _result.value = null
    }
}
