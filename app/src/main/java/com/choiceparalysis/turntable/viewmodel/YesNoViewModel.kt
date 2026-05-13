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

enum class YesNoResult(val displayName: String, val emoji: String) {
    YES("是的!", "👍"),
    NO("不行!", "👎"),
    MAYBE("也许吧", "🤔")
}

class YesNoViewModel(application: Application) : AndroidViewModel(application) {
    private val historyRepository = HistoryRepository(application)

    private val _result = MutableStateFlow<YesNoResult?>(null)
    val result: StateFlow<YesNoResult?> = _result.asStateFlow()

    private val _isAnimating = MutableStateFlow(false)
    val isAnimating: StateFlow<Boolean> = _isAnimating.asStateFlow()

    private val _customQuestion = MutableStateFlow("")
    val customQuestion: StateFlow<String> = _customQuestion.asStateFlow()

    fun updateQuestion(question: String) {
        _customQuestion.value = question
    }

    fun decide() {
        if (_isAnimating.value) return
        _isAnimating.value = true
        _result.value = null

        viewModelScope.launch {
            kotlinx.coroutines.delay(1000) // Animation time
            val random = Math.random()
            val result = when {
                random < 0.45 -> YesNoResult.YES
                random < 0.9 -> YesNoResult.NO
                else -> YesNoResult.MAYBE
            }
            _result.value = result
            _isAnimating.value = false

            historyRepository.addEntry(
                HistoryEntry(
                    method = DecisionMethod.YES_NO,
                    options = listOf("是", "否", "也许"),
                    result = result.displayName,
                )
            )
        }
    }

    fun clearResult() {
        _result.value = null
    }
}
