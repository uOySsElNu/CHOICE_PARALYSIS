package com.choiceparalysis.turntable.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_data")

object DataStoreKeys {
    val OPTION_LISTS = stringPreferencesKey("option_lists")
    val HISTORY = stringPreferencesKey("history")
    val DYNAMIC_COLOR_ENABLED = stringPreferencesKey("dynamic_color_enabled")
    val SELECTED_PRESET = stringPreferencesKey("selected_preset")
    val CUSTOM_COLORS = stringPreferencesKey("custom_colors")
    val CURRENT_OPTIONS = stringPreferencesKey("current_options")
    val OPTION_GROUPS = stringPreferencesKey("option_groups")
    val COIN_HEADS_IMAGE = stringPreferencesKey("coin_heads_image")
    val COIN_TAILS_IMAGE = stringPreferencesKey("coin_tails_image")
    val DICE_FACE_1_IMAGE = stringPreferencesKey("dice_face_1_image")
    val DICE_FACE_2_IMAGE = stringPreferencesKey("dice_face_2_image")
    val DICE_FACE_3_IMAGE = stringPreferencesKey("dice_face_3_image")
    val DICE_FACE_4_IMAGE = stringPreferencesKey("dice_face_4_image")
    val DICE_FACE_5_IMAGE = stringPreferencesKey("dice_face_5_image")
    val DICE_FACE_6_IMAGE = stringPreferencesKey("dice_face_6_image")
}
