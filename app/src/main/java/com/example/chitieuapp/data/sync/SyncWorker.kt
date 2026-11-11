package com.example.chitieuapp.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.coroutines.cancellation.CancellationException

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        syncManager.sync()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // Lỗi mạng/Firestore: thử lại với exponential backoff, tối đa 5 lần.
        if (runAttemptCount >= 5) Result.failure() else Result.retry()
    }
}
