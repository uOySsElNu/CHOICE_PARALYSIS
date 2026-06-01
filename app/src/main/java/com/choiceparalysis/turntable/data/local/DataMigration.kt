package com.choiceparalysis.turntable.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.choiceparalysis.turntable.data.datastore.DataStoreKeys
import com.choiceparalysis.turntable.data.local.dao.HistoryDao
import com.choiceparalysis.turntable.data.local.entity.HistoryEntity
import com.choiceparalysis.turntable.data.model.HistoryEntry
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject

class DataMigration @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val historyDao: HistoryDao
) {
    companion object {
        private val MIGRATION_DONE = booleanPreferencesKey("room_migration_done")
    }

    suspend fun migrateIfNeeded() {
        val preferences = dataStore.data.first()
        if (preferences[MIGRATION_DONE] == true) return

        migrateHistory()

        dataStore.edit { it[MIGRATION_DONE] = true }
    }

    private suspend fun migrateHistory() {
        val preferences = dataStore.data.first()
        val json = preferences[DataStoreKeys.HISTORY] ?: return
        if (json == "[]") return

        try {
            val entries = Json.decodeFromString<List<HistoryEntry>>(json)
            if (entries.isEmpty()) return

            val entities = entries.map { entry ->
                HistoryEntity(
                    legacyId = entry.id,
                    method = entry.method.name,
                    result = entry.result,
                    optionsSnapshot = Json.encodeToString(entry.options),
                    listName = entry.listName,
                    timestamp = entry.timestamp
                )
            }
            historyDao.insertAll(entities)
        } catch (e: Exception) {
            // Migration failed silently - data preserved in DataStore
            e.printStackTrace()
        }
    }
}
