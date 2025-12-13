package pt.ipp.estg.readytomove

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import pt.ipp.estg.readytomove.ui.navigation.ReadyToMoveApp
import pt.ipp.estg.readytomove.ui.theme.ReadyToMoveTheme
import pt.ipp.estg.readytomove.util.NotificationHelper
import pt.ipp.estg.readytomove.worker.LeaderboardWorker
import java.util.concurrent.TimeUnit

/**
 * The main entry point of the application.
 *
 * This Activity is responsible for:
 * - Initializing global configurations (Firebase, Notification Channels).
 * - Setting up background workers (Leaderboard checks).
 * - Calculating window size classes for adaptive UI.
 * - Setting the content view to the Compose hierarchy via [ReadyToMoveApp].
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {

    // State to manage the application theme (Dark/Light mode)
    private var isDarkTheme by mutableStateOf(false)

    /**
     * Called when the activity is starting.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     * this Bundle contains the data it most recently supplied in onSaveInstanceState.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Notification Channels for the app
        NotificationHelper.createNotificationChannel(this)

        // Initialize Firebase and App Check (using Debug provider for development)
        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        // Schedule periodic background tasks
        setupBackgroundWorker()

        setContent {
            // Calculate the window size class to support adaptive layouts (Phone vs Tablet)
            val windowSizeClass = calculateWindowSizeClass(this)

            ReadyToMoveTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReadyToMoveApp(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = it },
                        windowSizeClass = windowSizeClass
                    )
                }
            }
        }
    }

    /**
     * Configures and enqueues the [LeaderboardWorker].
     *
     * This sets up a periodic task that runs approximately every 15 minutes
     * to check the user's leaderboard status in the background.
     * It uses [ExistingPeriodicWorkPolicy.KEEP] to ensure we don't duplicate the request
     * if it's already scheduled.
     */
    private fun setupBackgroundWorker() {
        val leaderboardWorkRequest = PeriodicWorkRequestBuilder<LeaderboardWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "LeaderboardCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            leaderboardWorkRequest
        )
    }
}