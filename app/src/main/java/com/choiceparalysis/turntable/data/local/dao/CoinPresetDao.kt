package com.choiceparalysis.turntable.data.local.dao

import androidx.room.*
import com.choiceparalysis.turntable.data.local.entity.CoinPresetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinPresetDao {
    @Query("SELECT * FROM coin_presets ORDER BY createdAt DESC")
    fun getAll(): Flow<List<CoinPresetEntity>>

    @Query("SELECT * FROM coin_presets WHERE id = :id")
    suspend fun getById(id: String): CoinPresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CoinPresetEntity)

    @Query("DELETE FROM coin_presets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CoinPresetEntity>)
}
