package com.choiceparalysis.turntable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coin_presets")
data class CoinPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val headsImagePath: String,
    val tailsImagePath: String,
    val createdAt: Long,
)
