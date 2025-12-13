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

/**
 * Repository for managing local data storage using Room database.
 *
 * @property db The AppDatabase instance for accessing DAOs.
 */
class LocalRepository private constructor(private val db: AppDatabase) {

    private val tripDao = db.tripDao()
    private val userDao = db.userDao()

    /** Converts a UserCache object to a User domain model.
     *
     * @return The corresponding User domain model.
     */
    private fun UserCache.toDomainModel(): User = User(id, name, email, points, totalTrips, avatarUrl)
    /** Converts a User domain model to a UserCache object.
     *
     * @return The corresponding UserCache object.
     */
    private fun User.toCacheModel(): UserCache = UserCache(id, name, email, points, totalTrips, avatarUrl)

    /** Retrieves a user by their ID from the local database.
     *
     * @param userId The ID of the user to retrieve.
     * @return The User domain model if found, null otherwise.
     */
    suspend fun getUser(userId: String): User? {
        return userDao.getById(userId)?.toDomainModel()
    }

    /** Saves a user to the local database.
     *
     * @param user The User domain model to save.
     */
    suspend fun saveUser(user: User) {
        userDao.insert(user.toCacheModel())
    }

    /** Deletes a user from the local database.
     *
     * @param user The User domain model to delete.
     */
    suspend fun deleteUser(user: User) {
        userDao.delete(user.toCacheModel())
    }

    /** Adds a trip to the local database and updates user statistics.
     *
     * @param userId The ID of the user associated with the trip.
     * @param startTime The start time of the trip in milliseconds.
     * @param endTime The end time of the trip in milliseconds (nullable).
     * @param distanceMeters The distance of the trip in meters.
     */
    suspend fun addTrip(userId: String, startTime: Long, endTime: Long?, distanceMeters: Double, startLocation: String?,
                        endLocation: String?) {
        val tripCache = TripCache(
            userId = userId,
            startTime = startTime,
            endTime = endTime,
            distanceMeters = distanceMeters,
            startLocation = startLocation,
            endLocation = endLocation
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

    /** Synchronizes local trips with remote trips for a specific user.
     *
     * @param userId The ID of the user whose trips are being synchronized.
     * @param remoteTrips The list of TripCache objects retrieved from the remote source.
     */
    suspend fun saveRemoteTrips(userId: String, remoteTrips: List<TripCache>) {
        val localTrips = tripDao.getByUser(userId)

        val localMap = localTrips.associateBy { it.startTime }
        val remoteStartTimes = remoteTrips.map { it.startTime }.toSet()

        for (remote in remoteTrips) {
            val local = localMap[remote.startTime]
            if (local != null) {
                val updatedTrip = remote.copy(id = local.id)
                tripDao.insert(updatedTrip)
            } else {
                tripDao.insert(remote)
            }
        }

        for (local in localTrips) {
            if (local.startTime !in remoteStartTimes) {
                tripDao.deleteById(local.id)
            }
        }
    }

    /** Observes trips for a specific user as a Flow.
     *
     * @param userId The ID of the user whose trips are being observed.
     * @return A Flow emitting lists of Trip domain models.
     */
    fun observeTripsForUser(userId: String): Flow<List<Trip>> {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        return tripDao.getByUserFlow(userId).map { list ->
            list.map { cache ->
                val date = sdf.format(Date(cache.startTime))

                val end = cache.endTime ?: cache.startTime
                val diffMillis = end - cache.startTime
                val durationMin = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
                val durationStr = if (durationMin < 1L) "< 1 min" else "$durationMin min"

                Trip(
                    id = cache.id.toString(),
                    date = date,
                    distanceKm = (cache.distanceMeters / 1000.0),
                    points = calculatePointsFromDistance(cache.distanceMeters),
                    duration = durationStr,
                    startLocation = cache.startLocation,
                    endLocation = cache.endLocation
                )
            }
        }
    }

    /** Calculates points based on the distance traveled.
     *
     * @param distanceMeters The distance traveled in meters.
     * @return The calculated points.
     */
    private fun calculatePointsFromDistance(distanceMeters: Double): Int {
        return (distanceMeters / 100.0).toInt()
    }

    /**
     * Companion object to manage the singleton instance of LocalRepository.
     */
    companion object {
        @Volatile private var INSTANCE: LocalRepository? = null

        /** Get the singleton instance of LocalRepository.
         *
         * @param context The application context.
         * @return The LocalRepository instance.
         */
        fun getInstance(context: Context): LocalRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalRepository(AppDatabase.getInstance(context)).also { INSTANCE = it }
            }
    }
}