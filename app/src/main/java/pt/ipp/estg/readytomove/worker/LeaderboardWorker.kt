package pt.ipp.estg.readytomove.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import pt.ipp.estg.readytomove.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth

/**
 * A background worker responsible for checking the user's leaderboard ranking periodically.
 *
 * This worker runs in the background (scheduled via WorkManager) to:
 * 1. Fetch the top 50 users from Firestore.
 * 2. Calculate the current user's rank.
 * 3. Compare the new rank with the previously saved rank.
 * 4. Trigger a notification if the user has dropped in rank (specifically if they were in the top 3).
 *
 * @param context The application context.
 * @param params Parameters for the worker initialization.
 */
class LeaderboardWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    /**
     * Executes the background work.
     *
     * @return The result of the work (Success, Retry, or Failure).
     */
    override suspend fun doWork(): Result {
        return try {
            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid ?: return Result.success()

            val db = FirebaseFirestore.getInstance()
            val prefs = applicationContext.getSharedPreferences("leaderboard_prefs", Context.MODE_PRIVATE)

            // 1. Fetch the leaderboard data (Top 50 by points)
            val result = db.collection("users")
                .orderBy("points", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            // 2. Calculate my current position
            var currentRank = -1
            result.documents.forEachIndexed { index, doc ->
                if (doc.id == userId) {
                    currentRank = index + 1
                }
            }

            // If user is not in the top 50 fetched, we stop here
            if (currentRank == -1) return Result.success()

            val oldRank = prefs.getInt("last_rank_$userId", -1)

            // Logic: Notify only if the user was previously in the top 3 and their rank has dropped
            if (oldRank != -1 && oldRank <= 3 && currentRank > oldRank) {
                NotificationHelper.showRankDropNotification(
                    applicationContext,
                    oldRank,
                    currentRank
                )
            }

            // Save the current rank for the next check
            prefs.edit().putInt("last_rank_$userId", currentRank).apply()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Return retry to let WorkManager try again later if it failed (e.g., network issue)
            Result.retry()
        }
    }
}