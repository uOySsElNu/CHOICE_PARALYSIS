package com.choiceparalysis.turntable.ui.fingerroulette

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FingerInfo(
    val id: Int,
    val x: Float,
    val y: Float,
    val isEliminated: Boolean = false,
    val isCurrentlyTargeted: Boolean = false,
)

enum class RoulettePhase {
    WAITING,
    READY,
    PLAYING,
    FINISHED,
}

@HiltViewModel
class FingerRouletteViewModel @Inject constructor() : ViewModel() {

    private val _fingers = MutableStateFlow<List<FingerInfo>>(emptyList())
    val fingers: StateFlow<List<FingerInfo>> = _fingers.asStateFlow()

    private val _phase = MutableStateFlow(RoulettePhase.WAITING)
    val phase: StateFlow<RoulettePhase> = _phase.asStateFlow()

    private val _winnerId = MutableStateFlow<Int?>(null)
    val winnerId: StateFlow<Int?> = _winnerId.asStateFlow()

    // Incremented each time a target is highlighted (for haptic feedback)
    private val _targetFlash = MutableStateFlow(0)
    val targetFlash: StateFlow<Int> = _targetFlash.asStateFlow()

    private var nextId = 0
    private var autoStartJob: Job? = null

    fun addFinger(x: Float, y: Float): Int {
        val id = nextId++
        val current = _fingers.value.toMutableList()
        current.add(FingerInfo(id = id, x = x, y = y))
        _fingers.value = current
        updatePhase()
        return id
    }

    fun updateFingerPosition(id: Int, x: Float, y: Float) {
        val current = _fingers.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            current[index] = current[index].copy(x = x, y = y)
            _fingers.value = current
        }
    }

    fun removeFinger(id: Int) {
        val current = _fingers.value.toMutableList()
        current.removeAll { it.id == id }
        _fingers.value = current
        updatePhase()
    }

    private fun updatePhase() {
        val activeFingers = _fingers.value.filter { !it.isEliminated }
        val newPhase = when {
            _winnerId.value != null -> RoulettePhase.FINISHED
            activeFingers.size < 2 -> RoulettePhase.WAITING
            else -> RoulettePhase.READY
        }
        _phase.value = newPhase

        // Auto-start after 2s of no new fingers when READY
        if (newPhase == RoulettePhase.READY) {
            autoStartJob?.cancel()
            autoStartJob = viewModelScope.launch {
                delay(2000)
                if (_phase.value == RoulettePhase.READY) {
                    startElimination()
                }
            }
        } else {
            autoStartJob?.cancel()
        }
    }

    fun startElimination() {
        if (_phase.value != RoulettePhase.READY) return
        _phase.value = RoulettePhase.PLAYING
        viewModelScope.launch {
            runEliminationLoop()
        }
    }

    private suspend fun runEliminationLoop() {
        var delayMs = 1500L
        while (true) {
            val activeFingers = _fingers.value.filter { !it.isEliminated }
            if (activeFingers.size <= 1) {
                if (activeFingers.size == 1) {
                    _winnerId.value = activeFingers[0].id
                }
                _phase.value = RoulettePhase.FINISHED
                return
            }

            // Pick random target
            val target = activeFingers.random()
            // Highlight target
            val current = _fingers.value.toMutableList()
            val index = current.indexOfFirst { it.id == target.id }
            if (index >= 0) {
                current[index] = current[index].copy(isCurrentlyTargeted = true)
                _fingers.value = current
                _targetFlash.value++
            }

            delay(800) // Flash duration

            // Eliminate target
            val updated = _fingers.value.toMutableList()
            val idx = updated.indexOfFirst { it.id == target.id }
            if (idx >= 0) {
                updated[idx] = updated[idx].copy(isEliminated = true, isCurrentlyTargeted = false)
                _fingers.value = updated
            }

            delayMs = (delayMs * 0.85).toLong().coerceAtLeast(400L)
            delay(delayMs - 800)
        }
    }

    fun reset() {
        _fingers.value = emptyList()
        _phase.value = RoulettePhase.WAITING
        _winnerId.value = null
        nextId = 0
    }
}
