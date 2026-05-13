package com.choiceparalysis.turntable.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.choiceparalysis.turntable.data.datastore.DataStoreKeys
import com.choiceparalysis.turntable.data.datastore.dataStore
import com.choiceparalysis.turntable.data.model.OptionGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsRepository(private val context: Context) {

    val dynamicColorEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DataStoreKeys.DYNAMIC_COLOR_ENABLED]?.toBooleanStrictOrNull() ?: true
    }

    val selectedPreset: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[DataStoreKeys.SELECTED_PRESET] ?: "CLASSIC_RAINBOW"
    }

    val customColors: Flow<List<Int>> = context.dataStore.data.map { prefs ->
        val json = prefs[DataStoreKeys.CUSTOM_COLORS] ?: "[]"
        runCatching { Json.decodeFromString<List<Int>>(json) }.getOrElse { emptyList() }
    }

    val currentOptions: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val json = prefs[DataStoreKeys.CURRENT_OPTIONS] ?: "[\"Yes\",\"No\"]"
        runCatching { Json.decodeFromString<List<String>>(json) }.getOrElse { listOf("Yes", "No") }
    }

    val optionGroups: Flow<List<OptionGroup>> = context.dataStore.data.map { prefs ->
        val json = prefs[DataStoreKeys.OPTION_GROUPS] ?: "[]"
        runCatching { Json.decodeFromString<List<OptionGroup>>(json) }.getOrElse { emptyList() }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DataStoreKeys.DYNAMIC_COLOR_ENABLED] = enabled.toString()
        }
    }

    suspend fun setSelectedPreset(preset: String) {
        context.dataStore.edit { prefs ->
            prefs[DataStoreKeys.SELECTED_PRESET] = preset
        }
    }

    suspend fun setCustomColors(colors: List<Int>) {
        context.dataStore.edit { prefs ->
            prefs[DataStoreKeys.CUSTOM_COLORS] = Json.encodeToString(colors)
        }
    }

    suspend fun setCurrentOptions(options: List<String>) {
        context.dataStore.edit { prefs ->
            prefs[DataStoreKeys.CURRENT_OPTIONS] = Json.encodeToString(options)
        }
    }

    suspend fun saveOptionGroup(group: OptionGroup) {
        context.dataStore.edit { prefs ->
            val current = runCatching {
                Json.decodeFromString<List<OptionGroup>>(prefs[DataStoreKeys.OPTION_GROUPS] ?: "[]")
            }.getOrElse { emptyList() }
            val updated = current.filter { it.id != group.id } + group
            prefs[DataStoreKeys.OPTION_GROUPS] = Json.encodeToString(updated)
        }
    }

    suspend fun deleteOptionGroup(id: String) {
        context.dataStore.edit { prefs ->
            val current = runCatching {
                Json.decodeFromString<List<OptionGroup>>(prefs[DataStoreKeys.OPTION_GROUPS] ?: "[]")
            }.getOrElse { emptyList() }
            val updated = current.filter { it.id != id }
            prefs[DataStoreKeys.OPTION_GROUPS] = Json.encodeToString(updated)
        }
    }
}
