package pt.ipp.estg.readytomove.viewmodel

import androidx.lifecycle.ViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pt.ipp.estg.readytomove.data.model.Trip

class TravelHistoryViewModel : ViewModel() {

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips

    init {
        loadMockTrips()
    }

    private fun loadMockTrips() {
        _trips.value = listOf(
            Trip("t1", "2025-10-20", 3.2, 10),
            Trip("t2", "2025-10-18", 1.1, 4),
            Trip("t3", "2025-10-15", 5.6, 18),
            Trip("t4", "2025-10-10", 2.4, 8),
            Trip("t5", "2025-09-30", 7.8, 25)
        )
    }
}
