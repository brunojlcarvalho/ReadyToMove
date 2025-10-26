package pt.ipp.estg.readytomove.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import pt.ipp.estg.readytomove.data.model.User

object FirebaseAuthHelper {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun login(email: String, password: String): FirebaseUser? {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user
        } catch (e: Exception) {
            null
        }
    }

    suspend fun register(username: String, email: String, password: String): FirebaseUser? {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return null

            val existing = db.collection("users")
                .whereEqualTo("name", username)
                .get()
                .await()

            if (!existing.isEmpty) {
                user.delete().await()
                throw Exception("Username já existente")
            }

            val newUser = User(
                id = user.uid,
                name = username,
                email = email,
                points = 0,
                totalTrips = 0
            )

            db.collection("users").document(user.uid).set(newUser).await()
            user
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun logout() {
        auth.signOut()
    }
}
