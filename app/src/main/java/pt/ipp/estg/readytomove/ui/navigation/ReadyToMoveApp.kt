package pt.ipp.estg.readytomove.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.runtime.LaunchedEffect
import pt.ipp.estg.readytomove.ui.screens.travel.TripDetailsScreen
import pt.ipp.estg.readytomove.viewmodel.TravelHistoryViewModel
import pt.ipp.estg.readytomove.ui.screens.home.LoginScreen
import pt.ipp.estg.readytomove.ui.screens.home.RegisterScreen
import pt.ipp.estg.readytomove.ui.screens.leaderboard.LeaderboardScreen
import pt.ipp.estg.readytomove.ui.screens.map.MapScreen
import pt.ipp.estg.readytomove.ui.screens.profile.ProfileScreen
import pt.ipp.estg.readytomove.ui.screens.travel.TravelHistoryScreen

@Composable
fun ReadyToMoveApp(
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    windowSizeClass: WindowSizeClass
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                viewModel = viewModel(),
                windowSizeClass = windowSizeClass,
                onLoginSuccess = { navController.navigate("map") },
                onRegisterClick = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterScreen(
                viewModel = viewModel(),
                windowSizeClass = windowSizeClass,
                onRegisterSuccess = { navController.navigate("map") },
                onLoginClick = { navController.navigate("login") }
            )
        }
        composable("map") {
            MapScreen(
                windowSizeClass = windowSizeClass,
                navController = navController,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )
        }
        composable("profile") { ProfileScreen(navController, windowSizeClass) }
        composable("leaderboard") { LeaderboardScreen(navController, windowSizeClass) }
        composable("travel_history") {
            val historyViewModel: TravelHistoryViewModel = viewModel()
            TravelHistoryScreen(
                navController,
                windowSizeClass,
                historyViewModel
            )
        }
        composable(
            route = "tripDetails/{tripId}",
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            val historyViewModel: TravelHistoryViewModel = viewModel()

            LaunchedEffect(Unit) {
                if (historyViewModel.trips.value.isEmpty()) historyViewModel.loadUserTrips()
            }

            TripDetailsScreen(
                navController = navController,
                tripId = tripId,
                viewModel = historyViewModel,
                windowSizeClass = windowSizeClass
            )
        }
    }
}