package pt.ipp.estg.readytomove.data.model

/**
 * Data class representing a trip made by a user.
 *
 * @property id The unique identifier of the trip.
 * @property date The date when the trip was made.
 * @property distanceKm The distance covered during the trip in kilometers.
 * @property points The points earned from the trip.
 * @property duration The duration of the trip.
 */
data class Trip(
    val id: String = "",
    val date: String = "",
    val distanceKm: Double = 0.0,
    val points: Int = 0,
    val duration: String = "",
    val startLocation: String? = "",
    val endLocation: String? = ""
)