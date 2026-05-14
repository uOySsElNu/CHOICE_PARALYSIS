package com.choiceparalysis.turntable.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.data.model.DecisionMethod
import com.choiceparalysis.turntable.data.model.HistoryEntry
import com.choiceparalysis.turntable.data.repository.HistoryRepository
import com.choiceparalysis.turntable.data.repository.SettingsRepository
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
    private val settingsRepository = SettingsRepository(application)

    private val _coinResult = MutableStateFlow<CoinSide?>(null)
    val coinResult: StateFlow<CoinSide?> = _coinResult.asStateFlow()

    private val _diceValue = MutableStateFlow<Int?>(null)
    val diceValue: StateFlow<Int?> = _diceValue.asStateFlow()

    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

    private val _pendingCoinResult = MutableStateFlow<CoinSide?>(null)
    private val _pendingDiceValue = MutableStateFlow<Int?>(null)

    // Custom image URIs
    private val _customCoinHeadsUri = MutableStateFlow<String?>(null)
    val customCoinHeadsUri: StateFlow<String?> = _customCoinHeadsUri.asStateFlow()

    private val _customCoinTailsUri = MutableStateFlow<String?>(null)
    val customCoinTailsUri: StateFlow<String?> = _customCoinTailsUri.asStateFlow()

    private val _customDiceUris = MutableStateFlow<Map<Int, String?>>(emptyMap())
    val customDiceUris: StateFlow<Map<Int, String?>> = _customDiceUris.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.coinHeadsImage.collect { _customCoinHeadsUri.value = it }
        }
        viewModelScope.launch {
            settingsRepository.coinTailsImage.collect { _customCoinTailsUri.value = it }
        }
        viewModelScope.launch {
            settingsRepository.diceImages.collect { _customDiceUris.value = it }
        }
    }

    fun flipCoin() {
        if (_isAnimating.value) return
        _isAnimating.value = true
        _coinResult.value = null
        _pendingCoinResult.value = if (Math.random() < 0.5) CoinSide.HEADS else CoinSide.TAILS
    }

    fun onCoinFlipAnimationComplete() {
        val result = _pendingCoinResult.value ?: return
        _coinResult.value = result
        _isAnimating.value = false

        viewModelScope.launch {
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

    fun clearResults() {
        _coinResult.value = null
        _diceValue.value = null
    }

    fun setCustomCoinImage(side: CoinSide, uri: String?) {
        viewModelScope.launch {
            when (side) {
                CoinSide.HEADS -> settingsRepository.setCoinHeadsImage(uri)
                CoinSide.TAILS -> settingsRepository.setCoinTailsImage(uri)
            }
        }
    }

    fun setCustomDiceFace(face: Int, uri: String?) {
        viewModelScope.launch {
            settingsRepository.setDiceFaceImage(face, uri)
        }
    }

    fun clearAllImages() {
        viewModelScope.launch {
            settingsRepository.clearAllCustomImages()
        }
    }
}
