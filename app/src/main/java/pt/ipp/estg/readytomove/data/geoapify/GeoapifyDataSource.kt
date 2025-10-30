package pt.ipp.estg.readytomove.data.geoapify

import pt.ipp.estg.readytomove.data.model.OperatorPoint
import pt.ipp.estg.readytomove.data.model.OperatorType
import retrofit2.Retrofit

class GeoapifyDataSource(
    retrofit: Retrofit,
    private val apiKey: String
) {
    private val service = retrofit.create(GeoapifyService::class.java)

    private val portoBragaRect =
        "rect:-8.801175002023257,41.60626461183551,-8.230622304421189,40.79925908283835"

    suspend fun fetchBusStops(): List<OperatorPoint> {
        val response = service.getPlaces(
            categories = "public_transport.bus",
            filter = portoBragaRect,
            limit = 500,
            apiKey = apiKey
        )

        return response.features.mapNotNull { f ->
            val props = f.properties
            val coords = f.geometry.coordinates
            if (coords.size < 2) return@mapNotNull null
            OperatorPoint(
                id = props.placeId ?: "${coords[0]},${coords[1]}",
                type = OperatorType.BUS,
                name = props.name ?: "Paragem de autocarro",
                lat = coords[1],
                lon = coords[0]
            )
        }
    }

    suspend fun fetchBikeRentals(): List<OperatorPoint> {
        val response = service.getPlaces(
            categories = "rental.bicycle",
            filter = portoBragaRect,
            limit = 500,
            apiKey = apiKey
        )

        return response.features.mapNotNull { f ->
            val props = f.properties
            val coords = f.geometry.coordinates
            if (coords.size < 2) return@mapNotNull null
            OperatorPoint(
                id = props.placeId ?: "${coords[0]},${coords[1]}",
                type = OperatorType.BIKE,
                name = props.name ?: "Estação de bicicletas",
                lat = coords[1],
                lon = coords[0]
            )
        }
    }

}
