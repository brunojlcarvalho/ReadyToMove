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

/**
 * ViewModel responsible for managing and displaying the user's travel history.
 *
 * It combines local data (Room) with real-time updates from the Cloud (Firestore)
 * to ensure the trip list is always up-to-date.
 *
 * @param application The application context.
 */
class TravelHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val localRepository = LocalRepository.getInstance(application)
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips
    private var listenerRegistration: ListenerRegistration? = null

    /**
     * Loads trips for the currently logged-in user.
     * Initiates observation of the local database and starts real-time synchronization with Firestore.
     */
    fun loadUserTrips() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Log.e("TravelHistory", "User not logged in")
            return
        }

        // Observe local database
        viewModelScope.launch {
            localRepository.observeTripsForUser(userId).collect { userTrips ->
                _trips.value = userTrips
            }
        }

        // Subscribe to changes in Cloud (Firestore) in Real-Time
        startRealTimeSync(userId)
    }

    /**
     * Starts listening for trip updates in Firestore for the specific user.
     * New or updated trips found in Firestore are automatically saved to the local database.
     *
     * @param userId The ID of the user whose trips to sync.
     */
    private fun startRealTimeSync(userId: String) {
        // If an active listener exists, remove it to avoid duplicates
        listenerRegistration?.remove()

        val query = db.collection("trips").whereEqualTo("userId", userId)

        listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("TravelHistory", "Error syncing trips: ${error.message}")
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

                Log.d("TravelHistory", "Received ${remoteTrips.size} trips in real-time.")

                if (remoteTrips.isNotEmpty()) {
                    viewModelScope.launch {
                        localRepository.saveRemoteTrips(userId, remoteTrips)
                    }
                }
            }
        }
    }

    /**
     * Retrieves a specific trip by its ID from the current list.
     *
     * @param tripId The ID of the trip to find.
     * @return The Trip object if found, null otherwise.
     */
    fun getTripById(tripId: String): Trip? {
        return _trips.value.find { it.id == tripId }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}