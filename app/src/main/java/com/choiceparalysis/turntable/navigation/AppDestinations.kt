package com.choiceparalysis.turntable.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbsUpDown
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
    val route: String,
) {
    SPIN_WHEEL("转盘", Icons.Default.Autorenew, "spin_wheel"),
    COIN_DICE("硬币骰子", Icons.Default.Casino, "coin_dice"),
    YES_NO("Yes/No", Icons.Default.ThumbsUpDown, "yes_no"),
    HISTORY("历史记录", Icons.Default.History, "history"),
    SETTINGS("设置", Icons.Default.Settings, "settings"),
}
