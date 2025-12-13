package pt.ipp.estg.readytomove.data.model

/**
 * Data class representing an operator point such as a bus stop or bike station.
 *
 * @property id The unique identifier of the operator point.
 * @property type The type of the operator (e.g., BUS, BIKE).
 * @property name The name of the operator point.
 * @property lat The latitude coordinate of the operator point.
 * @property lon The longitude coordinate of the operator point.
 */
data class OperatorPoint(
    val id: String,
    val type: OperatorType,
    val name: String,
    val lat: Double,
    val lon: Double
)
