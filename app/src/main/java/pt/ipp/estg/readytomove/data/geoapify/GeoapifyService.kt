package pt.ipp.estg.readytomove.data.geoapify

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Query

data class GeoapifyResponse(
    val features: List<Feature>
) {
    data class Feature(
        val properties: Properties,
        val geometry: Geometry
    )

    data class Properties(
        @field:Json(name = "placeId") val placeId: String?,
        val name: String?,
        @field:Json(name = "addressLine1") val addressLine1: String?,
        val categories: List<String>?
    )

    data class Geometry(
        val coordinates: List<Double>
    )
}

interface GeoapifyService {
    @GET("v2/places")
    suspend fun getPlaces(
        @Query("categories") categories: String,
        @Query("filter") filter: String,
        @Query("limit") limit: Int = 100,
        @Query("apiKey") apiKey: String
    ): GeoapifyResponse
}
