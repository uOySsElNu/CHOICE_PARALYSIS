package com.choiceparalysis.turntable.data.repository

import android.content.Context
import android.graphics.BitmapFactory
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.choiceparalysis.turntable.R
import com.choiceparalysis.turntable.data.datastore.DataStoreKeys
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
    @param:ApplicationContext private val context: Context
) {
    private val jsonConfig = Json { ignoreUnknownKeys = true }

    val dynamicColorEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DataStoreKeys.DYNAMIC_COLOR_ENABLED]?.toBooleanStrictOrNull() ?: true
    }

    val followSystemTheme: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DataStoreKeys.FOLLOW_SYSTEM_THEME]?.toBooleanStrictOrNull() ?: true
    }

    /** Manual dark mode preference (only used when followSystemTheme is false). */
    val darkMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DataStoreKeys.DARK_MODE]?.toBooleanStrictOrNull() ?: false
    }

    /** App locale code: "system" (follow system), "zh", "en", "ja", "ko", etc. */
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

    val optionGroups: Flow<List<OptionGroup>> = dataStore.data.map { prefs ->
        val json = prefs[DataStoreKeys.OPTION_GROUPS] ?: "[]"
        runCatching { jsonConfig.decodeFromString<List<OptionGroup>>(json) }.getOrElse { emptyList() }
    }

    val coinHeadsImage: Flow<String?> = dataStore.data.map { prefs ->
        prefs[DataStoreKeys.COIN_HEADS_IMAGE]
    }

    val coinTailsImage: Flow<String?> = dataStore.data.map { prefs ->
        prefs[DataStoreKeys.COIN_TAILS_IMAGE]
    }

    val coinPresets: Flow<List<CoinPreset>> = dataStore.data.map { prefs ->
        val json = prefs[DataStoreKeys.COIN_PRESETS] ?: "[]"
        runCatching { jsonConfig.decodeFromString<List<CoinPreset>>(json) }.getOrElse { emptyList() }
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

    suspend fun saveOptionGroup(group: OptionGroup) {
        dataStore.edit { prefs ->
            val current = runCatching {
                jsonConfig.decodeFromString<List<OptionGroup>>(prefs[DataStoreKeys.OPTION_GROUPS] ?: "[]")
            }.getOrElse { emptyList() }
            val updated = current.filter { it.id != group.id } + group
            prefs[DataStoreKeys.OPTION_GROUPS] = jsonConfig.encodeToString(updated)
        }
    }

    suspend fun deleteOptionGroup(id: String) {
        dataStore.edit { prefs ->
            val current = runCatching {
                jsonConfig.decodeFromString<List<OptionGroup>>(prefs[DataStoreKeys.OPTION_GROUPS] ?: "[]")
            }.getOrElse { emptyList() }
            val updated = current.filter { it.id != id }
            prefs[DataStoreKeys.OPTION_GROUPS] = jsonConfig.encodeToString(updated)
        }
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

    suspend fun saveCoinPreset(preset: CoinPreset) {
        dataStore.edit { prefs ->
            val current = runCatching {
                jsonConfig.decodeFromString<List<CoinPreset>>(prefs[DataStoreKeys.COIN_PRESETS] ?: "[]")
            }.getOrElse { emptyList() }
            val updated = current.filter { it.id != preset.id } + preset
            prefs[DataStoreKeys.COIN_PRESETS] = jsonConfig.encodeToString(updated)
        }
    }

    suspend fun deleteCoinPreset(id: String) {
        dataStore.edit { prefs ->
            val current = runCatching {
                jsonConfig.decodeFromString<List<CoinPreset>>(prefs[DataStoreKeys.COIN_PRESETS] ?: "[]")
            }.getOrElse { emptyList() }
            val updated = current.filter { it.id != id }
            prefs[DataStoreKeys.COIN_PRESETS] = jsonConfig.encodeToString(updated)
        }
    }

    companion object {
        const val DEFAULT_PRESET_ID = "default_tom_jerry"
    }

    suspend fun ensureDefaultCoinPreset() {
        val presets = coinPresets.first()
        if (presets.any { it.id == DEFAULT_PRESET_ID }) return

        // Load drawable resources and crop to circles
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
