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

class LeaderboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val prefs = application.getSharedPreferences("leaderboard_prefs", Context.MODE_PRIVATE)
    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())

    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard

    private val _isLoading = MutableStateFlow(false)
    private var listenerRegistration: ListenerRegistration? = null

    init {
        startRealTimeUpdates()
    }

    private fun startRealTimeUpdates() {
        _isLoading.value = true

        // Definimos a Query
        val query = db.collection("users")
            .orderBy("points", Query.Direction.DESCENDING)
            .limit(50)

        // SUBSCREVER (Publish/Subscribe Pattern)
        // O addSnapshotListener é chamado automaticamente sempre que algo muda no servidor
        listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("LeaderboardViewModel", "Error listening changes: ${error.message}")
                _isLoading.value = false
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Mapear os documentos
                val entries = snapshot.documents.mapIndexed { index, doc ->
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

                // Atualizar o Flow (A UI reage automaticamente)
                _leaderboard.value = entries

                // Verificar se perdemos posições (agora em tempo real!)
                checkIfRankDropped(entries)

                _isLoading.value = false
            }
        }
    }

    private fun checkIfRankDropped(entries: List<LeaderboardEntry>) {
        val currentUserId = auth.currentUser?.uid ?: return

        val myEntry = entries.find { it.id == currentUserId } ?: return
        val currentRank = myEntry.position

        val oldRank = prefs.getInt("last_rank_$currentUserId", -1)

        // Lógica de notificação
        if (oldRank != -1 && oldRank <= 3 && currentRank > oldRank) {
            NotificationHelper.showRankDropNotification(getApplication(), oldRank, currentRank)
        }

        prefs.edit().putInt("last_rank_$currentUserId", currentRank).apply()
    }

    // Parar de escutar quando o ViewModel é destruído para poupar bateria/dados
    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}