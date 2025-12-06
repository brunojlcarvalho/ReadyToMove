package pt.ipp.estg.readytomove.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel // MUDANÇA: Usar AndroidViewModel
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
import pt.ipp.estg.readytomove.util.NotificationHelper // O helper que criamos

// MUDANÇA: Herdar de AndroidViewModel para ter acesso ao 'getApplication()'
class LeaderboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // SharedPreferences para guardar a última posição conhecida
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

                // --- LÓGICA DE NOTIFICAÇÃO ---
                checkIfRankDropped(entries)
                // -----------------------------

            } catch (e: Exception) {
                Log.e("LeaderboardViewModel", "Erro ao carregar leaderboard: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun checkIfRankDropped(entries: List<LeaderboardEntry>) {
        val currentUserId = auth.currentUser?.uid ?: return

        // 1. Descobrir a posição atual do utilizador nesta lista
        val myEntry = entries.find { it.id == currentUserId } ?: return
        val currentRank = myEntry.position

        // 2. Ler a posição antiga guardada (Default é -1 se não existir)
        val oldRank = prefs.getInt("last_rank_$currentUserId", -1)

        // 3. Verificar a condição:
        // - oldRank != -1 (significa que já tínhamos registo)
        // - oldRank <= 3 (estava no TOP 3)
        // - currentRank > oldRank (a posição atual é pior, ex: era 2, agora é 3 ou 4)
        if (oldRank != -1 && oldRank <= 3 && currentRank > oldRank) {
            // Disparar notificação
            NotificationHelper.showRankDropNotification(getApplication(), oldRank, currentRank)
        }

        // 4. Guardar a posição atual para a próxima comparação
        prefs.edit().putInt("last_rank_$currentUserId", currentRank).apply()
    }
}