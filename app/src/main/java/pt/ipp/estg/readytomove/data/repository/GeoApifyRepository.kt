package pt.ipp.estg.readytomove.data.repository

import android.util.Log
import pt.ipp.estg.readytomove.data.geoapify.GeoApifyDataSource
import pt.ipp.estg.readytomove.data.geoapify.GeoApifyResponse
import pt.ipp.estg.readytomove.data.model.OperatorPoint
import pt.ipp.estg.readytomove.data.model.OperatorType

/**
 * Repository class for fetching operator points such as bus stops and bike rentals from GeoApify API.
 *
 * @property dataSource The data source for making API calls to GeoApify.
 */
class GeoApifyRepository(private val dataSource: GeoApifyDataSource) {

    suspend fun getBusStops(lat: Double, lon: Double): List<OperatorPoint> {
        return try {
            val response = dataSource.fetchBusStops(lat, lon)

            if (response.isSuccessful) {
                val geoapifyResponse = response.body()
                geoapifyResponse?.let { mapToOperatorPoints(it, OperatorType.BUS, "Bus Stop") } ?: emptyList()
            } else {
                Log.e("GeoApifyRepository", "Error in API searching for stops ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("GeoApifyRepository", "Exception searching for stops: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Fetches bike rental stations near the specified latitude and longitude.
     *
     * @param lat The latitude coordinate.
     * @param lon The longitude coordinate.
     * @return A list of [OperatorPoint] representing bike rental stations.
     */
    suspend fun getBikeRentals(lat: Double, lon: Double): List<OperatorPoint> {
        return try {
            val response = dataSource.fetchBikeRentals(lat, lon)
            if (response.isSuccessful) {
                val geoapifyResponse = response.body()
                geoapifyResponse?.let { mapToOperatorPoints(it, OperatorType.BIKE, "Bike stations") } ?: emptyList()
            } else {
                Log.e("GeoApifyRepository", "Error in Api searching for bikes: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("GeoApifyRepository", "Exception looking for bikes: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Fetches all operator points (bus stops and bike rentals) near the specified latitude and longitude.
     *
     * @param lat The latitude coordinate.
     * @param lon The longitude coordinate.
     * @return A list of [OperatorPoint] representing all operator points.
     */
    suspend fun getAllOperators(lat: Double, lon: Double): List<OperatorPoint> {
        val busStops = getBusStops(lat, lon)
        val bikeRentals = getBikeRentals(lat, lon)
        return busStops + bikeRentals
    }

    /**
     * Maps the [GeoApifyResponse] to a list of [OperatorPoint].
     *
     * @param response The response from GeoApify API.
     * @param type The type of operator (BUS or BIKE).
     * @param defaultName The default name to use if the name is not provided in the response.
     * @return A list of [OperatorPoint].
     */
    private fun mapToOperatorPoints(
        response: GeoApifyResponse,
        type: OperatorType,
        defaultName: String
    ): List<OperatorPoint> {
        return response.features.mapNotNull { feature ->
            val props = feature.properties
            val coords = feature.geometry.coordinates

            if (coords.size < 2) return@mapNotNull null

            OperatorPoint(
                id = props.placeId ?: "${coords[0]},${coords[1]}",
                type = type,
                name = props.name ?: defaultName,
                lat = coords[1],
                lon = coords[0]
            )
        }
    }
}