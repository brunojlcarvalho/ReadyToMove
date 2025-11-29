package pt.ipp.estg.readytomove.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pt.ipp.estg.readytomove.data.model.LeaderboardEntry
import pt.ipp.estg.readytomove.data.model.User

class LeaderboardViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

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

            } catch (e: Exception) {
                Log.e("LeaderboardViewModel", "Erro ao carregar leaderboard: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}