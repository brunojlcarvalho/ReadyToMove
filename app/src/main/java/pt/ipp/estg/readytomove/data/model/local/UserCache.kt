package pt.ipp.estg.readytomove.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class representing a user in the local database cache.
 *
 * @property id Unique identifier for the user.
 * @property name Name of the user.
 * @property email Email address of the user.
 * @property points Total points accumulated by the user.
 * @property totalTrips Total number of trips taken by the user.
 * @property avatarUrl URL of the user's avatar image (nullable).
 */
@Entity(tableName = "users")
data class UserCache(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val points: Int = 0,
    val totalTrips: Int = 0,
    val avatarUrl: String? = null
)