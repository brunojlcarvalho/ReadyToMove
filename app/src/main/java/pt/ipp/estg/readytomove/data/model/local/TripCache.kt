package pt.ipp.estg.readytomove.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class representing a cached trip in the local database.
 *
 * @property id The unique identifier for the trip (auto-generated).
 * @property userId The identifier of the user who made the trip.
 * @property startTime The start time of the trip in milliseconds since epoch.
 * @property endTime The end time of the trip in milliseconds since epoch, nullable if the trip is ongoing.
 * @property distanceMeters The total distance of the trip in meters.
 */
@Entity(tableName = "trips")
data class TripCache(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val startTime: Long,
    val endTime: Long?,
    val distanceMeters: Double = 0.0,
    val startLocation: String? = null,
    val endLocation: String? = null
)