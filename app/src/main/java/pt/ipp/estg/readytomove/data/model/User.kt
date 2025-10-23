package pt.ipp.estg.readytomove.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val points: Int = 0,
    val totalTrips: Int = 0,
    val avatarUrl: String? = null
)
