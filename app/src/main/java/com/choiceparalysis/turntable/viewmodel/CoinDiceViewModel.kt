package com.choiceparalysis.turntable.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.data.model.CoinPreset
import com.choiceparalysis.turntable.data.model.DecisionMethod
import com.choiceparalysis.turntable.data.repository.SettingsRepository.Companion.DEFAULT_PRESET_ID
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
    val pendingCoinResult: StateFlow<CoinSide?> = _pendingCoinResult.asStateFlow()
    private val _pendingDiceValue = MutableStateFlow<Int?>(null)

    // Custom image URIs
    private val _customCoinHeadsUri = MutableStateFlow<String?>(null)
    val customCoinHeadsUri: StateFlow<String?> = _customCoinHeadsUri.asStateFlow()

    private val _customCoinTailsUri = MutableStateFlow<String?>(null)
    val customCoinTailsUri: StateFlow<String?> = _customCoinTailsUri.asStateFlow()

    private val _coinPresets = MutableStateFlow<List<CoinPreset>>(emptyList())
    val coinPresets: StateFlow<List<CoinPreset>> = _coinPresets.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.coinHeadsImage.collect { _customCoinHeadsUri.value = it }
        }
        viewModelScope.launch {
            settingsRepository.coinTailsImage.collect { _customCoinTailsUri.value = it }
        }
        viewModelScope.launch {
            settingsRepository.coinPresets.collect { _coinPresets.value = it }
        }
        viewModelScope.launch {
            settingsRepository.ensureDefaultCoinPreset()
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

    /** Drag-triggered flip: record result without triggering button animation */
    fun flipCoinDirectly(result: CoinSide) {
        _pendingCoinResult.value = result
        _coinResult.value = result

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
        _isAnimating.value = false
    }

    fun setCustomCoinImage(side: CoinSide, uri: String?) {
        viewModelScope.launch {
            when (side) {
                CoinSide.HEADS -> settingsRepository.setCoinHeadsImage(uri)
                CoinSide.TAILS -> settingsRepository.setCoinTailsImage(uri)
            }
        }
    }

    fun clearAllImages() {
        viewModelScope.launch {
            settingsRepository.clearAllCustomImages()
        }
    }

    fun saveCoinPreset(name: String) {
        viewModelScope.launch {
            val headsUri = _customCoinHeadsUri.value ?: return@launch
            val tailsUri = _customCoinTailsUri.value ?: return@launch
            settingsRepository.saveCoinPreset(
                CoinPreset(name = name, headsImagePath = headsUri, tailsImagePath = tailsUri)
            )
        }
    }

    fun deleteCoinPreset(id: String) {
        if (id == DEFAULT_PRESET_ID) return
        viewModelScope.launch {
            settingsRepository.deleteCoinPreset(id)
        }
    }

    fun loadCoinPreset(preset: CoinPreset) {
        viewModelScope.launch {
            settingsRepository.setCoinHeadsImage(preset.headsImagePath)
            settingsRepository.setCoinTailsImage(preset.tailsImagePath)
        }
    }
}
