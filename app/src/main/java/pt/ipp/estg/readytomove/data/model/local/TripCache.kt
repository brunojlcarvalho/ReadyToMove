package pt.ipp.estg.readytomove.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripCache(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val startTime: Long,
    val endTime: Long?,
    val distanceMeters: Double = 0.0
)