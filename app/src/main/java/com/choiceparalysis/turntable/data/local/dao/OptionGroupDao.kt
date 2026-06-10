package com.choiceparalysis.turntable.data.local.dao

import androidx.room.*
import com.choiceparalysis.turntable.data.local.entity.OptionGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OptionGroupDao {
    @Query("SELECT * FROM option_groups ORDER BY createdAt DESC")
    fun getAll(): Flow<List<OptionGroupEntity>>

    @Query("SELECT * FROM option_groups WHERE id = :id")
    suspend fun getById(id: String): OptionGroupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OptionGroupEntity)

    @Query("DELETE FROM option_groups WHERE id = :id")
    suspend fun deleteById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<OptionGroupEntity>)
}
