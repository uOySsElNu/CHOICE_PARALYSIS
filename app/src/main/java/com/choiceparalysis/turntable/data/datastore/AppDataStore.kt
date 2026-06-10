package com.choiceparalysis.turntable.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_data")

object DataStoreKeys {
    val HISTORY = stringPreferencesKey("history") // kept for DataMigration
    val DYNAMIC_COLOR_ENABLED = stringPreferencesKey("dynamic_color_enabled")
    val SELECTED_PRESET = stringPreferencesKey("selected_preset")
    val CUSTOM_COLORS = stringPreferencesKey("custom_colors")
    val CURRENT_OPTIONS = stringPreferencesKey("current_options")
    val OPTION_GROUPS = stringPreferencesKey("option_groups")
    val COIN_HEADS_IMAGE = stringPreferencesKey("coin_heads_image")
    val COIN_TAILS_IMAGE = stringPreferencesKey("coin_tails_image")
    val COIN_PRESETS = stringPreferencesKey("coin_presets")
    val OPTION_WEIGHTS = stringPreferencesKey("option_weights")
    val FOLLOW_SYSTEM_THEME = stringPreferencesKey("follow_system_theme")
    val DARK_MODE = stringPreferencesKey("dark_mode")
    val SOUND_ENABLED = stringPreferencesKey("sound_enabled")
    val HAPTIC_ENABLED = stringPreferencesKey("haptic_enabled")
    val LAST_SPIN_RESULT = stringPreferencesKey("last_spin_result")
    val LAST_COIN_RESULT = stringPreferencesKey("last_coin_result")
    val LAST_DICE_RESULT = stringPreferencesKey("last_dice_result")
    val APP_LOCALE = stringPreferencesKey("app_locale")
}
