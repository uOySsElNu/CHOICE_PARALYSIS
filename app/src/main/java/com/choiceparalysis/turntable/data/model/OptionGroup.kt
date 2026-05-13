package com.choiceparalysis.turntable.data.model

import kotlinx.serialization.Serializable

@Serializable
data class OptionGroup(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val options: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
)
