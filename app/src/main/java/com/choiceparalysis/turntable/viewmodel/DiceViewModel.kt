package com.choiceparalysis.turntable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.data.model.DecisionMethod
import com.choiceparalysis.turntable.data.model.HistoryEntry
import com.choiceparalysis.turntable.data.repository.HistoryRepository
import com.choiceparalysis.turntable.data.repository.SettingsRepository
import com.choiceparalysis.turntable.widget.WidgetDataSync
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class DiceViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _diceValue = MutableStateFlow<Int?>(null)
    val diceValue: StateFlow<Int?> = _diceValue.asStateFlow()

    private val _rollTrigger = MutableStateFlow(0)
    val rollTrigger: StateFlow<Int> = _rollTrigger.asStateFlow()

    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

    private val _pendingDiceValue = MutableStateFlow<Int?>(null)
    private var safetyTimeoutJob: Job? = null

    init {
        // Restore last persisted result
        viewModelScope.launch {
            settingsRepository.lastDiceResult.collect { saved ->
                if (saved != null && _diceValue.value == null) {
                    _diceValue.value = saved.toIntOrNull()
                }
            }
        }
    }

    fun rollDice() {
        if (_isAnimating.value) return
        _isAnimating.value = true
        _diceValue.value = null
        _pendingDiceValue.value = (1..6).random()
        // Safety timeout: force end animation if stuck for 10 seconds
        safetyTimeoutJob?.cancel()
        safetyTimeoutJob = viewModelScope.launch {
            delay(10_000.milliseconds)
            if (_isAnimating.value) {
                onDiceRollAnimationComplete()
            }
        }
    }

    fun onDiceRollAnimationComplete() {
        safetyTimeoutJob?.cancel()
        val result = _pendingDiceValue.value ?: return
        _diceValue.value = result
        _isAnimating.value = false
        _rollTrigger.value++
        viewModelScope.launch {
            settingsRepository.setLastDiceResult(result.toString())
            historyRepository.addEntry(
                HistoryEntry(
                    method = DecisionMethod.DICE_ROLL,
                    options = listOf("1", "2", "3", "4", "5", "6"),
                    result = result.toString(),
                )
            )
            WidgetDataSync.updateLastResultWidget(
                appContext, result.toString(), appContext.getString(com.choiceparalysis.turntable.R.string.method_dice_roll)
            )
        }
    }

    fun clearResult() {
        _diceValue.value = null
        viewModelScope.launch { settingsRepository.setLastDiceResult(null) }
    }

    fun clearDisplayResult() {
        _diceValue.value = null
    }
}
