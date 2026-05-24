package com.choiceparalysis.turntable.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.choiceparalysis.turntable.ui.coin.CoinScreen
import com.choiceparalysis.turntable.ui.components.StandardEasing
import com.choiceparalysis.turntable.ui.dice.DiceScreen
import com.choiceparalysis.turntable.ui.fingerroulette.FingerRouletteScreen
import com.choiceparalysis.turntable.ui.history.HistoryScreen
import com.choiceparalysis.turntable.ui.home.SpinWheelScreen
import com.choiceparalysis.turntable.ui.hub.HubScreen
import com.choiceparalysis.turntable.ui.settings.SettingsScreen
import com.choiceparalysis.turntable.ui.stats.StatsScreen
import com.choiceparalysis.turntable.ui.yesno.YesNoScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HUB,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart)) +
                scaleIn(initialScale = 0.5f, animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart)) +
                scaleOut(targetScale = 1.5f, animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart)) +
                scaleIn(initialScale = 0.5f, animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart)) +
                scaleOut(targetScale = 1.5f, animationSpec = tween(500, easing = StandardEasing.EaseInOutQuart))
        },
    ) {
        composable(Routes.HUB) {
            HubScreen(onNavigate = { route ->
                navController.navigate(route)
            })
        }
        composable(Routes.SPIN_WHEEL) {
            SpinWheelScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.COIN) {
            CoinScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.DICE) {
            DiceScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.YES_NO) {
            YesNoScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.FINGER_ROULETTE) {
            FingerRouletteScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.STATS) {
            StatsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.HISTORY) {
            HistoryScreen()
        }
        composable(Routes.SETTINGS) {
            SettingsScreen()
        }
    }
}
