package pt.ipp.estg.readytomove.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pt.ipp.estg.readytomove.data.model.Trip
import pt.ipp.estg.readytomove.data.model.local.TripCache
import pt.ipp.estg.readytomove.data.repository.LocalRepository

class TravelHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val localRepository = LocalRepository.getInstance(application)
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance() // Adicionado Firestore

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips

    fun loadUserTrips() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Log.e("TravelHistory", "User not logged in")
            return
        }
        // 1. Começa a observar a BD Local (Room)
        // Assim que guardarmos dados lá, a UI atualiza sozinha
        viewModelScope.launch {
            localRepository.observeTripsForUser(userId).collect { userTrips ->
                _trips.value = userTrips
            }
        }

        // 2. Sincronizar com a Cloud (Firestore)
        syncTripsFromCloud(userId)
    }

    private fun syncTripsFromCloud(userId: String) {
        viewModelScope.launch {
            try {
                // Vai buscar todas as viagens deste user
                val result = db.collection("trips")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                if (!result.isEmpty) {
                    val remoteTrips = result.documents.mapNotNull { doc ->
                        try {
                            val startTime = doc.getLong("startTime") ?: 0L
                            val endTime = doc.getLong("endTime")
                            val distance = doc.getDouble("distanceMeters") ?: 0.0

                            // O ID vai a 0 para o Room gerar um novo
                            TripCache(
                                id = 0,
                                userId = userId,
                                startTime = startTime,
                                endTime = endTime,
                                distanceMeters = distance
                            )
                        } catch (e: Exception) { null }
                    }

                    Log.d("TravelHistory", "Sincronizing ${remoteTrips.size} trips.")

                    // Chamamos a função passando o userId para ele limpar a cache desse user
                    if (remoteTrips.isNotEmpty()) {
                        localRepository.saveRemoteTrips(userId, remoteTrips)
                    }
                }
            } catch (e: Exception) {
                Log.e("TravelHistory", "Error sync: ${e.message}")
            }
        }
    }

    fun getTripById(tripId: String): Trip? {
        return _trips.value.find { it.id == tripId }
    }
}