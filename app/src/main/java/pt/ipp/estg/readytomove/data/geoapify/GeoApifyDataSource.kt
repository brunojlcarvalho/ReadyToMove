package pt.ipp.estg.readytomove.data.geoapify

import retrofit2.Response
import retrofit2.Retrofit

class GeoApifyDataSource(
    retrofit: Retrofit,
    private val apiKey: String
) {
    private val service = retrofit.create(GeoApifyService::class.java)

    suspend fun fetchBusStops(lat: Double, lon: Double): Response<GeoApifyResponse> {
        val circleFilter = "circle:$lon,$lat,5000"

        return service.getPlaces(
            categories = "public_transport.bus",
            filter = circleFilter,
            limit = 50,
            apiKey = apiKey
        )
    }

    suspend fun fetchBikeRentals(lat: Double, lon: Double): Response<GeoApifyResponse> {
        val circleFilter = "circle:$lon,$lat,5000"

        return service.getPlaces(
            categories = "rental.bicycle",
            filter = circleFilter,
            limit = 100,
            apiKey = apiKey
        )
    }
}
