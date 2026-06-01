package com.choiceparalysis.turntable.ui.stats

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
import java.util.Calendar
import javax.inject.Inject

data class StatsState(
    val totalCount: Int = 0,
    val methodDistribution: Map<DecisionMethod, Int> = emptyMap(),
    val topResults: List<Pair<String, Int>> = emptyList(),
    val hourlyDistribution: List<Int> = List(24) { 0 },
    val mostUsedMethod: DecisionMethod? = null,
    val lastDecisionTime: Long? = null,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val historyRepository: HistoryRepository
) : ViewModel() {

    private val _stats = MutableStateFlow(StatsState())
    val stats: StateFlow<StatsState> = _stats.asStateFlow()

    init {
        viewModelScope.launch {
            historyRepository.history.collect { entries ->
                _stats.value = computeStats(entries)
            }
        }
    }

    private fun computeStats(entries: List<HistoryEntry>): StatsState {
        if (entries.isEmpty()) return StatsState()

        val methodDistribution = entries.groupBy { it.method }.mapValues { it.value.size }
        val mostUsed = methodDistribution.maxByOrNull { it.value }?.key

        val topResults = entries
            .groupBy { it.result }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key to it.value }

        val hourlyDistribution = MutableList(24) { 0 }
        val calendar = Calendar.getInstance()
        entries.forEach { entry ->
            calendar.timeInMillis = entry.timestamp
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            hourlyDistribution[hour]++
        }

        return StatsState(
            totalCount = entries.size,
            methodDistribution = methodDistribution,
            topResults = topResults,
            hourlyDistribution = hourlyDistribution,
            mostUsedMethod = mostUsed,
            lastDecisionTime = entries.firstOrNull()?.timestamp
        )
    }
}
