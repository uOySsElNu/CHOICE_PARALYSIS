package com.choiceparalysis.turntable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.data.model.DecisionMethod
import com.choiceparalysis.turntable.data.model.HistoryEntry
import com.choiceparalysis.turntable.data.repository.HistoryRepository
import com.choiceparalysis.turntable.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class SpinWheelVM @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result.asStateFlow()

    // Incremented on each new spin result. Used as LaunchedEffect key so Toast
    // fires even when consecutive spins land on the same option.
    private val _spinTrigger = MutableStateFlow(0)
    val spinTrigger: StateFlow<Int> = _spinTrigger.asStateFlow()

    init {
        // Restore last persisted result on app restart
        viewModelScope.launch {
            settingsRepository.lastSpinResult.collect { saved ->
                if (saved != null && _result.value == null) {
                    _result.value = saved
                }
            }
        }
    }

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
            delay(10_000.milliseconds)
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
        _spinTrigger.value++  // new spin → trigger Toast
        viewModelScope.launch {
            settingsRepository.setLastSpinResult(selectedOption)
            historyRepository.addEntry(
                HistoryEntry(
                    method = DecisionMethod.SPIN_WHEEL,
                    options = currentOptions,
                    result = selectedOption,
                )
            )
        }
    }

    /** Clear display result only — keep persisted value so it restores on re-enter. */
    fun clearDisplayResult() {
        _result.value = null
    }
}
