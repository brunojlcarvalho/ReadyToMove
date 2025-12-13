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

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val localRepository = LocalRepository.getInstance(application)
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // --- ESTADOS UI ---
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

    private var isTracking = false
    private var tripStartTime: Long = 0
    private var startPointType: OperatorType? = null

    private var locationCallback: LocationCallback? = null
    private var simulationJob: Job? = null

    private val geoapifyDataSource = GeoApifyDataSource(
        retrofit = RetrofitHelper.retrofit("https://api.geoapify.com/"),
        apiKey = "18d9603dbce24498a3959b908acfa516"
    )
    private val repository = GeoApifyRepository(geoapifyDataSource)

    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
        .setMinUpdateIntervalMillis(2000)
        .build()

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
        resetTripData()
        isTracking = true
        _isTrackingState.value = true
        _uiMessage.value = getApplication<Application>().getString(R.string.trip_started_at, nearestPoint.name)

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

        val intent = Intent(getApplication(), LocationService::class.java).apply {
            action = "STOP"
        }
        getApplication<Application>().startService(intent)

        startLocationUpdates()

        isTracking = false
        _isTrackingState.value = false
        simulationJob?.cancel()

        val finalPoints = _routePoints.value
        val totalDistanceMeters = calculateDistance(finalPoints)
        val endTime = System.currentTimeMillis()
        val userId = auth.currentUser?.uid
        val pointsEarned = (totalDistanceMeters / 100.0).toInt()

        if (userId != null) {
            viewModelScope.launch {
                localRepository.addTrip(userId, tripStartTime, endTime, totalDistanceMeters)
                try {
                    saveTripToFirebase(userId, tripStartTime, endTime, totalDistanceMeters, pointsEarned)
                } catch (e: Exception) { e.printStackTrace() }
            }
            _uiMessage.value = getApplication<Application>().getString(R.string.trip_finished_points, pointsEarned)
        }

        _routePoints.value = emptyList()
        startPointType = null
    }

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

    fun loadOperators(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                _operatorPoints.value = repository.getAllOperators(lat, lon)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun saveTripToFirebase(userId: String, start: Long, end: Long, distance: Double, points: Int) {
        val tripData = hashMapOf("userId" to userId, "startTime" to start, "endTime" to end, "distanceMeters" to distance, "pointsEarned" to points, "date" to Date(start))
        db.collection("trips").add(tripData)
        db.collection("users").document(userId).update("totalTrips", FieldValue.increment(1), "points", FieldValue.increment(points.toLong()))
    }

    private fun resetTripData() {
        _routePoints.value = emptyList()
        tripStartTime = System.currentTimeMillis()
    }

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

    override fun onCleared() {
        super.onCleared()
        stopLocalLocationUpdates()
        try { getApplication<Application>().unregisterReceiver(locationReceiver) } catch (e: Exception) {}
    }
}