package pt.ipp.estg.readytomove.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.ipp.estg.readytomove.data.geoapify.GeoApifyDataSource
import pt.ipp.estg.readytomove.data.model.OperatorPoint
import pt.ipp.estg.readytomove.data.model.OperatorType
import pt.ipp.estg.readytomove.data.repository.GeoApifyRepository
import pt.ipp.estg.readytomove.data.repository.LocalRepository
import pt.ipp.estg.readytomove.data.retrofit.RetrofitHelper
import pt.ipp.estg.readytomove.service.LocationService
import pt.ipp.estg.readytomove.R
import java.util.Date

/**
 * ViewModel for the Map screen.
 *
 * It handles:
 * - User location tracking via FusedLocationProviderClient.
 * - Managing active trips (start, stop, calculate distance).
 * - Fetching nearby operator points (bus stops, bike stations) from GeoApify.
 * - Communicating with the foreground LocationService.
 *
 * @param application The application context.
 */
class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val localRepository = LocalRepository.getInstance(application)
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // StateFlows for UI state
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints: StateFlow<List<LatLng>> = _routePoints

    private val _operatorPoints = MutableStateFlow<List<OperatorPoint>>(emptyList())
    val operatorPoints: StateFlow<List<OperatorPoint>> = _operatorPoints

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage

    private val _isTrackingState = MutableStateFlow(false)
    val isTrackingState: StateFlow<Boolean> = _isTrackingState

    // Internal state variables
    private var isTracking = false
    private var tripStartTime: Long = 0
    private var startPointType: OperatorType? = null
    private var startPointName: String? = null
    private var locationCallback: LocationCallback? = null
    private var simulationJob: Job? = null

    // GeoApify API setup
    private val geoapifyDataSource = GeoApifyDataSource(
        retrofit = RetrofitHelper.retrofit("https://api.geoapify.com/"),
        apiKey = "18d9603dbce24498a3959b908acfa516"
    )
    private val repository = GeoApifyRepository(geoapifyDataSource)

    // Location request configuration
    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
        .setMinUpdateIntervalMillis(2000)
        .build()

    // BroadcastReceiver to get updates from the background LocationService
    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationService.ACTION_LOCATION_UPDATE) {
                val location = intent.getParcelableExtra<Location>(LocationService.EXTRA_LOCATION)
                if (location != null) {
                    Log.d("MapViewModel", "Received location from service! Updating UI.")
                    updateLocation(location)
                } else {
                    Log.e("MapViewModel", "Received null location from service!")
                }
            }
        }
    }

    init {
        // Register the BroadcastReceiver
        val filter = IntentFilter(LocationService.ACTION_LOCATION_UPDATE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                application.registerReceiver(locationReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } catch (e: Exception) {
                ContextCompat.registerReceiver(
                    application,
                    locationReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }
        } else {
            ContextCompat.registerReceiver(
                application,
                locationReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    /**
     * Starts requesting location updates directly using FusedLocationProviderClient.
     * This is typically used when not in an active trip to show user location on the map.
     */
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (locationCallback != null) return

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    updateLocation(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: Exception) { e.printStackTrace() }
    }

    /**
     * Initiates a trip.
     *
     * Validates that the user has a GPS signal and is near a valid operator point (Bus/Bike).
     * If valid, it starts the foreground [LocationService] and updates the tracking state.
     */
    fun startTrip() {
        val currentLoc = _currentLocation.value

        if (currentLoc == null) {
            _uiMessage.value = getApplication<Application>().getString(R.string.waiting_gps)
            return
        }

        val nearestPoint = findNearestOperator(currentLoc, 10.0)
        if (nearestPoint == null) {
            _uiMessage.value = getApplication<Application>().getString(R.string.error_not_near_point)
            return
        }

        startPointType = nearestPoint.type
        startPointName = nearestPoint.name

        resetTripData()
        isTracking = true
        _isTrackingState.value = true
        _uiMessage.value = getApplication<Application>().getString(R.string.trip_started_at, nearestPoint.name)

        // Stop local updates as the service will now handle them
        stopLocalLocationUpdates()

        val intent = Intent(getApplication(), LocationService::class.java).apply {
            action = "START"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }

    /**
     * Stops the current trip.
     *
     * Validates that the user is near a valid operator point of the same type as the start point.
     * Calculates the total distance and points, saves the trip locally and to Firestore,
     * and stops the foreground service.
     */
    fun stopTrip() {
        if (!isTracking) return

        val currentLoc = _currentLocation.value
        if (currentLoc == null) {
            _uiMessage.value = getApplication<Application>().getString(R.string.error_no_gps)
            return
        }

        val nearestPoint = findNearestOperator(currentLoc, 10.0)
        if (nearestPoint == null) {
            _uiMessage.value = getApplication<Application>().getString(R.string.error_end_trip_near_point)
            return
        }

        if (startPointType != null && nearestPoint.type != startPointType) {
            _uiMessage.value = getApplication<Application>().getString(R.string.error_wrong_vehicle_type, startPointType?.name)
            return
        }

        val endPointName = nearestPoint.name

        // Stop the foreground service
        val intent = Intent(getApplication(), LocationService::class.java).apply {
            action = "STOP"
        }
        getApplication<Application>().startService(intent)

        // Resume local updates
        startLocationUpdates()

        isTracking = false
        _isTrackingState.value = false
        simulationJob?.cancel()

        // Calculate final trip details
        val finalPoints = _routePoints.value
        val routeString = finalPoints.joinToString(";") { "${it.latitude},${it.longitude}" }
        val totalDistanceMeters = calculateDistance(finalPoints)
        val endTime = System.currentTimeMillis()
        val userId = auth.currentUser?.uid
        val pointsEarned = (totalDistanceMeters / 100.0).toInt()

        if (userId != null) {
            viewModelScope.launch {
                localRepository.addTrip(userId, tripStartTime, endTime, totalDistanceMeters, startPointName, endPointName, routeString)
                try {
                    saveTripToFirebase(userId, tripStartTime, endTime, totalDistanceMeters, pointsEarned, startPointName, endPointName, routeString)
                } catch (e: Exception) { e.printStackTrace() }
            }
            _uiMessage.value = getApplication<Application>().getString(R.string.trip_finished_points, pointsEarned)
        }

        _routePoints.value = emptyList()
        startPointType = null
    }

    /**
     * Starts a simulated trip for testing purposes without moving physically.
     */
    fun startSimulatedTrip() {
        stopLocalLocationUpdates()

        resetTripData()
        isTracking = true
        _isTrackingState.value = true
        startPointType = OperatorType.BUS

        var currentLat = 41.366
        var currentLng = -8.195

        simulationJob = viewModelScope.launch {
            while (isTracking) {
                val mockLocation = Location("mock").apply {
                    latitude = currentLat
                    longitude = currentLng
                    accuracy = 5f
                    time = System.currentTimeMillis()
                }
                updateLocation(mockLocation)
                currentLat += 0.0001
                currentLng += 0.0001
                delay(1000)
            }
        }
    }

    private fun stopLocalLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    private fun updateLocation(location: Location) {
        _currentLocation.value = location
        // Refresh nearby operators based on new location
        loadOperators(location.latitude, location.longitude)

        if (isTracking) {
            val newPoint = LatLng(location.latitude, location.longitude)
            val currentList = _routePoints.value.toMutableList()
            currentList.add(newPoint)
            _routePoints.value = currentList
        }
    }

    private fun findNearestOperator(currentLoc: Location, maxDistanceMeters: Double): OperatorPoint? {
        val allPoints = _operatorPoints.value
        var closestPoint: OperatorPoint? = null
        var minDistance = Float.MAX_VALUE

        for (point in allPoints) {
            val pointLoc = Location("point").apply { latitude = point.lat; longitude = point.lon }
            val distance = currentLoc.distanceTo(pointLoc)
            if (distance < minDistance) {
                minDistance = distance
                closestPoint = point
            }
        }
        return if (minDistance <= maxDistanceMeters) closestPoint else null
    }

    fun clearUiMessage() { _uiMessage.value = null }

    /**
     * Fetches nearby operator points from the repository.
     */
    fun loadOperators(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                _operatorPoints.value = repository.getAllOperators(lat, lon)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    /**
     * Saves the trip data to Firebase Firestore.
     */
    private fun saveTripToFirebase(userId: String, start: Long, end: Long, distance: Double, points: Int, startLoc: String?, endLoc: String, routePath: String) {
        val tripData = hashMapOf("userId" to userId, "startTime" to start, "endTime" to end, "distanceMeters" to distance, "pointsEarned" to points, "date" to Date(start),
            "startLocation" to startLoc, "endLocation" to endLoc, "routePath" to routePath)
        db.collection("trips").add(tripData)
        // Increment user stats atomically
        db.collection("users").document(userId).update("totalTrips", FieldValue.increment(1), "points", FieldValue.increment(points.toLong()))
    }

    /**
     * Resets trip-related data for a new trip.
     */
    private fun resetTripData() {
        _routePoints.value = emptyList()
        tripStartTime = System.currentTimeMillis()
    }

    /**
     * Calculates the total distance of a route defined by a list of LatLng points.
     *
     * @param points The list of LatLng points representing the route.
     * @return The total distance in meters.
     */
    private fun calculateDistance(points: List<LatLng>): Double {
        var totalDistance = 0.0
        if (points.size < 2) return 0.0
        for (i in 0 until points.size - 1) {
            val p1 = points[i]; val p2 = points[i + 1]; val results = FloatArray(1)
            Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, results)
            totalDistance += results[0]
        }
        return totalDistance
    }

    /**
     * Cleans up resources when the ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        stopLocalLocationUpdates()
        try { getApplication<Application>().unregisterReceiver(locationReceiver) } catch (e: Exception) {}
    }

    /**
     * Starts a trip initiated by a QR Code scan.
     *
     * @param qrContent The raw content of the scanned QR Code (expected to be "BUS" or "BIKE").
     */
    fun startTripWithQr(qrContent: String) {
        val currentLoc = _currentLocation.value

        if (currentLoc == null) {
            _uiMessage.value = getApplication<Application>().getString(R.string.waiting_gps)
            return
        }

        // Assume QR Code contains only "BUS" or "BIKE"
        val detectedType = try {
            // Convert to uppercase and try to match Enum OperatorType
            OperatorType.valueOf(qrContent.uppercase())
        } catch (e: Exception) {
            _uiMessage.value = "Invalid QR Code: Unknown vehicle type ($qrContent)."
            return
        }

        // Check if we are near a point (10m radius)
        val nearestPoint = findNearestOperator(currentLoc, 10.0)

        if (nearestPoint == null) {
            _uiMessage.value = "You must be within 10m of a point to use this QR Code!"
            return
        }

        // Validate if the QR Code matches the nearby point type
        if (nearestPoint.type != detectedType) {
            _uiMessage.value = "Wrong QR Code! You are at a ${nearestPoint.type} point, but the code is for $detectedType."
            return
        }

        // All checks passed -> Start Trip
        startPointType = detectedType
        startPointName = nearestPoint.name
        resetTripData()
        isTracking = true
        _isTrackingState.value = true
        _uiMessage.value = "Vehicle unlocked: ${detectedType.name}. Enjoy your trip!"

        stopLocalLocationUpdates()

        val intent = Intent(getApplication(), LocationService::class.java).apply {
            action = "START"
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            getApplication<Application>().startForegroundService(intent)
        } else {
            getApplication<Application>().startService(intent)
        }
    }
}