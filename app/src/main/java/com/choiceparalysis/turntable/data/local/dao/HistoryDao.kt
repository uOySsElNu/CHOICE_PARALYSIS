package com.choiceparalysis.turntable.data.local.dao

import androidx.room.*
import com.choiceparalysis.turntable.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC LIMIT 100")
    fun getRecent(): Flow<List<HistoryEntity>>

    @Insert
    suspend fun insert(entity: HistoryEntity)

    @Insert
    suspend fun insertAll(entities: List<HistoryEntity>)

    @Query("DELETE FROM history WHERE legacyId = :legacyId OR id = :id")
    suspend fun deleteById(legacyId: String, id: Long)

    @Query("DELETE FROM history")
    suspend fun deleteAll()

    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY timestamp DESC LIMIT 100)")
    suspend fun trimOld()

    @Query("SELECT COUNT(*) FROM history LIMIT 1")
    suspend fun count(): Int
}
