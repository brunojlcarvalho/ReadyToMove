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

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val localRepository = LocalRepository.getInstance(application)

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    init {
        loadUserData()
    }

    fun refreshUserData() {
        loadUserData()
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // 1. Primeiro carrega o que temos localmente (Instantâneo)
                val cachedUser = localRepository.getUser(uid)
                cachedUser?.let { _user.value = it }

                // 2. Sincronizar com Firebase (Vai buscar os dados reais à cloud)
                try {
                    val snapshot = db.collection("users").document(uid).get().await()
                    val remoteUser = snapshot.toObject(User::class.java)

                    // Se encontrarmos o user na cloud, atualizamos a cache local e a UI
                    if (remoteUser != null) {
                        // Garantir que o ID está correto caso o objeto venha sem ID
                        val finalUser = remoteUser.copy(id = uid)

                        localRepository.saveUser(finalUser)
                        _user.value = finalUser
                    }
                } catch (e: Exception) {
                    // Se falhar a net, continuamos a mostrar os dados locais silenciosamente
                    e.printStackTrace()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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

    fun updateAvatar(uri: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Atualizar no Firebase
                db.collection("users").document(uid)
                    .update("avatarUrl", uri)
                    .await()

                // Atualizar Localmente
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

    fun logout() {
        viewModelScope.launch {
            auth.signOut()
        }
    }
}