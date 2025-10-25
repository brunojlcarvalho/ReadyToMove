package pt.ipp.estg.readytomove.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pt.ipp.estg.readytomove.ui.screens.home.LoginScreen
import pt.ipp.estg.readytomove.ui.screens.leaderboard.LeaderboardScreen
import pt.ipp.estg.readytomove.ui.screens.map.MapScreen
import pt.ipp.estg.readytomove.ui.screens.profile.ProfileScreen
import pt.ipp.estg.readytomove.ui.screens.travel.TravelHistoryScreen

@Composable
fun ReadyToMoveApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("map") { MapScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
        composable("leaderboard") { LeaderboardScreen(navController) }
        composable("travel_history") { TravelHistoryScreen(navController) }
    }
}
