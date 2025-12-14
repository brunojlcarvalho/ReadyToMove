package pt.ipp.estg.readytomove.ui.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import pt.ipp.estg.readytomove.ui.screens.home.LoginScreen
import pt.ipp.estg.readytomove.ui.screens.home.RegisterScreen
import pt.ipp.estg.readytomove.ui.screens.leaderboard.LeaderboardScreen
import pt.ipp.estg.readytomove.ui.screens.map.MapScreen
import pt.ipp.estg.readytomove.ui.screens.profile.CameraXScreen
import pt.ipp.estg.readytomove.ui.screens.profile.ProfileScreen
import pt.ipp.estg.readytomove.ui.screens.travel.TravelHistoryScreen
import pt.ipp.estg.readytomove.ui.screens.travel.TripDetailsScreen
import pt.ipp.estg.readytomove.viewmodel.TravelHistoryViewModel

/**
 * The main Composable that acts as the entry point for the application's UI structure.
 *
 * It hosts the [NavHost] which manages the navigation graph, defining all available
 * screens (routes) and how to navigate between them. It also handles the initial
 * authentication check to determine the start screen.
 *
 * @param isDarkTheme Boolean state indicating if the app is currently in Dark Mode.
 * @param onToggleTheme Callback function to toggle between Dark and Light themes.
 * @param windowSizeClass The size class of the window, used for adaptive layouts (e.g., Tablet vs Phone).
 */
@Composable
fun ReadyToMoveApp(
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    windowSizeClass: WindowSizeClass
) {
    // 1. Create the NavController which manages app navigation stack
    val navController = rememberNavController()

    // 2. Determine Start Destination based on Auth State
    // If the user is already logged in (currentUser != null), we skip Login and go to Map.
    // Otherwise, we start at the Login screen.
    val currentUser = FirebaseAuth.getInstance().currentUser
    val startDestination = if (currentUser != null) "map" else "login"

    // 3. Define the Navigation Graph
    NavHost(navController = navController, startDestination = startDestination) {

        // --- AUTHENTICATION FLOW ---

        composable("login") {
            LoginScreen(
                viewModel = viewModel(),
                windowSizeClass = windowSizeClass,
                onLoginSuccess = {
                    // Navigate to Map and clear the backstack (inclusive)
                    // This prevents the user from going back to Login by pressing 'Back'
                    navController.navigate("map") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onRegisterClick = { navController.navigate("register") }
            )
        }

        composable("register") {
            RegisterScreen(
                viewModel = viewModel(),
                windowSizeClass = windowSizeClass,
                onRegisterSuccess = {
                    // On successful registration, go to Map and clear history
                    navController.navigate("map") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onLoginClick = { navController.navigate("login") }
            )
        }

        // --- MAIN APP FEATURES ---

        composable("map") {
            MapScreen(
                windowSizeClass = windowSizeClass,
                navController = navController,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )
        }

        composable("profile") {
            ProfileScreen(navController, windowSizeClass)
        }

        composable("leaderboard") {
            LeaderboardScreen(navController, windowSizeClass)
        }

        // --- HISTORY & DETAILS FLOW ---

        composable("travel_history") {
            // Note: This creates a ViewModel scoped to this specific back-stack entry.
            val historyViewModel: TravelHistoryViewModel = viewModel()
            TravelHistoryScreen(
                navController,
                windowSizeClass,
                historyViewModel
            )
        }

        // Dynamic Route: Expects a 'tripId' argument (e.g., "tripDetails/123")
        composable(
            route = "tripDetails/{tripId}",
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            // Extract the argument from the route
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""

            // We instantiate a new ViewModel here.
            // Since it's a new instance, we must check if data is loaded.
            val historyViewModel: TravelHistoryViewModel = viewModel()

            LaunchedEffect(Unit) {
                // If the list is empty (because it's a new VM), we force a reload from the repo
                if (historyViewModel.trips.value.isEmpty()) {
                    historyViewModel.loadUserTrips()
                }
            }

            TripDetailsScreen(
                navController = navController,
                tripId = tripId,
                viewModel = historyViewModel,
                windowSizeClass = windowSizeClass
            )
        }

        // --- CAMERA FEATURE ---

        composable("camera") {
            CameraXScreen(
                onPhotoTaken = { uriString ->
                    // PASSING DATA BACK:
                    // Instead of passing arguments forward, we save the result in the
                    // 'savedStateHandle' of the PREVIOUS entry (ProfileScreen).
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("avatar_result", uriString)

                    // Pop the Camera screen off the stack to return to Profile
                    navController.popBackStack()
                }
            )
        }
    }
}