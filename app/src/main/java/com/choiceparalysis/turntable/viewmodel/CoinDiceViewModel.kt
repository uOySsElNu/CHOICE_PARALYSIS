package com.choiceparalysis.turntable.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.data.model.DecisionMethod
import com.choiceparalysis.turntable.data.model.HistoryEntry
import com.choiceparalysis.turntable.data.repository.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CoinSide(val displayName: String) {
    HEADS("正面"),
    TAILS("反面")
}

class CoinDiceViewModel(application: Application) : AndroidViewModel(application) {
    private val historyRepository = HistoryRepository(application)

    private val _coinResult = MutableStateFlow<CoinSide?>(null)
    val coinResult: StateFlow<CoinSide?> = _coinResult.asStateFlow()

    private val _diceValue = MutableStateFlow<Int?>(null)
    val diceValue: StateFlow<Int?> = _diceValue.asStateFlow()

    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

    fun flipCoin() {
        if (_isAnimating.value) return
        _isAnimating.value = true
        _coinResult.value = null

        viewModelScope.launch {
            kotlinx.coroutines.delay(1500) // Animation time
            val result = if (Math.random() < 0.5) CoinSide.HEADS else CoinSide.TAILS
            _coinResult.value = result
            _isAnimating.value = false

            historyRepository.addEntry(
                HistoryEntry(
                    method = DecisionMethod.COIN_FLIP,
                    options = listOf("正面", "反面"),
                    result = result.displayName,
                )
            )
        }
    }

    fun rollDice() {
        if (_isAnimating.value) return
        _isAnimating.value = true
        _diceValue.value = null

        viewModelScope.launch {
            kotlinx.coroutines.delay(1500) // Animation time
            val result = (1..6).random()
            _diceValue.value = result
            _isAnimating.value = false

            historyRepository.addEntry(
                HistoryEntry(
                    method = DecisionMethod.DICE_ROLL,
                    options = listOf("1", "2", "3", "4", "5", "6"),
                    result = result.toString(),
                )
            )
        }
    }
}
