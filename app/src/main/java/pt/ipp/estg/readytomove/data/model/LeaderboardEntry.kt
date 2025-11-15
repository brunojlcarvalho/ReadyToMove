package pt.ipp.estg.readytomove.data.model

data class LeaderboardEntry(
    val id: String = "",
    val name: String = "",
    val points: Int = 0,
    val totalTrips: Int = 0,
    val avatarUrl: String? = null,
    val position: Int = 0
)
