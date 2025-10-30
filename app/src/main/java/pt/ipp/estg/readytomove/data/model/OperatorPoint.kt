package pt.ipp.estg.readytomove.data.model

data class OperatorPoint(
    val id: String,
    val type: OperatorType,
    val name: String,
    val lat: Double,
    val lon: Double
)
