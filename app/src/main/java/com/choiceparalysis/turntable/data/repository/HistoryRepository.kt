package com.choiceparalysis.turntable.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.choiceparalysis.turntable.data.datastore.DataStoreKeys
import com.choiceparalysis.turntable.data.datastore.dataStore
import com.choiceparalysis.turntable.data.model.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HistoryRepository(private val context: Context) {

    val history: Flow<List<HistoryEntry>> = context.dataStore.data.map { preferences ->
        val json = preferences[DataStoreKeys.HISTORY] ?: "[]"
        Json.decodeFromString<List<HistoryEntry>>(json)
    }

    suspend fun addEntry(entry: HistoryEntry) {
        context.dataStore.edit { preferences ->
            val current = Json.decodeFromString<List<HistoryEntry>>(preferences[DataStoreKeys.HISTORY] ?: "[]")
            val updated = (listOf(entry) + current).take(100) // Keep last 100 entries
            preferences[DataStoreKeys.HISTORY] = Json.encodeToString(updated)
        }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { preferences ->
            preferences[DataStoreKeys.HISTORY] = "[]"
        }
    }

    suspend fun deleteEntry(id: String) {
        context.dataStore.edit { preferences ->
            val current = Json.decodeFromString<List<HistoryEntry>>(preferences[DataStoreKeys.HISTORY] ?: "[]")
            val updated = current.filter { it.id != id }
            preferences[DataStoreKeys.HISTORY] = Json.encodeToString(updated)
        }
    }
}
