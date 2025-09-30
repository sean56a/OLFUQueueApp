package com.example.olfuantipoloregistrarqueueingmanagementsystem.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.olfuantipoloregistrarqueueingmanagementsystem.R
import com.example.olfuantipoloregistrarqueueingmanagementsystem.api.QueueResponse
import com.example.olfuantipoloregistrarqueueingmanagementsystem.api.RequestItem
import com.example.olfuantipoloregistrarqueueingmanagementsystem.api.RetrofitClient
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

class QueueCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "QueueCheckWorker"
        private const val CHANNEL_ID = "queue_status_channel"
        private const val INPUT_STUDENT_NUMBER = "student_number"
        private const val CHECK_INTERVAL_SECONDS = 10L

        // Keep track of last notified status to avoid duplicates
        private var lastNotifiedStatus: MutableMap<String, String> = mutableMapOf()

        fun startWorker(context: Context, studentNumber: String) {
            val data = workDataOf(INPUT_STUDENT_NUMBER to studentNumber)
            val work = OneTimeWorkRequestBuilder<QueueCheckWorker>()
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "queue_check_worker_$studentNumber",
                ExistingWorkPolicy.REPLACE,
                work
            )
            Log.d(TAG, "Worker started for student: $studentNumber")
        }
    }

    override suspend fun doWork(): Result {
        val studentNumber = inputData.getString(INPUT_STUDENT_NUMBER) ?: return Result.failure()
        Log.d(TAG, "doWork called for student: $studentNumber")

        createNotificationChannel()

        return try {
            // Network request with proper exception handling
            val response = try {
                RetrofitClient.instance.getQueueWithRequests(studentNumber).execute()
            } catch (e: UnknownHostException) {
                Log.e(TAG, "Network unavailable or host not resolved: ${e.message}")
                return Result.retry()
            }

            Log.d(TAG, "API response code: ${response.code()}")

            val queueResponse: QueueResponse? = response.body()

            if (queueResponse?.success == true && !queueResponse.requests.isNullOrEmpty()) {
                val userRequest: RequestItem? = queueResponse.requests.firstOrNull {
                    it.student_number.trim() == studentNumber.trim()
                }

                userRequest?.let { request ->
                    val isLine1 = (request.serving_position ?: Int.MAX_VALUE) == 1
                    val shouldNotify = request.status == "Serving" || isLine1

                    val lastStatus = lastNotifiedStatus[studentNumber]
                    if (shouldNotify && lastStatus != request.status) {
                        val title = if (request.status == "Serving") "It's your turn!" else "Almost your turn!"
                        val message = "Your queue number ${request.queueing_num} is line 1 now!"
                        sendNotification(title, message)
                        lastNotifiedStatus[studentNumber] = request.status
                    }

                    Log.d(TAG, "Checked queue: queueNum=${request.queueing_num}, status=${request.status}")
                }
            }

            scheduleNextCheck(studentNumber)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Exception in doWork: ${e.message}", e)
            Result.retry() // Retry on any unexpected exception
        }
    }

    private fun sendNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(applicationContext)) {
            notify(1, builder.build())
        }

        Log.d(TAG, "Notification sent: $title - $message")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Queue Status",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for your queue status"
                    enableLights(true)
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun scheduleNextCheck(studentNumber: String) {
        val data = workDataOf(INPUT_STUDENT_NUMBER to studentNumber)
        val nextWork = OneTimeWorkRequestBuilder<QueueCheckWorker>()
            .setInitialDelay(CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "queue_check_worker_$studentNumber",
            ExistingWorkPolicy.REPLACE,
            nextWork
        )

        Log.d(TAG, "Scheduled next check for student: $studentNumber in $CHECK_INTERVAL_SECONDS seconds")
    }
}
