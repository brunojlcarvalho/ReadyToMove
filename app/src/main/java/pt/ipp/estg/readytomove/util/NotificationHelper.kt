package pt.ipp.estg.readytomove.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import pt.ipp.estg.readytomove.R
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Helper object to manage application notifications.
 *
 * It handles the creation of notification channels (required for Android O+) and
 * the display of specific notifications, such as leaderboard rank changes.
 */
object NotificationHelper {
    const val CHANNEL_ID_LEADERBOARD = "leaderboard_channel"

    /**
     * Creates the notification channel for leaderboard updates..
     *
     * @param context The application context.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Leaderboard Updates"
            val descriptionText = "Notifications about changes in ranking"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID_LEADERBOARD, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Displays a notification informing the user that their rank has dropped.
     *
     * @param context The application context.
     * @param oldRank The user's previous rank.
     * @param newRank The user's new (lower) rank.
     */
    fun showRankDropNotification(context: Context, oldRank: Int, newRank: Int) {
        // Check for POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_LEADERBOARD)
            .setSmallIcon(R.drawable.ic_scooter)
            .setContentTitle("You lost a position!")
            .setContentText("You dropped from #$oldRank to #$newRank. Go for a ride to recover!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            // notifyId is hardcoded to 1001 for simplicity; could be unique per event type
            notify(1001, builder.build())
        }
    }
}