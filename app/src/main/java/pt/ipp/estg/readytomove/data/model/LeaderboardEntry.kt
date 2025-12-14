package pt.ipp.estg.readytomove.data.model

/**
 * Data class representing an entry in the leaderboard.
 *
 * @property id The unique identifier of the user.
 * @property name The name of the user.
 * @property points The total points accumulated by the user.
 * @property totalTrips The total number of trips made by the user.
 * @property avatarUrl The URL of the user's avatar image (optional).
 * @property position The position of the user in the leaderboard.
 */
data class LeaderboardEntry(
    val id: String = "",
    val name: String = "",
    val points: Int = 0,
    val totalTrips: Int = 0,
    val avatarUrl: String? = null,
    val position: Int = 0
)
