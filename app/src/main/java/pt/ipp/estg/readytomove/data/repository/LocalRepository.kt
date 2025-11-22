package pt.ipp.estg.readytomove.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pt.ipp.estg.readytomove.data.local.AppDatabase
import pt.ipp.estg.readytomove.data.model.Trip
import pt.ipp.estg.readytomove.data.model.User
import pt.ipp.estg.readytomove.data.model.local.TripCache
import pt.ipp.estg.readytomove.data.model.local.UserCache
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalRepository private constructor(private val db: AppDatabase) {

    private val tripDao = db.tripDao()
    private val userDao = db.userDao()

    private fun UserCache.toDomainModel(): User = User(id, name, email, points, totalTrips, avatarUrl)
    private fun User.toCacheModel(): UserCache = UserCache(id, name, email, points, totalTrips, avatarUrl)

    suspend fun getUser(userId: String): User? {
        return userDao.getById(userId)?.toDomainModel()
    }

    suspend fun saveUser(user: User) {
        userDao.insert(user.toCacheModel())
    }

    suspend fun deleteUser(user: User) {
        userDao.delete(user.toCacheModel())
    }

    suspend fun addTrip(userId: String, startTime: Long, endTime: Long?, distanceMeters: Double) {
        // 1. Guardar a Viagem na tabela 'trips'
        val tripCache = TripCache(
            userId = userId,
            startTime = startTime,
            endTime = endTime,
            distanceMeters = distanceMeters
        )
        tripDao.insert(tripCache)

        // 2. Calcular pontos (1 ponto a cada 100 metros)
        val pointsEarned = calculatePointsFromDistance(distanceMeters)

        // 3. Atualizar o Utilizador na tabela 'users'
        val currentUserCache = userDao.getById(userId)
        if (currentUserCache != null) {
            val updatedUser = currentUserCache.copy(
                totalTrips = currentUserCache.totalTrips + 1,       // +1 Viagem
                points = currentUserCache.points + pointsEarned     // +Pontos
            )
            userDao.insert(updatedUser) // O Room faz replace se o ID for igual
        }
    }

    fun observeTripsForUser(userId: String): Flow<List<Trip>> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return tripDao.getByUserFlow(userId).map { list ->
            list.map { cache ->
                val date = sdf.format(Date(cache.startTime))
                Trip(
                    id = cache.id.toString(),
                    date = date,
                    distanceKm = (cache.distanceMeters / 1000.0),
                    points = calculatePointsFromDistance(cache.distanceMeters)
                )
            }
        }
    }

    suspend fun deleteTripById(id: Long) = tripDao.deleteById(id)

    private fun calculatePointsFromDistance(distanceMeters: Double): Int {
        return (distanceMeters / 100.0).toInt()
    }

    companion object {
        @Volatile private var INSTANCE: LocalRepository? = null

        fun getInstance(context: Context): LocalRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalRepository(AppDatabase.getInstance(context)).also { INSTANCE = it }
            }
    }
}