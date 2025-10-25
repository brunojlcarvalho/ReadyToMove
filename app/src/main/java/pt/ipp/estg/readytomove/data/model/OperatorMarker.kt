package pt.ipp.estg.readytomove.data.model

import com.google.android.gms.maps.model.LatLng

data class OperatorMarker(
    val position: LatLng,
    val title: String,
    val iconRes: Int
)
