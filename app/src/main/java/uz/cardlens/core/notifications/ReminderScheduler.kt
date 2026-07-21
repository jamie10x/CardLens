package uz.cardlens.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import uz.cardlens.MainActivity
import uz.cardlens.R
import uz.cardlens.core.domain.FollowUp
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "cardlens_followups"
private const val KEY_TITLE = "title"
private const val KEY_CONTACT_ID = "contactId"

class ReminderScheduler(
    private val context: Context
) {
    fun schedule(followUp: FollowUp) {
        val delay = (followUp.dueAt - System.currentTimeMillis()).coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<FollowUpReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    KEY_TITLE to followUp.title,
                    KEY_CONTACT_ID to followUp.contactId
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "follow-up-${followUp.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}

class FollowUpReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        NotificationChannels.ensure(applicationContext)
        val title = inputData.getString(KEY_TITLE) ?: applicationContext.getString(R.string.notification_default_title)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(applicationContext.getString(R.string.notification_content_title))
            .setContentText(title)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        if (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext).notify(title.hashCode(), notification)
        }
        return Result.success()
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(applicationContext, 0, intent, flags)
    }
}

object NotificationChannels {
    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_followups),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }
}
