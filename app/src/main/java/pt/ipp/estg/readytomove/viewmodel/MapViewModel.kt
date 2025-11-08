package pt.ipp.estg.readytomove.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.ipp.estg.readytomove.data.geoapify.GeoApifyDataSource
import pt.ipp.estg.readytomove.data.model.OperatorPoint
import pt.ipp.estg.readytomove.data.retrofit.RetrofitHelper
import pt.ipp.estg.readytomove.data.repository.GeoApifyRepository

class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    private val _operatorPoints = MutableStateFlow<List<OperatorPoint>>(emptyList())
    val operatorPoints: StateFlow<List<OperatorPoint>> = _operatorPoints

    private val geoapifyDataSource = GeoApifyDataSource(
        retrofit = RetrofitHelper.retrofit("https://api.geoapify.com/"),
        apiKey = "18d9603dbce24498a3959b908acfa516"
    )

    private val repository = GeoApifyRepository(geoapifyDataSource)

    @SuppressLint("MissingPermission")
    fun getUserLocation() {
        viewModelScope.launch {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                _currentLocation.value = location
            }
        }
    }

    fun loadOperators() {
        viewModelScope.launch {
            _operatorPoints.value = repository.getAllOperators()
        }
    }

}
