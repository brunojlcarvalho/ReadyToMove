package pt.ipp.estg.readytomove.data.geoapify

import retrofit2.Response
import retrofit2.Retrofit

class GeoApifyDataSource(
    retrofit: Retrofit,
    private val apiKey: String
) {
    private val service = retrofit.create(GeoApifyService::class.java)

    private val portoBragaRect =
        "rect:-8.801175002023257,41.60626461183551,-8.230622304421189,40.79925908283835"

    suspend fun fetchBusStops(): Response<GeoApifyResponse> {
        return service.getPlaces(
            categories = "public_transport.bus",
            filter = portoBragaRect,
            limit = 100,
            apiKey = apiKey
        )
    }

    suspend fun fetchBikeRentals(): Response<GeoApifyResponse> {
        return service.getPlaces(
            categories = "rental.bicycle",
            filter = portoBragaRect,
            limit = 100,
            apiKey = apiKey
        )
    }
}
