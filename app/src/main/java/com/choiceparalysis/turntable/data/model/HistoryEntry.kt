package com.choiceparalysis.turntable.data.model

import kotlinx.serialization.Serializable

@Serializable
data class HistoryEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val method: DecisionMethod,
    val options: List<String>,
    val result: String,
    val listName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)
