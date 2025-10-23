package pt.ipp.estg.readytomove.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import pt.ipp.estg.readytomove.data.model.User

class ProfileViewModel : ViewModel() {

    private val _user = MutableStateFlow(
        User(
            id = "1",
            name = "Tomás Nunes",
            email = "tomas@readytomove.pt",
            points = 240,
            totalTrips = 15,
            avatarUrl = null
        )
    )
    val user: StateFlow<User> = _user

    fun updateName(newName: String) {
        _user.value = _user.value.copy(name = newName)
    }

    fun addPoints(points: Int) {
        _user.value = _user.value.copy(points = _user.value.points + points)
    }

    fun logout() {
        // FirebaseAuth.getInstance().signOut()
    }
}
