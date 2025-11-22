package pt.ipp.estg.readytomove.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.ipp.estg.readytomove.data.geoapify.GeoApifyDataSource
import pt.ipp.estg.readytomove.data.model.OperatorPoint
import pt.ipp.estg.readytomove.data.repository.GeoApifyRepository
import pt.ipp.estg.readytomove.data.repository.LocalRepository
import pt.ipp.estg.readytomove.data.retrofit.RetrofitHelper

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    // Repositórios e Auth
    private val localRepository = LocalRepository.getInstance(application)
    private val auth = FirebaseAuth.getInstance()

    // Estados de UI
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    private val _routePoints = MutableStateFlow<List<LatLng>>(emptyList())
    val routePoints: StateFlow<List<LatLng>> = _routePoints

    private val _operatorPoints = MutableStateFlow<List<OperatorPoint>>(emptyList())
    val operatorPoints: StateFlow<List<OperatorPoint>> = _operatorPoints

    // Variáveis de Controlo da Viagem
    private var isTracking = false
    private var tripStartTime: Long = 0
    private var locationCallback: LocationCallback? = null
    private var simulationJob: Job? = null

    // GeoApify (para os operadores)
    private val geoapifyDataSource = GeoApifyDataSource(
        retrofit = RetrofitHelper.retrofit("https://api.geoapify.com/"),
        apiKey = "18d9603dbce24498a3959b908acfa516"
    )
    private val repository = GeoApifyRepository(geoapifyDataSource)

    // Configuração do GPS (Atualiza a cada 4s)
    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
        .setMinUpdateIntervalMillis(2000)
        .build()

    // --- FUNÇÕES PRINCIPAIS ---

    // Iniciar Viagem REAL (GPS)
    @SuppressLint("MissingPermission")
    fun startTrip() {
        resetTripData()
        isTracking = true

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

    // Iniciar Viagem SIMULADA (Para testares no PC)
    fun startSimulatedTrip() {
        resetTripData()
        isTracking = true

        // Coordenadas iniciais (ex: perto da ESTG)
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

                // Simula movimento para Nordeste
                currentLat += 0.0001
                currentLng += 0.0001
                delay(1000) // Atualiza a cada segundo
            }
        }
    }

    // Parar Viagem e Guardar
    fun stopTrip() {
        if (!isTracking) return
        isTracking = false

        // Parar updates
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        simulationJob?.cancel()

        // 1. Calcular Distância
        val finalPoints = _routePoints.value
        val totalDistanceMeters = calculateDistance(finalPoints)
        val endTime = System.currentTimeMillis()
        val userId = auth.currentUser?.uid

        // 2. Guardar na BD (Se houver user e distância > 0)
        if (userId != null && totalDistanceMeters > 0) {
            viewModelScope.launch {
                localRepository.addTrip(
                    userId = userId,
                    startTime = tripStartTime,
                    endTime = endTime,
                    distanceMeters = totalDistanceMeters
                )
            }
        }

        // 3. Limpar a linha do mapa
        _routePoints.value = emptyList()
    }

    // --- FUNÇÕES AUXILIARES ---

    private fun resetTripData() {
        _routePoints.value = emptyList()
        tripStartTime = System.currentTimeMillis()
    }

    private fun updateLocation(location: Location) {
        _currentLocation.value = location

        // Se estivermos numa viagem, adiciona o ponto à lista para desenhar a linha
        if (isTracking) {
            val newPoint = LatLng(location.latitude, location.longitude)
            val currentList = _routePoints.value.toMutableList()
            currentList.add(newPoint)
            _routePoints.value = currentList
        }
    }

    private fun calculateDistance(points: List<LatLng>): Double {
        var totalDistance = 0.0
        if (points.size < 2) return 0.0

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val results = FloatArray(1)
            Location.distanceBetween(
                p1.latitude, p1.longitude,
                p2.latitude, p2.longitude,
                results
            )
            totalDistance += results[0]
        }
        return totalDistance
    }

    @SuppressLint("MissingPermission")
    fun getUserLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let { _currentLocation.value = it }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun loadOperators() {
        viewModelScope.launch {
            try {
                _operatorPoints.value = repository.getAllOperators()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}