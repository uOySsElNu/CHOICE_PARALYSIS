package com.choiceparalysis.turntable.ui.settings

import android.app.Activity
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.choiceparalysis.turntable.BuildConfig
import com.choiceparalysis.turntable.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.choiceparalysis.turntable.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onEasterEgg: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showTermsOfUse by remember { mutableStateOf(false) }
    var showOpenSourceLicenses by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val followSystemTheme by viewModel.followSystemTheme.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val appLocale by viewModel.appLocale.collectAsState()

    // Easter egg: 6 consecutive taps on version
    var versionTapCount by remember { mutableIntStateOf(0) }
    var versionTapReset by remember { mutableStateOf(false) }
    LaunchedEffect(versionTapReset) {
        if (versionTapCount > 0) {
            delay(2000.milliseconds)
            versionTapCount = 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Sound & Haptic section
        Text(
            text = stringResource(R.string.settings_sound_haptic_section),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_sound)) },
                    supportingContent = { Text(stringResource(R.string.settings_sound_description)) },
                    trailingContent = {
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { value ->
                                viewModel.playHapticTick()
                                viewModel.setSoundEnabled(value)
                            }
                        )
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_haptic)) },
                    supportingContent = { Text(stringResource(R.string.settings_haptic_description)) },
                    trailingContent = {
                        Switch(
                            checked = hapticEnabled,
                            onCheckedChange = { value ->
                                viewModel.playHapticTick()
                                viewModel.setHapticEnabled(value)
                            }
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display section
        Text(
            text = stringResource(R.string.settings_display_section),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_follow_system_theme)) },
                    supportingContent = { Text(stringResource(R.string.settings_follow_system_theme_description)) },
                    trailingContent = {
                        Switch(
                            checked = followSystemTheme,
                            onCheckedChange = { value ->
                                viewModel.playHapticTick()
                                viewModel.setFollowSystemTheme(value)
                            }
                        )
                    }
                )
                if (!followSystemTheme) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_dark_mode)) },
                        supportingContent = { Text(stringResource(if (darkMode) R.string.settings_dark_mode_on else R.string.settings_light_mode_on)) },
                        trailingContent = {
                            Switch(
                                checked = darkMode,
                                onCheckedChange = { value ->
                                    viewModel.playHapticTick()
                                    viewModel.setDarkMode(value)
                                }
                            )
                        }
                    )
                }
                val localeLabel = when (appLocale) {
                    "system" -> stringResource(R.string.locale_follow_system)
                    "zh" -> stringResource(R.string.locale_simplified_chinese)
                    "zh-TW" -> stringResource(R.string.locale_traditional_chinese)
                    "en" -> stringResource(R.string.locale_english)
                    "ja" -> stringResource(R.string.locale_japanese)
                    "ko" -> stringResource(R.string.locale_korean)
                    "es" -> stringResource(R.string.locale_spanish)
                    "fr" -> stringResource(R.string.locale_french)
                    "de" -> stringResource(R.string.locale_german)
                    "ru" -> stringResource(R.string.locale_russian)
                    "pt" -> stringResource(R.string.locale_portuguese)
                    "ar" -> stringResource(R.string.locale_arabic)
                    else -> stringResource(R.string.locale_follow_system)
                }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_language)) },
                    supportingContent = { Text(localeLabel) },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable { showLanguageDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About section
        Text(
            text = stringResource(R.string.settings_about_section),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_version)) },
                    trailingContent = {
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable {
                        versionTapCount++
                        versionTapReset = !versionTapReset
                        if (versionTapCount >= 6) {
                            versionTapCount = 0
                            onEasterEgg()
                        }
                    }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_privacy_policy)) },
                    leadingContent = {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable { showPrivacyPolicy = true }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_terms_of_use)) },
                    leadingContent = {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable { showTermsOfUse = true }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_open_source_licenses)) },
                    leadingContent = {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable { showOpenSourceLicenses = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Privacy Policy Dialog
    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicy = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_privacy_policy),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(text = stringResource(R.string.privacy_last_updated))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.privacy_intro))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.privacy_s1_title), fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.privacy_s1_body))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.privacy_s2_title), fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.privacy_s2_body))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.privacy_s3_title), fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.privacy_s3_body))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.privacy_s4_title), fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.privacy_s4_body))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.privacy_s5_title), fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.privacy_s5_body))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.privacy_s6_title), fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.privacy_s6_body))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.privacy_s7_title), fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.privacy_s7_body))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.privacy_s8_title), fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.privacy_s8_body))
                    val disclaimer = stringResource(R.string.ai_translation_disclaimer)
                    if (disclaimer.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = disclaimer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicy = false }) {
                    Text(stringResource(R.string.btn_confirm))
                }
            }
        )
    }

    // Terms of Use Dialog
    if (showTermsOfUse) {
        AlertDialog(
            onDismissRequest = { showTermsOfUse = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_terms_of_use),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(text = stringResource(R.string.terms_last_updated))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.terms_intro))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.terms_s1_title), fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.terms_s1_body))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.terms_s2_title), fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.terms_s2_body))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.terms_s3_title), fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.terms_s3_body))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.terms_s4_title), fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.terms_s4_body))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.terms_s5_title), fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.terms_s5_body))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.terms_s6_title), fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.terms_s6_body))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.terms_s7_title), fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.terms_s7_body))
                    val disclaimer = stringResource(R.string.ai_translation_disclaimer)
                    if (disclaimer.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = disclaimer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermsOfUse = false }) {
                    Text(stringResource(R.string.btn_confirm))
                }
            }
        )
    }

    // Open Source Licenses Dialog
    if (showOpenSourceLicenses) {
        AlertDialog(
            onDismissRequest = { showOpenSourceLicenses = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_open_source_licenses),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(text = stringResource(R.string.oss_intro))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = stringResource(R.string.oss_compose_name), fontWeight = FontWeight.Bold)
                    Text(text = "${stringResource(R.string.oss_copyright)} © Android Open Source Project")
                    Text(text = stringResource(R.string.oss_license_label))
                    Text(text = stringResource(R.string.oss_compose_desc))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = stringResource(R.string.oss_hilt_name), fontWeight = FontWeight.Bold)
                    Text(text = "${stringResource(R.string.oss_copyright)} © Google LLC")
                    Text(text = stringResource(R.string.oss_license_label))
                    Text(text = stringResource(R.string.oss_hilt_desc))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = stringResource(R.string.oss_room_name), fontWeight = FontWeight.Bold)
                    Text(text = "${stringResource(R.string.oss_copyright)} © Google LLC")
                    Text(text = stringResource(R.string.oss_license_label))
                    Text(text = stringResource(R.string.oss_room_desc))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = stringResource(R.string.oss_coil_name), fontWeight = FontWeight.Bold)
                    Text(text = "${stringResource(R.string.oss_copyright)} © Coil Contributors")
                    Text(text = stringResource(R.string.oss_license_label))
                    Text(text = stringResource(R.string.oss_coil_desc))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = stringResource(R.string.oss_navigation_name), fontWeight = FontWeight.Bold)
                    Text(text = "${stringResource(R.string.oss_copyright)} © Android Open Source Project")
                    Text(text = stringResource(R.string.oss_license_label))
                    Text(text = stringResource(R.string.oss_navigation_desc))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = stringResource(R.string.oss_datastore_name), fontWeight = FontWeight.Bold)
                    Text(text = "${stringResource(R.string.oss_copyright)} © Android Open Source Project")
                    Text(text = stringResource(R.string.oss_license_label))
                    Text(text = stringResource(R.string.oss_datastore_desc))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Apache License 2.0 — ${stringResource(R.string.oss_apache_intro)}", fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.oss_apache_use))
                    Text(text = stringResource(R.string.oss_apache_modify))
                    Text(text = stringResource(R.string.oss_apache_distribute))
                    Text(text = stringResource(R.string.oss_apache_conditions))
                    Text(text = stringResource(R.string.oss_apache_copyright_notice))
                    Text(text = stringResource(R.string.oss_apache_changes))
                    Text(text = "${stringResource(R.string.oss_apache_link)}\nhttps://www.apache.org/licenses/LICENSE-2.0")
                    val disclaimer = stringResource(R.string.ai_translation_disclaimer)
                    if (disclaimer.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = disclaimer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOpenSourceLicenses = false }) {
                    Text(stringResource(R.string.btn_confirm))
                }
            }
        )
    }

    // Language selector dialog
    if (showLanguageDialog) {
        val languages = listOf(
            "system" to stringResource(R.string.locale_follow_system),
            "zh" to stringResource(R.string.locale_simplified_chinese),
            "zh-TW" to stringResource(R.string.locale_traditional_chinese),
            "en" to stringResource(R.string.locale_english),
            "ja" to stringResource(R.string.locale_japanese),
            "ko" to stringResource(R.string.locale_korean),
            "es" to stringResource(R.string.locale_spanish),
            "fr" to stringResource(R.string.locale_french),
            "de" to stringResource(R.string.locale_german),
            "ru" to stringResource(R.string.locale_russian),
            "pt" to stringResource(R.string.locale_portuguese),
            "ar" to stringResource(R.string.locale_arabic),
        )
        val configuration = LocalConfiguration.current
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.dialog_select_language)) },
            text = {
                Column {
                    languages.forEach { (code, label) ->
                        ListItem(
                            headlineContent = { Text(label) },
                            trailingContent = {
                                if (appLocale == code) {
                                    Text("✓", color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.clickable {
                                viewModel.playHapticTick()
                                scope.launch {
                                    viewModel.setAppLocale(code)
                                    val targetLocale = if (code == "system") {
                                        // Get the REAL system locale, not the app-overridden one
                                        Resources.getSystem().configuration.locales[0]
                                    } else {
                                        Locale.forLanguageTag(code)
                                    }
                                    Locale.setDefault(targetLocale)
                                    val config = Configuration(Resources.getSystem().configuration)
                                    config.setLocale(targetLocale)
                                    @Suppress("DEPRECATION")
                                    context.resources.updateConfiguration(config, context.resources.displayMetrics)
                                    (context as? Activity)?.recreate()
                                }
                                showLanguageDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}
