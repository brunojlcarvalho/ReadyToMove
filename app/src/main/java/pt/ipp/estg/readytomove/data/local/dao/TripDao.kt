package pt.ipp.estg.readytomove.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import pt.ipp.estg.readytomove.data.model.local.TripCache

/**
 * Data Access Object (DAO) for managing TripCache entities in the local database.
 */
@Dao
interface TripDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trip: TripCache)

    @Query("SELECT * FROM trips WHERE userId = :userId ORDER BY startTime DESC")
    fun getByUserFlow(userId: String): Flow<List<TripCache>>

    @Query("SELECT * FROM trips WHERE userId = :userId")
    suspend fun getByUser(userId: String): List<TripCache>

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM trips WHERE userId = :userId")
    suspend fun deleteByUser(userId: String)
}