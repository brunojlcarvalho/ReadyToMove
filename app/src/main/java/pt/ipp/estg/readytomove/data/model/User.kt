package pt.ipp.estg.readytomove.data.model

/**
 * Data class representing a user in the application.
 *
 * @property id The unique identifier of the user.
 * @property name The name of the user.
 * @property email The email address of the user.
 * @property points The total points accumulated by the user.
 * @property totalTrips The total number of trips made by the user.
 * @property avatarUrl The URL of the user's avatar image (optional).
 */
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val points: Int = 0,
    val totalTrips: Int = 0,
    val avatarUrl: String? = null
)
