package pt.ipp.estg.readytomove.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserCache(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val points: Int = 0,
    val totalTrips: Int = 0,
    val avatarUrl: String? = null
)