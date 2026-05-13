package com.choiceparalysis.turntable.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.choiceparalysis.turntable.ui.coindice.CoinDiceScreen
import com.choiceparalysis.turntable.ui.history.HistoryScreen
import com.choiceparalysis.turntable.ui.home.SpinWheelScreen
import com.choiceparalysis.turntable.ui.settings.SettingsScreen
import com.choiceparalysis.turntable.ui.yesno.YesNoScreen

private val win10Easing = CubicBezierEasing(0.1f, 0.0f, 0.0f, 1.0f)

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AppDestinations.SPIN_WHEEL.route,
        modifier = modifier,
        enterTransition = { fadeIn(tween(200, easing = win10Easing)) },
        exitTransition = { fadeOut(tween(150, easing = win10Easing)) },
        popEnterTransition = { fadeIn(tween(200, easing = win10Easing)) },
        popExitTransition = { fadeOut(tween(150, easing = win10Easing)) },
    ) {
        composable(AppDestinations.SPIN_WHEEL.route) {
            SpinWheelScreen()
        }
        composable(AppDestinations.COIN_DICE.route) {
            CoinDiceScreen()
        }
        composable(AppDestinations.YES_NO.route) {
            YesNoScreen()
        }
        composable(AppDestinations.HISTORY.route) {
            HistoryScreen()
        }
        composable(AppDestinations.SETTINGS.route) {
            SettingsScreen()
        }
    }
}
