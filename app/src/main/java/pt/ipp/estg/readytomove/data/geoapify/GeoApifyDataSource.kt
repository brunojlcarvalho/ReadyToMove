package pt.ipp.estg.readytomove.data.geoapify

import retrofit2.Response
import retrofit2.Retrofit

/** Data source class for interacting with the GeoApify API to fetch bus stops and bike rental locations.
 *
 * @property retrofit The Retrofit instance used for making network requests.
 * @property apiKey The API key for authenticating requests to the GeoApify API.
 */
class GeoApifyDataSource(
    retrofit: Retrofit,
    private val apiKey: String
) {
    private val service = retrofit.create(GeoApifyService::class.java)

    /** Fetches bus stops within a 5km radius of the specified latitude and longitude.
     *
     * @param lat The latitude of the center point.
     * @param lon The longitude of the center point.
     * @return A [Response] containing a [GeoApifyResponse] with the bus stop data.
     */
    suspend fun fetchBusStops(lat: Double, lon: Double): Response<GeoApifyResponse> {
        val circleFilter = "circle:$lon,$lat,5000"

        return service.getPlaces(
            categories = "public_transport.bus",
            filter = circleFilter,
            limit = 50,
            apiKey = apiKey
        )
    }

    /** Fetches bike rental locations within a 5km radius of the specified latitude and longitude.
     *
     * @param lat The latitude of the center point.
     * @param lon The longitude of the center point.
     * @return A [Response] containing a [GeoApifyResponse] with the bike rental data.
     */
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
