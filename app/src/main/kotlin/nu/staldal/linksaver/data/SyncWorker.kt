package nu.staldal.linksaver.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = ItemRepository(applicationContext)
        return try {
            repository.syncToServer()
            repository.refreshFromServer()
            Result.success()
        } catch (e: Exception) {
            Log.w("SyncWorker", "Sync failed: ${e.message}", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
