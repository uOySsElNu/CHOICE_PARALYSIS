package com.choiceparalysis.turntable

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.choiceparalysis.turntable.data.repository.SettingsRepository
import com.choiceparalysis.turntable.navigation.AppNavGraph
import com.choiceparalysis.turntable.navigation.BottomNavDestinations
import com.choiceparalysis.turntable.ui.theme.CHOICEPARALYSISTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Apply saved locale before rendering UI (runBlocking is acceptable here —
        // this is a one-shot DataStore read that must complete before setContent)
        val savedLocale = runBlocking { settingsRepository.appLocale.first() }
        if (savedLocale == "system") {
            // Get the REAL system locale (Resources.getSystem() bypasses app-overridden config)
            val sysLocale = Resources.getSystem().configuration.locales[0]
            Locale.setDefault(sysLocale)
            val config = Configuration(Resources.getSystem().configuration)
            config.setLocale(sysLocale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        } else {
            val locale = Locale.forLanguageTag(savedLocale)
            Locale.setDefault(locale)
            val config = Configuration(resources.configuration)
            config.setLocale(locale)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(config, resources.displayMetrics)
        }

        setContent {
            val followSystem by settingsRepository.followSystemTheme.collectAsState(initial = true)
            val manualDark by settingsRepository.darkMode.collectAsState(initial = false)
            val darkTheme = if (followSystem) isSystemInDarkTheme() else manualDark
            CHOICEPARALYSISTheme(darkTheme = darkTheme) {
                ChoiceParalysisMainScreen()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun ChoiceParalysisMainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            BottomNavDestinations.entries.forEach { dest ->
                item(
                    icon = { Icon(dest.icon, contentDescription = stringResource(dest.labelRes)) },
                    label = { Text(stringResource(dest.labelRes)) },
                    selected = currentDestination?.hasRoute(dest.route::class) == true,
                    onClick = {
                        if (currentDestination?.hasRoute(dest.route::class) != true) {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            AppNavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
