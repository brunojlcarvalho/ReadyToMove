package pt.ipp.estg.readytomove.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pt.ipp.estg.readytomove.data.model.LeaderboardEntry

class LeaderboardViewModel : ViewModel() {

    private val _leaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<LeaderboardEntry>> = _leaderboard

    init {
        // Mock para teste
        loadMockLeaderboard()
    }

    private fun loadMockLeaderboard() {
        _leaderboard.value = listOf(
            LeaderboardEntry("1", "Ana Costa", 820, 25, null, 1),
            LeaderboardEntry("2", "Tomás Nunes", 720, 20, null, 2),
            LeaderboardEntry("3", "Miguel Silva", 610, 18, null, 3),
            LeaderboardEntry("4", "Joana Rocha", 590, 17, null, 4),
            LeaderboardEntry("5", "Sara Oliveira", 550, 16, null, 5)
        )
    }
}
