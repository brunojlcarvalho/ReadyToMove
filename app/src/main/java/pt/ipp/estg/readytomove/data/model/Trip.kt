package pt.ipp.estg.readytomove.data.model

data class Trip(
    val id: String = "",
    val date: String = "",
    val distanceKm: Double = 0.0,
    val points: Int = 0
)