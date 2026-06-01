package com.choiceparalysis.turntable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.data.model.DecisionMethod
import com.choiceparalysis.turntable.data.model.HistoryEntry
import com.choiceparalysis.turntable.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiceViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _diceValue = MutableStateFlow<Int?>(null)
    val diceValue: StateFlow<Int?> = _diceValue.asStateFlow()

    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

    private val _pendingDiceValue = MutableStateFlow<Int?>(null)

    fun rollDice() {
        if (_isAnimating.value) return
        _isAnimating.value = true
        _diceValue.value = null
        _pendingDiceValue.value = (1..6).random()
    }

    fun onDiceRollAnimationComplete() {
        val result = _pendingDiceValue.value ?: return
        _diceValue.value = result
        _isAnimating.value = false
        viewModelScope.launch {
            historyRepository.addEntry(
                HistoryEntry(
                    method = DecisionMethod.DICE_ROLL,
                    options = listOf("1", "2", "3", "4", "5", "6"),
                    result = result.toString(),
                )
            )
        }
    }

    fun clearResult() {
        _diceValue.value = null
        _isAnimating.value = false
    }
}
