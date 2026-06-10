package com.choiceparalysis.turntable.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.choiceparalysis.turntable.data.local.dao.CoinPresetDao
import com.choiceparalysis.turntable.data.local.dao.HistoryDao
import com.choiceparalysis.turntable.data.local.dao.OptionGroupDao
import com.choiceparalysis.turntable.data.local.entity.CoinPresetEntity
import com.choiceparalysis.turntable.data.local.entity.HistoryEntity
import com.choiceparalysis.turntable.data.local.entity.OptionGroupEntity

@Database(
    entities = [OptionGroupEntity::class, HistoryEntity::class, CoinPresetEntity::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun optionGroupDao(): OptionGroupDao
    abstract fun historyDao(): HistoryDao
    abstract fun coinPresetDao(): CoinPresetDao
}
