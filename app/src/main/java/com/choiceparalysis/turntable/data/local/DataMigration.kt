package com.choiceparalysis.turntable.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.choiceparalysis.turntable.data.datastore.DataStoreKeys
import com.choiceparalysis.turntable.data.local.dao.CoinPresetDao
import com.choiceparalysis.turntable.data.local.dao.HistoryDao
import com.choiceparalysis.turntable.data.local.dao.OptionGroupDao
import com.choiceparalysis.turntable.data.local.entity.CoinPresetEntity
import com.choiceparalysis.turntable.data.local.entity.HistoryEntity
import com.choiceparalysis.turntable.data.local.entity.OptionGroupEntity
import com.choiceparalysis.turntable.data.model.CoinPreset
import com.choiceparalysis.turntable.data.model.HistoryEntry
import com.choiceparalysis.turntable.data.model.OptionGroup
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject

class DataMigration @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val historyDao: HistoryDao,
    private val optionGroupDao: OptionGroupDao,
    private val coinPresetDao: CoinPresetDao,
) {
    companion object {
        private val HISTORY_MIGRATED = booleanPreferencesKey("room_migration_done")
        private val OPTION_GROUPS_MIGRATED = booleanPreferencesKey("option_groups_migrated")
        private val COIN_PRESETS_MIGRATED = booleanPreferencesKey("coin_presets_migrated")
        private val jsonConfig = Json { ignoreUnknownKeys = true }
    }

    suspend fun migrateIfNeeded() {
        val preferences = dataStore.data.first()

        if (preferences[HISTORY_MIGRATED] != true) {
            try {
                migrateHistory()
                dataStore.edit { it[HISTORY_MIGRATED] = true }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (preferences[OPTION_GROUPS_MIGRATED] != true) {
            try {
                migrateOptionGroups()
                dataStore.edit { it[OPTION_GROUPS_MIGRATED] = true }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (preferences[COIN_PRESETS_MIGRATED] != true) {
            try {
                migrateCoinPresets()
                dataStore.edit { it[COIN_PRESETS_MIGRATED] = true }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun migrateHistory() {
        val preferences = dataStore.data.first()
        val json = preferences[DataStoreKeys.HISTORY] ?: return
        if (json == "[]") return

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
    }

    private suspend fun migrateOptionGroups() {
        val preferences = dataStore.data.first()
        val json = preferences[DataStoreKeys.OPTION_GROUPS] ?: return
        if (json == "[]") return

        val groups = jsonConfig.decodeFromString<List<OptionGroup>>(json)
        if (groups.isEmpty()) return

        val entities = groups.map { group ->
            OptionGroupEntity(
                id = group.id,
                name = group.name,
                options = jsonConfig.encodeToString(group.options),
                weights = jsonConfig.encodeToString(group.weights),
                createdAt = group.createdAt,
            )
        }
        optionGroupDao.insertAll(entities)

        // Clear DataStore key to avoid re-migration
        dataStore.edit { prefs -> prefs.remove(DataStoreKeys.OPTION_GROUPS) }
    }

    private suspend fun migrateCoinPresets() {
        val preferences = dataStore.data.first()
        val json = preferences[DataStoreKeys.COIN_PRESETS] ?: return
        if (json == "[]") return

        val presets = jsonConfig.decodeFromString<List<CoinPreset>>(json)
        if (presets.isEmpty()) return

        val entities = presets.map { preset ->
            CoinPresetEntity(
                id = preset.id,
                name = preset.name,
                headsImagePath = preset.headsImagePath,
                tailsImagePath = preset.tailsImagePath,
                createdAt = preset.createdAt,
            )
        }
        coinPresetDao.insertAll(entities)

        // Clear DataStore key to avoid re-migration
        dataStore.edit { prefs -> prefs.remove(DataStoreKeys.COIN_PRESETS) }
    }
}
