package pt.ipp.estg.readytomove.data.repository

import android.util.Log
import pt.ipp.estg.readytomove.data.geoapify.GeoApifyDataSource
import pt.ipp.estg.readytomove.data.geoapify.GeoApifyResponse
import pt.ipp.estg.readytomove.data.model.OperatorPoint
import pt.ipp.estg.readytomove.data.model.OperatorType

class GeoApifyRepository(private val dataSource: GeoApifyDataSource) {

    suspend fun getBusStops(lat: Double, lon: Double): List<OperatorPoint> {
        return try {
            val response = dataSource.fetchBusStops(lat, lon)

            if (response.isSuccessful) {
                val geoapifyResponse = response.body()
                geoapifyResponse?.let { mapToOperatorPoints(it, OperatorType.BUS, "Paragem de autocarro") } ?: emptyList()
            } else {
                Log.e("GeoApifyRepository", "Erro na API ao procurar paragens: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("GeoApifyRepository", "Exceção ao buscar paragens: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getBikeRentals(lat: Double, lon: Double): List<OperatorPoint> {
        return try {
            val response = dataSource.fetchBikeRentals(lat, lon)
            if (response.isSuccessful) {
                val geoapifyResponse = response.body()
                geoapifyResponse?.let { mapToOperatorPoints(it, OperatorType.BIKE, "Estação de bicicletas") } ?: emptyList()
            } else {
                Log.e("GeoApifyRepository", "Erro na API ao procurar bicicletas: ${response.code()} - ${response.message()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("GeoApifyRepository", "Exceção ao procurar bicicletas: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun getAllOperators(lat: Double, lon: Double): List<OperatorPoint> {
        val busStops = getBusStops(lat, lon)
        val bikeRentals = getBikeRentals(lat, lon)
        return busStops + bikeRentals
    }

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