package pt.ipp.estg.readytomove.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pt.ipp.estg.readytomove.data.model.LeaderboardEntry
import pt.ipp.estg.readytomove.data.model.User
import pt.ipp.estg.readytomove.util.NotificationHelper

/**
 * ViewModel responsible for managing the Leaderboard data.
 *
 * This ViewModel:
 * - Fetches the top 50 users sorted by points from Firestore in real-time.
 * - Tracks the user's rank changes and triggers notifications if the rank drops.
 * - Exposes the leaderboard data as a [StateFlow] to the UI.
 *
 * @param application The application context.
 */
class LeaderboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val prefs = application.getSharedPreferences("leaderboard_prefs", Context.MODE_PRIVATE)

    // Backing property for the leaderboard list
    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())

    /**
     * A [StateFlow] emitting the current list of leaderboard entries.
     */
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard

    private val _isLoading = MutableStateFlow(false)

    // Firestore listener registration, used to remove the listener when the ViewModel is cleared
    private var listenerRegistration: ListenerRegistration? = null

    init {
        startRealTimeUpdates()
    }

    /**
     * Starts listening for real-time updates from the "users" collection in Firestore.
     * The query orders users by points (descending) and limits the result to the top 50.
     */
    private fun startRealTimeUpdates() {
        _isLoading.value = true

        // Define the query: Sort by points descending, limit to top 50
        val query = db.collection("users")
            .orderBy("points", Query.Direction.DESCENDING)
            .limit(50)

        // SUBSCRIBE (Publish/Subscribe Pattern)
        // addSnapshotListener is automatically triggered whenever data changes on the server
        listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("LeaderboardViewModel", "Error listening changes: ${error.message}")
                _isLoading.value = false
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Map documents to LeaderboardEntry objects
                val entries = snapshot.documents.mapIndexed { index, doc ->
                    val user = doc.toObject(User::class.java)
                    if (user != null) {
                        LeaderboardEntry(
                            id = user.id,
                            name = user.name,
                            points = user.points,
                            totalTrips = user.totalTrips,
                            avatarUrl = user.avatarUrl,
                            position = index + 1 // Rank is 1-based index
                        )
                    } else {
                        null
                    }
                }.filterNotNull()

                // Update the Flow (UI reacts automatically)
                _leaderboard.value = entries

                // Check if the user's rank dropped (now in real-time!)
                checkIfRankDropped(entries)

                _isLoading.value = false
            }
        }
    }

    /**
     * Checks if the current user's rank has dropped compared to the last saved rank.
     * If the user was in the top 3 and their rank drops, a notification is triggered.
     *
     * @param entries The current list of leaderboard entries.
     */
    private fun checkIfRankDropped(entries: List<LeaderboardEntry>) {
        val currentUserId = auth.currentUser?.uid ?: return

        // Find the user's current entry and rank
        val myEntry = entries.find { it.id == currentUserId } ?: return
        val currentRank = myEntry.position

        // Retrieve the last known rank from SharedPreferences
        val oldRank = prefs.getInt("last_rank_$currentUserId", -1)

        // Notification logic: Notify only if the user was in top 3 and dropped rank
        if (oldRank != -1 && oldRank <= 3 && currentRank > oldRank) {
            NotificationHelper.showRankDropNotification(getApplication(), oldRank, currentRank)
        }

        // Save the current rank for future comparison
        prefs.edit().putInt("last_rank_$currentUserId", currentRank).apply()
    }

    /**
     * Cleans up resources when the ViewModel is destroyed.
     * Removes the Firestore listener to save battery and data usage.
     */
    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}