package com.choiceparalysis.turntable.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "option_groups")
data class OptionGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val options: String,
    val weights: String,
    val createdAt: Long,
    val updatedAt: Long
)
