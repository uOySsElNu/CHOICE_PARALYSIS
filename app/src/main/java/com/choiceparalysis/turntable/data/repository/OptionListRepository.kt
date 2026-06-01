package com.choiceparalysis.turntable.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.choiceparalysis.turntable.data.datastore.DataStoreKeys
import com.choiceparalysis.turntable.data.model.OptionList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class OptionListRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

    val optionLists: Flow<List<OptionList>> = dataStore.data.map { preferences ->
        val json = preferences[DataStoreKeys.OPTION_LISTS] ?: "[]"
        Json.decodeFromString<List<OptionList>>(json)
    }

    suspend fun saveList(list: OptionList) {
        dataStore.edit { preferences ->
            val current = Json.decodeFromString<List<OptionList>>(preferences[DataStoreKeys.OPTION_LISTS] ?: "[]")
            val updated = current.filter { it.id != list.id } + list
            preferences[DataStoreKeys.OPTION_LISTS] = Json.encodeToString(updated)
        }
    }

    suspend fun deleteList(id: String) {
        dataStore.edit { preferences ->
            val current = Json.decodeFromString<List<OptionList>>(preferences[DataStoreKeys.OPTION_LISTS] ?: "[]")
            val updated = current.filter { it.id != id }
            preferences[DataStoreKeys.OPTION_LISTS] = Json.encodeToString(updated)
        }
    }

    suspend fun getList(id: String): OptionList? {
        val lists = dataStore.data.map { preferences ->
            val json = preferences[DataStoreKeys.OPTION_LISTS] ?: "[]"
            Json.decodeFromString<List<OptionList>>(json)
        }
        return lists.map { lists -> lists.find { it.id == id } }.let { flow ->
            var result: OptionList? = null
            flow.collect { result = it }
            result
        }
    }
}
