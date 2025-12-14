package pt.ipp.estg.readytomove.data.geoapify

import com.google.gson.annotations.SerializedName

/**
 * Data class representing the response from the GeoApify API.
 */
data class GeoApifyResponse(
    @SerializedName("type")
    val type: String,
    @SerializedName("features")
    val features: List<Feature>
) {

    data class Feature(
        @SerializedName("type")
        val type: String,
        @SerializedName("properties")
        val properties: Properties,
        @SerializedName("geometry")
        val geometry: Geometry
    )

    data class Properties(
        @SerializedName("name")
        val name: String?,
        @SerializedName("place_id")
        val placeId: String?
    )

    data class Geometry(
        @SerializedName("type")
        val type: String,
        @SerializedName("coordinates")
        val coordinates: List<Double>
    )
}
