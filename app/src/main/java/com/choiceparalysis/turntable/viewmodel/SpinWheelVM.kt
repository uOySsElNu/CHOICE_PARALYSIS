package com.choiceparalysis.turntable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.data.model.DecisionMethod
import com.choiceparalysis.turntable.data.model.HistoryEntry
import com.choiceparalysis.turntable.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpinWheelVM @Inject constructor(
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result.asStateFlow()

    private var spinJob: Job? = null

    /**
     * Start a spin animation. Returns false if already spinning.
     * The caller should run the animation and then call endSpin().
     */
    fun startSpin(): Boolean {
        if (_isAnimating.value) return false
        _isAnimating.value = true
        // Safety timeout: auto-reset after 10s
        spinJob = viewModelScope.launch {
            delay(10_000)
            if (_isAnimating.value) {
                _isAnimating.value = false
            }
        }
        return true
    }

    /**
     * End a spin animation. Always safe to call.
     */
    fun endSpin() {
        spinJob?.cancel()
        spinJob = null
        _isAnimating.value = false
    }

    /**
     * Record a spin result and save it to history.
     */
    fun recordSpinResult(selectedOption: String, currentOptions: List<String>) {
        _result.value = selectedOption
        viewModelScope.launch {
            historyRepository.addEntry(
                HistoryEntry(
                    method = DecisionMethod.SPIN_WHEEL,
                    options = currentOptions,
                    result = selectedOption,
                )
            )
        }
    }

    fun clearResult() {
        _result.value = null
    }
}
