package com.choiceparalysis.turntable.data.local.dao

import androidx.room.*
import com.choiceparalysis.turntable.data.local.entity.OptionGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OptionGroupDao {
    @Query("SELECT * FROM option_groups ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<OptionGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OptionGroupEntity)

    @Delete
    suspend fun delete(entity: OptionGroupEntity)
}
