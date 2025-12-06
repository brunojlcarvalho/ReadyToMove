package pt.ipp.estg.readytomove.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import pt.ipp.estg.readytomove.data.model.User
import pt.ipp.estg.readytomove.util.NotificationHelper
import com.google.firebase.auth.FirebaseAuth

class LeaderboardWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val auth = FirebaseAuth.getInstance()
            val userId = auth.currentUser?.uid ?: return Result.success() // Se não tiver login, não faz nada

            val db = FirebaseFirestore.getInstance()
            val prefs = applicationContext.getSharedPreferences("leaderboard_prefs", Context.MODE_PRIVATE)

            // 1. Obter o Leaderboard atualizado
            val result = db.collection("users")
                .orderBy("points", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            // 2. Calcular a minha posição atual
            var currentRank = -1
            result.documents.forEachIndexed { index, doc ->
                if (doc.id == userId) {
                    currentRank = index + 1
                }
            }

            if (currentRank == -1) return Result.success() // Não estou no top 50

            // 3. Ler a última posição guardada
            val oldRank = prefs.getInt("last_rank_$userId", -1)

            // 4. Lógica de Notificação (Estava no Top 3 e desceu)
            if (oldRank != -1 && oldRank <= 3 && currentRank > oldRank) {
                NotificationHelper.showRankDropNotification(
                    applicationContext,
                    oldRank,
                    currentRank
                )
            }

            // 5. Atualizar a posição guardada
            prefs.edit().putInt("last_rank_$userId", currentRank).apply()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry() // Tenta de novo se falhar (ex: sem net)
        }
    }
}