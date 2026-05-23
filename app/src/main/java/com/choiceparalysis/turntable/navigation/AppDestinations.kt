package com.choiceparalysis.turntable.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomNavDestinations(
    val label: String,
    val icon: ImageVector,
    val route: String,
) {
    HUB("首页", Icons.Default.Home, "hub"),
    HISTORY("历史记录", Icons.Default.History, "history"),
    SETTINGS("设置", Icons.Default.Settings, "settings"),
}

object Routes {
    const val HUB = "hub"
    const val SPIN_WHEEL = "spin_wheel"
    const val COIN = "coin"
    const val DICE = "dice"
    const val YES_NO = "yes_no"
    const val FINGER_ROULETTE = "finger_roulette"
    const val STATS = "stats"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}
