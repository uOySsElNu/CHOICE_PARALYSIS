package com.choiceparalysis.turntable.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.choiceparalysis.turntable.data.local.dao.HistoryDao
import com.choiceparalysis.turntable.data.local.dao.OptionGroupDao
import com.choiceparalysis.turntable.data.local.entity.HistoryEntity
import com.choiceparalysis.turntable.data.local.entity.OptionGroupEntity

@Database(
    entities = [OptionGroupEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun optionGroupDao(): OptionGroupDao
    abstract fun historyDao(): HistoryDao
}
