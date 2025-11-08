package pt.ipp.estg.readytomove.data.geoapify

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface GeoApifyService {

    @Headers("Content-Type: application/json", "Accept: application/json")
    @GET("v2/places")
    suspend fun getPlaces(
        @Query("categories") categories: String,
        @Query("filter") filter: String,
        @Query("limit") limit: Int,
        @Query("apiKey") apiKey: String
    ): Response<GeoApifyResponse>
}
