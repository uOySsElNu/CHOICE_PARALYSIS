package com.choiceparalysis.turntable.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
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
}

enum class BottomNavDestinations(
    val label: String,
    val icon: ImageVector,
    val route: Route,
) {
    HUB("首页", Icons.Default.Home, Route.Hub),
    HISTORY("历史记录", Icons.Default.History, Route.History),
    SETTINGS("设置", Icons.Default.Settings, Route.Settings),
}
