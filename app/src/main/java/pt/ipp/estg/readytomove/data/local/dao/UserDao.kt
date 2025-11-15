package pt.ipp.estg.readytomove.data.local.dao

import androidx.room.*
import pt.ipp.estg.readytomove.data.model.local.UserCache

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserCache)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: String): UserCache?

    @Query("SELECT * FROM users")
    suspend fun getAll(): List<UserCache>

    @Delete
    suspend fun delete(user: UserCache)
}