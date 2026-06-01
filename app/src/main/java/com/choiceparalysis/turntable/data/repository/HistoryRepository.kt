package com.choiceparalysis.turntable.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.choiceparalysis.turntable.data.datastore.DataStoreKeys
import com.choiceparalysis.turntable.data.model.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class HistoryRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

    val history: Flow<List<HistoryEntry>> = dataStore.data.map { preferences ->
        val json = preferences[DataStoreKeys.HISTORY] ?: "[]"
        Json.decodeFromString<List<HistoryEntry>>(json)
    }

    suspend fun addEntry(entry: HistoryEntry) {
        dataStore.edit { preferences ->
            val current = Json.decodeFromString<List<HistoryEntry>>(preferences[DataStoreKeys.HISTORY] ?: "[]")
            val updated = (listOf(entry) + current).take(100) // Keep last 100 entries
            preferences[DataStoreKeys.HISTORY] = Json.encodeToString(updated)
        }
    }

    suspend fun clearHistory() {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.HISTORY] = "[]"
        }
    }

    suspend fun deleteEntry(id: String) {
        dataStore.edit { preferences ->
            val current = Json.decodeFromString<List<HistoryEntry>>(preferences[DataStoreKeys.HISTORY] ?: "[]")
            val updated = current.filter { it.id != id }
            preferences[DataStoreKeys.HISTORY] = Json.encodeToString(updated)
        }
    }
}
