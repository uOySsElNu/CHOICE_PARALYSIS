package com.choiceparalysis.turntable.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.choiceparalysis.turntable.R
import kotlinx.serialization.Serializable

@Serializable
sealed class Route {
    @Serializable data object Hub : Route()
    @Serializable data object SpinWheel : Route()
    @Serializable data object Coin : Route()
    @Serializable data object Dice : Route()
    @Serializable data object YesNo : Route()
    @Serializable data object FingerRoulette : Route()
    @Serializable data object Stats : Route()
    @Serializable data object History : Route()
    @Serializable data object Settings : Route()
    @Serializable data object EasterEgg : Route()
}

enum class BottomNavDestinations(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val route: Route,
) {
    HUB(R.string.nav_home, Icons.Default.Home, Route.Hub),
    HISTORY(R.string.nav_history, Icons.Default.History, Route.History),
    SETTINGS(R.string.nav_settings, Icons.Default.Settings, Route.Settings),
}
