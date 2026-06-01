package com.choiceparalysis.turntable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val method: String,
    val result: String,
    val optionsSnapshot: String,
    val timestamp: Long
)
