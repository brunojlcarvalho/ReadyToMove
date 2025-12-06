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

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {
    private var isDarkTheme by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)
        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
        setupBackgroundWorker()

        setContent {
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