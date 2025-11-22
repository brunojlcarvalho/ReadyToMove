package pt.ipp.estg.readytomove.viewmodel

import android.app.Application
import android.location.Location
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.ipp.estg.readytomove.data.geoapify.GeoApifyDataSource
import pt.ipp.estg.readytomove.data.model.OperatorPoint
import pt.ipp.estg.readytomove.data.repository.GeoApifyRepository
import pt.ipp.estg.readytomove.data.retrofit.RetrofitHelper

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    // Current user location (updates in real-time)
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    // List of points to draw the polyline on the map
    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints: StateFlow<List<LatLng>> = _routePoints

    // Tracking state
    private var isTracking = false
    private var locationCallback: LocationCallback? = null

    // Simulation Job
    private var simulationJob: Job? = null

    private val _operatorPoints = MutableStateFlow<List<OperatorPoint>>(emptyList())
    val operatorPoints: StateFlow<List<OperatorPoint>> = _operatorPoints

    private val geoapifyDataSource = GeoApifyDataSource(
        retrofit = RetrofitHelper.retrofit("https://api.geoapify.com/"),
        apiKey = "YOUR_API_KEY_HERE" // Careful with exposing keys
    )
    private val repository = GeoApifyRepository(geoapifyDataSource)

    // --- REAL GPS LOGIC ---

    // Config for location updates (high accuracy, update every 4 seconds)
    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
        .setMinUpdateIntervalMillis(2000)
        .build()

    /**
     * Starts tracking the user's location via GPS.
     */
    fun startTrip() {
        isTracking = true
        _routePoints.value = emptyList() // Clear previous route

        // Setup the callback to receive updates
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
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Stops tracking and saves the trip (logic to save to Room would go here).
     */
    fun stopTrip() {
        isTracking = false
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        simulationJob?.cancel()

        _routePoints.value = emptyList()
    }

    private fun updateLocation(location: Location) {
        _currentLocation.value = location

        // If we are recording a trip, add points to the list
        if (isTracking) {
            val newPoint = LatLng(location.latitude, location.longitude)
            val currentList = _routePoints.value.toMutableList()
            currentList.add(newPoint)
            _routePoints.value = currentList
        }
    }

    // --- SIMULATION LOGIC (For testing without moving) ---

    /**
     * Simulates movement starting from a specific point.
     * Useful for testing in emulator.
     */
    fun startSimulatedTrip() {
        isTracking = true
        _routePoints.value = emptyList()

        // Start near IPP/ESTG coordinates approximately
        var currentLat = 41.366
        var currentLng = -8.195

        simulationJob = viewModelScope.launch {
            while (isTracking) {
                // Create a fake location object
                val mockLocation = Location("mock").apply {
                    latitude = currentLat
                    longitude = currentLng
                    accuracy = 5f
                }

                updateLocation(mockLocation)

                // Move slightly North-East
                currentLat += 0.0001
                currentLng += 0.0001

                delay(1000) // Update every second
            }
        }
    }

    // --- INITIAL DATA ---

    fun loadOperators() {
        viewModelScope.launch {
            try {
                _operatorPoints.value = repository.getAllOperators()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    // Initial single fetch (optional now that we have updates)
    fun getUserLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let { _currentLocation.value = it }
            }
        } catch (e: SecurityException) { e.printStackTrace() }
    }
}