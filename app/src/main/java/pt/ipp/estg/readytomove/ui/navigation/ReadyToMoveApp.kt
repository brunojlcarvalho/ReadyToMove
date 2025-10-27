package pt.ipp.estg.readytomove.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pt.ipp.estg.readytomove.ui.screens.home.LoginScreen
import pt.ipp.estg.readytomove.ui.screens.home.RegisterScreen
import pt.ipp.estg.readytomove.ui.screens.leaderboard.LeaderboardScreen
import pt.ipp.estg.readytomove.ui.screens.map.MapScreen
import pt.ipp.estg.readytomove.ui.screens.profile.ProfileScreen
import pt.ipp.estg.readytomove.ui.screens.travel.TravelHistoryScreen

@Composable
fun ReadyToMoveApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                viewModel(),
                onLoginSuccess = { navController.navigate("map") },
                onRegisterClick = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterScreen(
                viewModel(),
                onRegisterSuccess = { navController.navigate("map") },
                onLoginClick = { navController.navigate("login") }
            )
        }
        composable("map") { MapScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
        composable("leaderboard") { LeaderboardScreen(navController) }
        composable("travel_history") { TravelHistoryScreen(navController) }
    }
}

