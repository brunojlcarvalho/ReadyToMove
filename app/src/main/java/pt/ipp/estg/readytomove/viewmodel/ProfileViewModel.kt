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

    // --- NOVO MÉTODO ---
    // Chamado pelo UI para forçar atualização dos dados (pontos/trips)
    fun refreshUserData() {
        loadUserData()
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // 1. Tenta carregar do Room (Cache local - mais rápido)
                val cachedUser = localRepository.getUser(uid)
                cachedUser?.let { _user.value = it }

                // 2. (Opcional) Sincronizar com Firebase se tiveres backend
                /*
                val snapshot = db.collection("users").document(uid).get().await()
                val remoteUser = snapshot.toObject(User::class.java)
                remoteUser?.let {
                    localRepository.saveUser(it)
                    _user.value = it
                }
                */
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
                _user.value = updatedUser
                updatedUser?.let { localRepository.saveUser(it) }

            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Opcional: Limpar dados locais ao sair
            // _user.value?.let { localRepository.deleteUser(it) }
            auth.signOut()
        }
    }
}