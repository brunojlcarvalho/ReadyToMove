package pt.ipp.estg.readytomove.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pt.ipp.estg.readytomove.data.model.User
import pt.ipp.estg.readytomove.data.repository.LocalRepository

/**
 * ViewModel responsible for managing User Profile data.
 *
 * This ViewModel handles loading user data from local cache and synchronizing it
 * with Firebase Firestore. It also manages updating user details like name and avatar.
 *
 * @param application The application context.
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val localRepository = LocalRepository.getInstance(application)

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    init {
        loadUserData()
    }

    /**
     * Triggers a refresh of the user data.
     */
    fun refreshUserData() {
        loadUserData()
    }

    /**
     * Loads the user data from local repository first, then fetches the latest
     * data from Firebase Firestore.
     */
    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // 1. First load what we have locally (Instant display)
                val cachedUser = localRepository.getUser(uid)
                cachedUser?.let { _user.value = it }

                // 2. Synchronize with Firebase (Fetch real data from cloud)
                try {
                    val snapshot = db.collection("users").document(uid).get().await()
                    val remoteUser = snapshot.toObject(User::class.java)

                    // If user is found in cloud, update local cache and UI
                    if (remoteUser != null) {
                        // Ensure ID is correct if object comes without it
                        val finalUser = remoteUser.copy(id = uid)

                        localRepository.saveUser(finalUser)
                        _user.value = finalUser
                    }
                } catch (e: Exception) {
                    // If network fails, silently continue showing local data
                    e.printStackTrace()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Updates the user's name in both Firestore and local database.
     *
     * @param newName The new name to set.
     */
    fun updateName(newName: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Update Firebase
                db.collection("users").document(uid)
                    .update("name", newName)
                    .await()

                // Update Local & State
                val updatedUser = _user.value?.copy(name = newName)
                if (updatedUser != null) {
                    _user.value = updatedUser
                    localRepository.saveUser(updatedUser)
                }

            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    /**
     * Updates the user's avatar URL in both Firestore and local database.
     *
     * @param uri The string URI of the new avatar image.
     */
    fun updateAvatar(uri: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Update Firebase
                db.collection("users").document(uid)
                    .update("avatarUrl", uri)
                    .await()

                // Update Local
                val updatedUser = _user.value?.copy(avatarUrl = uri)
                if (updatedUser != null) {
                    _user.value = updatedUser
                    localRepository.saveUser(updatedUser)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Logs out the current user.
     */
    fun logout() {
        viewModelScope.launch {
            auth.signOut()
        }
    }
}