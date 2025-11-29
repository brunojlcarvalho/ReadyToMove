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
import java.util.concurrent.TimeUnit

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
        val tripCache = TripCache(
            userId = userId,
            startTime = startTime,
            endTime = endTime,
            distanceMeters = distanceMeters
        )
        tripDao.insert(tripCache)

        val pointsEarned = calculatePointsFromDistance(distanceMeters)

        val currentUserCache = userDao.getById(userId)
        if (currentUserCache != null) {
            val updatedUser = currentUserCache.copy(
                totalTrips = currentUserCache.totalTrips + 1,
                points = currentUserCache.points + pointsEarned
            )
            userDao.insert(updatedUser)
        }
    }

    // --- CORREÇÃO AQUI ---
    suspend fun saveRemoteTrips(userId: String, remoteTrips: List<TripCache>) {
        // 1. Buscar viagens existentes na cache local
        val localTrips = tripDao.getByUser(userId)

        // 2. Criar um mapa para encontrar viagens locais pelo "startTime" (chave única lógica)
        val localMap = localTrips.associateBy { it.startTime }
        val remoteStartTimes = remoteTrips.map { it.startTime }.toSet()

        // 3. Inserir novas ou Atualizar existentes (mantendo o ID)
        for (remote in remoteTrips) {
            val local = localMap[remote.startTime]
            if (local != null) {
                // Se já existe, usamos o ID da versão local para substituir
                // Isto impede que o ID mude e que o ecrã de detalhes "perca" a viagem
                val updatedTrip = remote.copy(id = local.id)
                tripDao.insert(updatedTrip)
            } else {
                // Se é nova, inserimos tal como veio (id=0 gera um novo)
                tripDao.insert(remote)
            }
        }

        // 4. (Opcional) Apagar viagens locais que já não existem na nuvem
        for (local in localTrips) {
            if (local.startTime !in remoteStartTimes) {
                tripDao.deleteById(local.id)
            }
        }
    }

    fun observeTripsForUser(userId: String): Flow<List<Trip>> {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        return tripDao.getByUserFlow(userId).map { list ->
            list.map { cache ->
                val date = sdf.format(Date(cache.startTime))

                // Calcular duração
                val end = cache.endTime ?: cache.startTime
                val diffMillis = end - cache.startTime
                val durationMin = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
                val durationStr = if (durationMin < 1L) "< 1 min" else "$durationMin min"

                Trip(
                    id = cache.id.toString(),
                    date = date,
                    distanceKm = (cache.distanceMeters / 1000.0),
                    points = calculatePointsFromDistance(cache.distanceMeters),
                    duration = durationStr
                )
            }
        }
    }

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