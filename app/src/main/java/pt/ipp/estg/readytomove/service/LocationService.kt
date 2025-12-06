package pt.ipp.estg.readytomove.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import pt.ipp.estg.readytomove.R

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    companion object {
        const val CHANNEL_ID = "travel_channel_id_v2" // Mudei o ID para forçar recriação
        const val ACTION_LOCATION_UPDATE = "pt.ipp.estg.readytomove.LOCATION_UPDATE" // String ÚNICA
        const val EXTRA_LOCATION = "extra_location_data"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        Log.d("LocationService", "Serviço Criado")
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> {
                Log.d("LocationService", "Recebido comando START")
                startTracking()
            }
            "STOP" -> {
                Log.d("LocationService", "Recebido comando STOP")
                stopTracking()
            }
        }
        return START_STICKY
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun startTracking() {
        val notification = createNotification()
        startForeground(1, notification)
        Log.d("LocationService", "startForeground chamado")

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
            .setMinUpdateIntervalMillis(1000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    // Log para ver se o GPS está a funcionar
                    Log.d("LocationService", "Nova Localização: ${location.latitude}, ${location.longitude}")
                    sendLocationBroadcast(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e("LocationService", "Erro GPS: ${e.message}")
        }
    }

    private fun stopTracking() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) { e.printStackTrace() }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun sendLocationBroadcast(location: Location) {
        val intent = Intent(ACTION_LOCATION_UPDATE)
        intent.putExtra(EXTRA_LOCATION, location)
        intent.setPackage(packageName)
        sendBroadcast(intent)
        Log.d("LocationService", "Broadcast Enviado")
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ReadyToMove")
            .setContentText("A viajar...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Viagem Ativa",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}