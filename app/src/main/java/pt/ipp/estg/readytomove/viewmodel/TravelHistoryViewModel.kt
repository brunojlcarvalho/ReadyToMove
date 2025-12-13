package pt.ipp.estg.readytomove.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.ipp.estg.readytomove.data.model.Trip
import pt.ipp.estg.readytomove.data.model.local.TripCache
import pt.ipp.estg.readytomove.data.repository.LocalRepository

class TravelHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val localRepository = LocalRepository.getInstance(application)
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips
    private var listenerRegistration: ListenerRegistration? = null

    fun loadUserTrips() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Log.e("TravelHistory", "User not logged in")
            return
        }

        viewModelScope.launch {
            localRepository.observeTripsForUser(userId).collect { userTrips ->
                _trips.value = userTrips
            }
        }

        // Subscrever alterações na Cloud (Firestore) em Tempo Real
        startRealTimeSync(userId)
    }

    private fun startRealTimeSync(userId: String) {
        // Se já existir um listener ativo, removemos para evitar duplicados
        listenerRegistration?.remove()

        val query = db.collection("trips").whereEqualTo("userId", userId)

        listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("TravelHistory", "Erro ao sincronizar viagens: ${error.message}")
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                val remoteTrips = snapshot.documents.mapNotNull { doc ->
                    try {
                        val startTime = doc.getLong("startTime") ?: 0L
                        val endTime = doc.getLong("endTime")
                        val distance = doc.getDouble("distanceMeters") ?: 0.0
                        val startLoc = doc.getString("startLocation")
                        val endLoc = doc.getString("endLocation")
                        val routePath = doc.getString("routePath")

                        TripCache(
                            id = 0,
                            userId = userId,
                            startTime = startTime,
                            endTime = endTime,
                            distanceMeters = distance,
                            startLocation = startLoc,
                            endLocation = endLoc,
                            routePath = routePath
                        )
                    } catch (e: Exception) { null }
                }

                Log.d("TravelHistory", "Recebidas ${remoteTrips.size} viagens em tempo real.")

                if (remoteTrips.isNotEmpty()) {
                    viewModelScope.launch {
                        localRepository.saveRemoteTrips(userId, remoteTrips)
                    }
                }
            }
        }
    }

    fun getTripById(tripId: String): Trip? {
        return _trips.value.find { it.id == tripId }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}