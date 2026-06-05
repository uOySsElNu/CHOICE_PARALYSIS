package com.choiceparalysis.turntable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choiceparalysis.turntable.data.model.CoinPreset
import com.choiceparalysis.turntable.data.model.DecisionMethod
import com.choiceparalysis.turntable.data.model.HistoryEntry
import com.choiceparalysis.turntable.data.repository.HistoryRepository
import com.choiceparalysis.turntable.data.repository.SettingsRepository
import com.choiceparalysis.turntable.data.repository.SettingsRepository.Companion.DEFAULT_PRESET_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CoinSide(val displayName: String) {
    HEADS("正面"),
    TAILS("反面")
}

@HiltViewModel
class CoinViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _coinResult = MutableStateFlow<CoinSide?>(null)
    val coinResult: StateFlow<CoinSide?> = _coinResult.asStateFlow()

    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

    // Fling animation state (drag-initiated spin)
    private val _isFling = MutableStateFlow(false)
    val isFling: StateFlow<Boolean> = _isFling.asStateFlow()

    // Safety timeout to prevent permanent stuck state
    private var safetyTimeoutJob: Job? = null

    fun setFling(value: Boolean) {
        _isFling.value = value
    }

    private val _pendingCoinResult = MutableStateFlow<CoinSide?>(null)
    val pendingCoinResult: StateFlow<CoinSide?> = _pendingCoinResult.asStateFlow()

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
        if (_isAnimating.value || _isFling.value) return
        _isAnimating.value = true
        _coinResult.value = null
        _pendingCoinResult.value = if (Math.random() < 0.5) CoinSide.HEADS else CoinSide.TAILS
        // Safety timeout: force end animation if stuck for 10 seconds
        safetyTimeoutJob?.cancel()
        safetyTimeoutJob = viewModelScope.launch {
            delay(10_000)
            if (_isAnimating.value) {
                onCoinFlipAnimationComplete()
            }
        }
    }

    fun onCoinFlipAnimationComplete() {
        safetyTimeoutJob?.cancel()
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

    fun clearResult() {
        _coinResult.value = null
        // Don't reset _isAnimating here — let animation lifecycle manage it
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
