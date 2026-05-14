package com.choiceparalysis.turntable.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CoinPreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val headsImagePath: String,
    val tailsImagePath: String,
    val createdAt: Long = System.currentTimeMillis(),
)
