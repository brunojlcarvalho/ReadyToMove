package pt.ipp.estg.readytomove.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import pt.ipp.estg.readytomove.MainActivity
import pt.ipp.estg.readytomove.R

/**
 * Service responsible for tracking the user's location in the background.
 *
 * This is a Foreground Service, meaning it displays a persistent notification
 * to the user while running. This ensures the Android system does not kill
 * the process when the app is minimized or the screen is turned off.
 *
 * It uses the FusedLocationProviderClient to obtain high-accuracy location updates
 * and broadcasts them to the rest of the application.
 */
class LocationService : Service() {

    // Main entry point for interacting with the fused location provider
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Callback for location events. Nullable to prevent crashes if stopped before start.
    private var locationCallback: LocationCallback? = null

    companion object {
        const val CHANNEL_ID = "travel_channel_id_v2"
        const val ACTION_LOCATION_UPDATE = "pt.ipp.estg.readytomove.LOCATION_UPDATE"
        const val EXTRA_LOCATION = "extra_location_data"
        private const val TAG = "LocationService"
    }

    /**
     * Called when the service is first created.
     * Initializes the location client and creates the necessary notification channel.
     */
    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        Log.d(TAG, "Service Created")
    }

    /**
     * Called by the system every time a client explicitly starts the service by calling startService(Intent).
     *
     * @param intent The Intent supplied to startService, containing the action (START/STOP).
     * @param flags Additional data about this start request.
     * @param startId A unique integer representing this specific request to start.
     * @return The return value indicates what semantics the system should use for the service's current started state.
     * We use START_STICKY to restart the service if the system kills it for memory.
     */
    @SuppressLint("MissingPermission") // Permission is checked before starting the service
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> {
                Log.d(TAG, "Received START command")
                startTracking()
            }
            "STOP" -> {
                Log.d(TAG, "Received STOP command")
                stopTracking()
            }
        }
        return START_STICKY
    }

    /**
     * Configures and starts the location tracking process.
     *
     * 1. Promotes the service to a Foreground Service (required for background execution).
     * 2. Builds a high-accuracy LocationRequest.
     * 3. Registers the LocationCallback to receive updates.
     *
     * @throws SecurityException if the required location permissions are missing.
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startTracking() {
        // 1. Start Foreground Service IMMEDIATELY (Android 12+ requirement)
        val notification = createNotification()
        startForeground(1, notification)
        Log.d(TAG, "startForeground called")

        // 2. Configure GPS Request
        // PRIORITY_HIGH_ACCURACY: Uses GPS + WiFi + Cell Towers
        // Intervals: Tries every 3s, but accepts up to every 1s if available
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        // 3. Define callback behavior
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    Log.d(TAG, "New Location: ${location.latitude}, ${location.longitude}")
                    sendLocationBroadcast(location)
                }
            }
        }

        // 4. Request updates
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e(TAG, "GPS Error: ${e.message}")
        }
    }

    /**
     * Stops the location tracking and destroys the service.
     *
     * This method removes location updates to conserve battery and removes the
     * persistent notification from the status bar.
     */
    private fun stopTracking() {
        Log.d(TAG, "Stopping tracking...")
        try {
            // Remove updates to save battery
            locationCallback?.let {
                fusedLocationClient.removeLocationUpdates(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Remove notification and destroy service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Broadcasts the new location to the rest of the application.
     *
     * @param location The new Location object received from the FusedLocationProvider.
     */
    private fun sendLocationBroadcast(location: Location) {
        val intent = Intent(ACTION_LOCATION_UPDATE)
        intent.putExtra(EXTRA_LOCATION, location)
        // SECURITY: setPackage ensures only our app receives this broadcast
        intent.setPackage(packageName)
        sendBroadcast(intent)
        Log.d(TAG, "Broadcast Sent")
    }

    /**
     * Creates the persistent notification required for the Foreground Service.
     *
     * @return A Notification object configured with title, text, and a pending intent
     * to open the app when clicked.
     */
    private fun createNotification(): Notification {
        // Create an Intent to open the MainActivity when notification is clicked
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_trip_active))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent) // Makes the notification clickable
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    /**
     * Creates a Notification Channel, required for Android 8.0 (Oreo) and above.
     * This groups notifications and allows the user to configure them in system settings.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_trip_active),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Not used for this Started Service.
     * @return null because we do not support binding.
     */
    override fun onBind(intent: Intent?): IBinder? = null
}