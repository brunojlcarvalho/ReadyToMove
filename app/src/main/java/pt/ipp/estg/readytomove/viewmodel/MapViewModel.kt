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

    private val _isLowLight = MutableStateFlow(false)
    val isLowLight: StateFlow<Boolean> = _isLowLight

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage

    private val _isTrackingState = MutableStateFlow(false)
    val isTrackingState: StateFlow<Boolean> = _isTrackingState

    // --- VARIÁVEIS DE CONTROLO ---
    private var isTracking = false // Controla apenas se estamos a GRAVAR a rota
    private var tripStartTime: Long = 0
    private var startPointType: OperatorType? = null

    // Callback de localização persistente
    private var locationCallback: LocationCallback? = null

    private var simulationJob: Job? = null

    // --- REPOSITÓRIOS ---
    private val geoapifyDataSource = GeoApifyDataSource(
        retrofit = RetrofitHelper.retrofit("https://api.geoapify.com/"),
        apiKey = "18d9603dbce24498a3959b908acfa516"
    )
    private val repository = GeoApifyRepository(geoapifyDataSource)

    // Configuração GPS: Alta precisão
    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
        .setMinUpdateIntervalMillis(2000)
        .build()

    // --- NOVA FUNÇÃO: Iniciar GPS (Independente da Viagem) ---
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (locationCallback != null) return // Já está a correr

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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- FUNÇÕES DE VIAGEM ---

    fun startTrip() {
        val currentLoc = _currentLocation.value

        if (currentLoc == null) {
            _uiMessage.value = "A aguardar localização GPS..."
            return
        }

        val nearestPoint = findNearestOperator(currentLoc, 10.0)

        if (nearestPoint == null) {
            _uiMessage.value = "Tens de estar a 10m de um ponto (Bus/Bike) para iniciar!"
            return
        }

        // Tudo OK -> Iniciar Gravação
        startPointType = nearestPoint.type
        resetTripData()

        isTracking = true // Começa a gravar os pontos
        _isTrackingState.value = true
        _uiMessage.value = "Viagem iniciada em: ${nearestPoint.name}"

        // NOTA: Removemos daqui o requestLocationUpdates porque ele já deve estar a correr
    }

    fun startSimulatedTrip() {
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

    fun stopTrip() {
        if (!isTracking) return

        val currentLoc = _currentLocation.value

        if (currentLoc == null) {
            _uiMessage.value = "Sem GPS."
            return
        }

        val nearestPoint = findNearestOperator(currentLoc, 10.0)

        if (nearestPoint == null) {
            _uiMessage.value = "Tens de estar a 10m de um ponto para terminar a viagem!"
            return
        }

        if (startPointType != null && nearestPoint.type != startPointType) {
            _uiMessage.value = "Iniciaste em ${startPointType?.name}, tens de acabar num igual!"
            return
        }

        // Parar Gravação
        isTracking = false
        _isTrackingState.value = false
        simulationJob?.cancel()

        // NOTA: NÃO paramos o fusedLocationClient aqui. O GPS continua ligado para o boneco andar.

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
            _uiMessage.value = "Viagem terminada! Ganhaste $pointsEarned pontos."
        }

        _routePoints.value = emptyList()
        startPointType = null
    }

    // Limpar GPS quando o ViewModel morre (ex: fechar app)
    override fun onCleared() {
        super.onCleared()
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
    }

    // --- FUNÇÕES AUXILIARES ---

    private fun updateLocation(location: Location) {
        // 1. Atualizar SEMPRE a localização atual (Boneco mexe-se sempre)
        _currentLocation.value = location

        // Carregar operadores à volta (sempre que nos movemos)
        loadOperators(location.latitude, location.longitude)

        // 2. SÓ adicionar à lista se estivermos numa viagem (Linha azul)
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
            val pointLoc = Location("point").apply {
                latitude = point.lat
                longitude = point.lon
            }
            val distance = currentLoc.distanceTo(pointLoc)

            if (distance < minDistance) {
                minDistance = distance
                closestPoint = point
            }
        }
        return if (minDistance <= maxDistanceMeters) closestPoint else null
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    // Método antigo getUserLocation (snapshot) pode ser mantido para inicialização rápida se quiseres
    @SuppressLint("MissingPermission")
    fun getUserLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    _currentLocation.value = it
                    loadOperators(it.latitude, it.longitude)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun loadOperators(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                _operatorPoints.value = repository.getAllOperators(lat, lon)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun saveTripToFirebase(userId: String, start: Long, end: Long, distance: Double, points: Int) {
        val tripData = hashMapOf(
            "userId" to userId,
            "startTime" to start,
            "endTime" to end,
            "distanceMeters" to distance,
            "pointsEarned" to points,
            "date" to Date(start)
        )
        db.collection("trips").add(tripData)

        val userRef = db.collection("users").document(userId)
        userRef.update(
            "totalTrips", FieldValue.increment(1),
            "points", FieldValue.increment(points.toLong())
        )
    }

    private fun resetTripData() {
        _routePoints.value = emptyList()
        tripStartTime = System.currentTimeMillis()
    }

    private fun calculateDistance(points: List<LatLng>): Double {
        var totalDistance = 0.0
        if (points.size < 2) return 0.0
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            val results = FloatArray(1)
            Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, results)
            totalDistance += results[0]
        }
        return totalDistance
    }
}