package com.choiceparalysis.turntable.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.choiceparalysis.turntable.R
import com.choiceparalysis.turntable.data.datastore.DataStoreKeys
import com.choiceparalysis.turntable.data.local.dao.CoinPresetDao
import com.choiceparalysis.turntable.data.local.dao.OptionGroupDao
import com.choiceparalysis.turntable.data.local.entity.CoinPresetEntity
import com.choiceparalysis.turntable.data.local.entity.OptionGroupEntity
import com.choiceparalysis.turntable.data.model.CoinPreset
import com.choiceparalysis.turntable.data.model.OptionGroup
import com.choiceparalysis.turntable.ui.components.CircularCropUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @param:ApplicationContext private val context: Context,
    private val optionGroupDao: OptionGroupDao,
    private val coinPresetDao: CoinPresetDao,
) {
    private val jsonConfig = Json { ignoreUnknownKeys = true }

    val dynamicColorEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DataStoreKeys.DYNAMIC_COLOR_ENABLED]?.toBooleanStrictOrNull() ?: true
    }

    val followSystemTheme: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DataStoreKeys.FOLLOW_SYSTEM_THEME]?.toBooleanStrictOrNull() ?: true
    }

    val darkMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DataStoreKeys.DARK_MODE]?.toBooleanStrictOrNull() ?: false
    }

    val appLocale: Flow<String> = dataStore.data.map { prefs ->
        prefs[DataStoreKeys.APP_LOCALE] ?: "system"
    }

    val soundEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DataStoreKeys.SOUND_ENABLED]?.toBooleanStrictOrNull() ?: true
    }

    val hapticEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DataStoreKeys.HAPTIC_ENABLED]?.toBooleanStrictOrNull() ?: true
    }

    val lastSpinResult: Flow<String?> = dataStore.data.map { prefs ->
        prefs[DataStoreKeys.LAST_SPIN_RESULT]
    }

    val lastCoinResult: Flow<String?> = dataStore.data.map { prefs ->
        prefs[DataStoreKeys.LAST_COIN_RESULT]
    }

    val lastDiceResult: Flow<String?> = dataStore.data.map { prefs ->
        prefs[DataStoreKeys.LAST_DICE_RESULT]
    }

    val selectedPreset: Flow<String> = dataStore.data.map { prefs ->
        prefs[DataStoreKeys.SELECTED_PRESET] ?: "CLASSIC_RAINBOW"
    }

    val customColors: Flow<List<Int>> = dataStore.data.map { prefs ->
        val json = prefs[DataStoreKeys.CUSTOM_COLORS] ?: "[]"
        runCatching { jsonConfig.decodeFromString<List<Int>>(json) }.getOrElse { emptyList() }
    }

    val currentOptions: Flow<List<String>> = dataStore.data.map { prefs ->
        val json = prefs[DataStoreKeys.CURRENT_OPTIONS] ?: "[\"Yes\",\"No\"]"
        runCatching { jsonConfig.decodeFromString<List<String>>(json) }.getOrElse { listOf("Yes", "No") }
    }

    val currentWeights: Flow<List<Int>> = dataStore.data.map { prefs ->
        val json = prefs[DataStoreKeys.OPTION_WEIGHTS] ?: "[1,1]"
        runCatching { jsonConfig.decodeFromString<List<Int>>(json) }.getOrElse { listOf(1, 1) }
    }

    // OptionGroups — now from Room
    val optionGroups: Flow<List<OptionGroup>> = optionGroupDao.getAll().map { entities ->
        entities.map { entity ->
            OptionGroup(
                id = entity.id,
                name = entity.name,
                options = runCatching { jsonConfig.decodeFromString<List<String>>(entity.options) }.getOrElse { emptyList() },
                weights = runCatching { jsonConfig.decodeFromString<List<Int>>(entity.weights) }.getOrElse { emptyList() },
                createdAt = entity.createdAt,
            )
        }
    }

    val coinHeadsImage: Flow<String?> = dataStore.data.map { prefs ->
        prefs[DataStoreKeys.COIN_HEADS_IMAGE]
    }

    val coinTailsImage: Flow<String?> = dataStore.data.map { prefs ->
        prefs[DataStoreKeys.COIN_TAILS_IMAGE]
    }

    // CoinPresets — now from Room
    val coinPresets: Flow<List<CoinPreset>> = coinPresetDao.getAll().map { entities ->
        entities.map { entity ->
            CoinPreset(
                id = entity.id,
                name = entity.name,
                headsImagePath = entity.headsImagePath,
                tailsImagePath = entity.tailsImagePath,
                createdAt = entity.createdAt,
            )
        }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[DataStoreKeys.DYNAMIC_COLOR_ENABLED] = enabled.toString()
        }
    }

    suspend fun setFollowSystemTheme(follow: Boolean) {
        dataStore.edit { prefs ->
            prefs[DataStoreKeys.FOLLOW_SYSTEM_THEME] = follow.toString()
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[DataStoreKeys.DARK_MODE] = enabled.toString()
        }
    }

    suspend fun setAppLocale(locale: String) {
        dataStore.edit { prefs ->
            prefs[DataStoreKeys.APP_LOCALE] = locale
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[DataStoreKeys.SOUND_ENABLED] = enabled.toString()
        }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[DataStoreKeys.HAPTIC_ENABLED] = enabled.toString()
        }
    }

    suspend fun setLastSpinResult(result: String?) {
        dataStore.edit { prefs ->
            if (result != null) prefs[DataStoreKeys.LAST_SPIN_RESULT] = result
            else prefs.remove(DataStoreKeys.LAST_SPIN_RESULT)
        }
    }

    suspend fun setLastCoinResult(result: String?) {
        dataStore.edit { prefs ->
            if (result != null) prefs[DataStoreKeys.LAST_COIN_RESULT] = result
            else prefs.remove(DataStoreKeys.LAST_COIN_RESULT)
        }
    }

    suspend fun setLastDiceResult(result: String?) {
        dataStore.edit { prefs ->
            if (result != null) prefs[DataStoreKeys.LAST_DICE_RESULT] = result
            else prefs.remove(DataStoreKeys.LAST_DICE_RESULT)
        }
    }

    suspend fun clearAllLastResults() {
        dataStore.edit { prefs ->
            prefs.remove(DataStoreKeys.LAST_SPIN_RESULT)
            prefs.remove(DataStoreKeys.LAST_COIN_RESULT)
            prefs.remove(DataStoreKeys.LAST_DICE_RESULT)
        }
    }

    suspend fun setSelectedPreset(preset: String) {
        dataStore.edit { prefs ->
            prefs[DataStoreKeys.SELECTED_PRESET] = preset
        }
    }

    suspend fun setCustomColors(colors: List<Int>) {
        dataStore.edit { prefs ->
            prefs[DataStoreKeys.CUSTOM_COLORS] = jsonConfig.encodeToString(colors)
        }
    }

    suspend fun setCurrentOptions(options: List<String>) {
        dataStore.edit { prefs ->
            prefs[DataStoreKeys.CURRENT_OPTIONS] = jsonConfig.encodeToString(options)
        }
    }

    suspend fun setCurrentWeights(weights: List<Int>) {
        dataStore.edit { prefs ->
            prefs[DataStoreKeys.OPTION_WEIGHTS] = jsonConfig.encodeToString(weights)
        }
    }

    // OptionGroups — now via Room
    suspend fun saveOptionGroup(group: OptionGroup) {
        optionGroupDao.upsert(
            OptionGroupEntity(
                id = group.id,
                name = group.name,
                options = jsonConfig.encodeToString(group.options),
                weights = jsonConfig.encodeToString(group.weights),
                createdAt = group.createdAt,
            )
        )
    }

    suspend fun deleteOptionGroup(id: String) {
        optionGroupDao.deleteById(id)
    }

    suspend fun setCoinHeadsImage(uri: String?) {
        dataStore.edit { prefs ->
            if (uri != null) prefs[DataStoreKeys.COIN_HEADS_IMAGE] = uri
            else prefs.remove(DataStoreKeys.COIN_HEADS_IMAGE)
        }
    }

    suspend fun setCoinTailsImage(uri: String?) {
        dataStore.edit { prefs ->
            if (uri != null) prefs[DataStoreKeys.COIN_TAILS_IMAGE] = uri
            else prefs.remove(DataStoreKeys.COIN_TAILS_IMAGE)
        }
    }

    suspend fun clearAllCustomImages() {
        dataStore.edit { prefs ->
            prefs.remove(DataStoreKeys.COIN_HEADS_IMAGE)
            prefs.remove(DataStoreKeys.COIN_TAILS_IMAGE)
        }
    }

    // CoinPresets — now via Room
    suspend fun saveCoinPreset(preset: CoinPreset) {
        coinPresetDao.upsert(
            CoinPresetEntity(
                id = preset.id,
                name = preset.name,
                headsImagePath = preset.headsImagePath,
                tailsImagePath = preset.tailsImagePath,
                createdAt = preset.createdAt,
            )
        )
    }

    suspend fun deleteCoinPreset(id: String) {
        coinPresetDao.deleteById(id)
    }

    companion object {
        const val DEFAULT_PRESET_ID = "default_tom_jerry"
    }

    suspend fun ensureDefaultCoinPreset() {
        val presets = coinPresets.first()
        if (presets.any { it.id == DEFAULT_PRESET_ID }) return

        val headsBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.coin_default_heads)
        val tailsBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.coin_default_tails)

        val croppedHeads = CircularCropUtil.cropToCircle(headsBitmap, 0.5f, 0.5f, 0.5f)
        val croppedTails = CircularCropUtil.cropToCircle(tailsBitmap, 0.5f, 0.5f, 0.5f)

        val headsPath = CircularCropUtil.saveToInternalStorage(context, croppedHeads, "default_heads.png")
        val tailsPath = CircularCropUtil.saveToInternalStorage(context, croppedTails, "default_tails.png")

        headsBitmap.recycle()
        tailsBitmap.recycle()

        val defaultPreset = CoinPreset(
            id = DEFAULT_PRESET_ID,
            name = "Tom & Jerry",
            headsImagePath = headsPath,
            tailsImagePath = tailsPath,
        )
        saveCoinPreset(defaultPreset)
    }
}
