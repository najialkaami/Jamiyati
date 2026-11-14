package com.roma.jamiyati.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.roma.jamiyati.data.repository.JamiyatiRepository
import com.roma.jamiyati.ui.screens.CircleDetailScreen
import com.roma.jamiyati.ui.screens.CircleEditorScreen
import com.roma.jamiyati.ui.screens.HomeScreen
import com.roma.jamiyati.ui.screens.RoundPaymentsScreen
import com.roma.jamiyati.ui.screens.SettingsScreen
import com.roma.jamiyati.ui.viewmodel.CircleDetailViewModel
import com.roma.jamiyati.ui.viewmodel.CircleEditorViewModel
import com.roma.jamiyati.ui.viewmodel.HomeViewModel
import com.roma.jamiyati.ui.viewmodel.SettingsViewModel

sealed class Routes(val route: String) {
    data object Home : Routes("home")
    data object Settings : Routes("settings")
    data object CreateCircle : Routes("circle/new")
    data object EditCircle : Routes("circle/edit/{circleId}") {
        fun build(circleId: Long) = "circle/edit/$circleId"
    }
    data object CircleDetail : Routes("circle/detail/{circleId}") {
        fun build(circleId: Long) = "circle/detail/$circleId"
    }
    data object RoundPayments : Routes("circle/{circleId}/round/{roundNumber}") {
        fun build(circleId: Long, roundNumber: Int) = "circle/$circleId/round/$roundNumber"
    }
}

@Composable
fun JamiyatiNavGraph(repository: JamiyatiRepository) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Home.route
    ) {
        composable(Routes.Home.route) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.Factory(repository)
            )
            HomeScreen(
                viewModel = viewModel,
                onOpenCircle = { id -> navController.navigate(Routes.CircleDetail.build(id)) },
                onCreateCircle = { navController.navigate(Routes.CreateCircle.route) },
                onEditCircle = { id -> navController.navigate(Routes.EditCircle.build(id)) },
                onOpenSettings = { navController.navigate(Routes.Settings.route) }
            )
        }

        composable(Routes.CreateCircle.route) {
            val viewModel: CircleEditorViewModel = viewModel(
                factory = CircleEditorViewModel.Factory(repository, circleId = null)
            )
            CircleEditorScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaved = { savedId ->
                    navController.popBackStack()
                    navController.navigate(Routes.CircleDetail.build(savedId))
                },
                onDeleted = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EditCircle.route,
            arguments = listOf(navArgument("circleId") { type = NavType.LongType })
        ) { entry ->
            val circleId = entry.arguments?.getLong("circleId") ?: 0L
            val viewModel: CircleEditorViewModel = viewModel(
                factory = CircleEditorViewModel.Factory(repository, circleId = circleId)
            )
            CircleEditorScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                onDeleted = {
                    navController.popBackStack(Routes.Home.route, inclusive = false)
                }
            )
        }

        composable(
            route = Routes.CircleDetail.route,
            arguments = listOf(navArgument("circleId") { type = NavType.LongType })
        ) { entry ->
            val circleId = entry.arguments?.getLong("circleId") ?: 0L
            val viewModel: CircleDetailViewModel = viewModel(
                factory = CircleDetailViewModel.Factory(repository, circleId)
            )
            CircleDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEditCircle = { navController.navigate(Routes.EditCircle.build(circleId)) },
                onOpenRound = { roundNum ->
                    navController.navigate(Routes.RoundPayments.build(circleId, roundNum))
                }
            )
        }

        composable(
            route = Routes.RoundPayments.route,
            arguments = listOf(
                navArgument("circleId") { type = NavType.LongType },
                navArgument("roundNumber") { type = NavType.IntType }
            )
        ) { entry ->
            val circleId = entry.arguments?.getLong("circleId") ?: 0L
            val roundNumber = entry.arguments?.getInt("roundNumber") ?: 1
            val viewModel: CircleDetailViewModel = viewModel(
                factory = CircleDetailViewModel.Factory(repository, circleId)
            )
            RoundPaymentsScreen(
                viewModel = viewModel,
                roundNumber = roundNumber,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.Settings.route) {
            val viewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(repository)
            )
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
