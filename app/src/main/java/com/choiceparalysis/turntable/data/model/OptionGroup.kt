package com.choiceparalysis.turntable.data.model

import kotlinx.serialization.Serializable

@Serializable
data class OptionGroup(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val options: List<String>,
    val weights: List<Int> = options.map { 1 },
    val createdAt: Long = System.currentTimeMillis(),
)
