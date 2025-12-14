package pt.ipp.estg.readytomove.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import pt.ipp.estg.readytomove.data.model.User
import pt.ipp.estg.readytomove.data.repository.LocalRepository

/**
 * ViewModel responsible for handling user authentication logic.
 *
 * This ViewModel interacts with Firebase Auth for authentication services and
 * Firestore/LocalRepository for storing user data. It provides methods for
 * registering new users, logging in existing users, and logging out.
 *
 * @param application The application context, used to initialize the LocalRepository.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    // Firebase Authentication instance
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    // Firestore database instance
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    // Local repository for caching user data
    private val localRepository = LocalRepository.getInstance(application)

    /**
     * Registers a new user with the provided details.
     *
     * This function performs the following steps:
     * 1. Creates a user in Firebase Auth.
     * 2. Creates a corresponding User document in Firestore.
     * 3. Saves the user data locally in the Room database.
     * 4. Invokes the callback with the result.
     *
     * @param name The user's name.
     * @param email The user's email address.
     * @param password The user's password.
     * @param onResult Callback function invoked with a success boolean and an optional error message.
     */
    fun registerUser(
        name: String,
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // 1. Create user in Firebase Auth
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("Error obtaining user ID.")

                // 2. Create User object
                val newUser = User(
                    id = uid,
                    name = name,
                    email = email,
                    points = 0,
                    totalTrips = 0,
                    avatarUrl = null
                )

                // 3. Save User to Firestore
                db.collection("users").document(uid).set(newUser).await()

                // 4. Save User to Local Database (Room)
                localRepository.saveUser(newUser)

                // 5. Notify success
                onResult(true, null)
            } catch (e: Exception) {
                // Notify failure with error message
                onResult(false, e.message)
            }
        }
    }

    /**
     * Logs in an existing user with the provided credentials.
     *
     * This function performs the following steps:
     * 1. Signs in the user with Firebase Auth.
     * 2. Fetches the user's profile data from Firestore.
     * 3. Updates the local cache with the fetched user data.
     * 4. Invokes the callback with the result.
     *
     * @param email The user's email address.
     * @param password The user's password.
     * @param onResult Callback function invoked with a success boolean and an optional error message.
     */
    fun loginUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                // 1. Sign in with Firebase Auth
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: throw Exception("User not found.")

                // 2. Fetch User data from Firestore
                val snapshot = db.collection("users").document(uid).get().await()
                val user = snapshot.toObject(User::class.java)

                // 3. Update Local Database
                user?.let {
                    localRepository.saveUser(it)
                }

                // 4. Notify success
                onResult(true, null)
            } catch (e: Exception) {
                // Notify failure
                onResult(false, e.message)
            }
        }
    }

    /**
     * Logs out the current user from Firebase Auth.
     */
    fun logout() {
        auth.signOut()
    }
}