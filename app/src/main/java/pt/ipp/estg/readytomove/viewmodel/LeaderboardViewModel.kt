package pt.ipp.estg.readytomove.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pt.ipp.estg.readytomove.data.model.LeaderboardEntry
import pt.ipp.estg.readytomove.data.model.User
import pt.ipp.estg.readytomove.util.NotificationHelper

class LeaderboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val prefs = application.getSharedPreferences("leaderboard_prefs", Context.MODE_PRIVATE)

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadRealLeaderboard()
    }

    fun loadRealLeaderboard() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = db.collection("users")
                    .orderBy("points", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()

                val entries = result.documents.mapIndexed { index, doc ->
                    val user = doc.toObject(User::class.java)

                    if (user != null) {
                        LeaderboardEntry(
                            id = user.id,
                            name = user.name,
                            points = user.points,
                            totalTrips = user.totalTrips,
                            avatarUrl = user.avatarUrl,
                            position = index + 1
                        )
                    } else {
                        null
                    }
                }.filterNotNull()

                _leaderboard.value = entries

                checkIfRankDropped(entries)

            } catch (e: Exception) {
                Log.e("LeaderboardViewModel", "Erro ao carregar leaderboard: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun checkIfRankDropped(entries: List<LeaderboardEntry>) {
        val currentUserId = auth.currentUser?.uid ?: return

        val myEntry = entries.find { it.id == currentUserId } ?: return
        val currentRank = myEntry.position

        val oldRank = prefs.getInt("last_rank_$currentUserId", -1)

        if (oldRank != -1 && oldRank <= 3 && currentRank > oldRank) {
            NotificationHelper.showRankDropNotification(getApplication(), oldRank, currentRank)
        }

        prefs.edit().putInt("last_rank_$currentUserId", currentRank).apply()
    }
}