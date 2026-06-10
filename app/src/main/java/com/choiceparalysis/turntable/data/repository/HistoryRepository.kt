package com.choiceparalysis.turntable.data.repository

import com.choiceparalysis.turntable.data.local.dao.HistoryDao
import com.choiceparalysis.turntable.data.local.entity.HistoryEntity
import com.choiceparalysis.turntable.data.model.DecisionMethod
import com.choiceparalysis.turntable.data.model.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class HistoryRepository @Inject constructor(private val historyDao: HistoryDao) {

    val history: Flow<List<HistoryEntry>> = historyDao.getRecent().map { entities ->
        entities.map { it.toHistoryEntry() }
    }

    suspend fun addEntry(entry: HistoryEntry) {
        historyDao.insert(entry.toEntity())
        // Only trim when we have more than 100 entries (avoids subquery on every insert)
        if (historyDao.count() > 100) {
            historyDao.trimOld()
        }
    }

    suspend fun clearHistory() {
        historyDao.deleteAll()
    }

    suspend fun deleteEntry(id: String) {
        // Try numeric ID first (for entries with numeric PK)
        val numericId = id.toLongOrNull()
        if (numericId != null) {
            historyDao.deleteById(numericId)
        } else {
            // UUID-based legacy entries
            historyDao.deleteByLegacyId(id)
        }
    }

    private fun HistoryEntity.toHistoryEntry(): HistoryEntry {
        return HistoryEntry(
            id = legacyId ?: id.toString(),
            method = try {
                DecisionMethod.valueOf(method)
            } catch (e: IllegalArgumentException) {
                DecisionMethod.SPIN_WHEEL
            },
            options = try {
                Json.decodeFromString<List<String>>(optionsSnapshot)
            } catch (e: Exception) {
                emptyList()
            },
            result = result,
            listName = listName,
            timestamp = timestamp
        )
    }

    private fun HistoryEntry.toEntity(): HistoryEntity {
        return HistoryEntity(
            legacyId = id,
            method = method.name,
            result = result,
            optionsSnapshot = Json.encodeToString(options),
            listName = listName,
            timestamp = timestamp
        )
    }
}
