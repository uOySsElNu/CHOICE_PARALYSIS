package com.choiceparalysis.turntable.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.choiceparalysis.turntable.ui.coindice.CoinDiceScreen
import com.choiceparalysis.turntable.ui.history.HistoryScreen
import com.choiceparalysis.turntable.ui.home.SpinWheelScreen
import com.choiceparalysis.turntable.ui.lists.ListsScreen
import com.choiceparalysis.turntable.ui.yesno.YesNoScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AppDestinations.SPIN_WHEEL.route,
        modifier = modifier
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
        composable(AppDestinations.LISTS.route) {
            ListsScreen()
        }
        composable(AppDestinations.HISTORY.route) {
            HistoryScreen()
        }
    }
}
