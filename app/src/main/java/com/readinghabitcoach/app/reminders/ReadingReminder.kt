package com.readinghabitcoach.app.reminders

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.readinghabitcoach.app.MainActivity
import com.readinghabitcoach.app.R
import java.time.*
import java.util.concurrent.TimeUnit

class ReadingReminder(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val hour = LocalTime.now().hour
        if (hour < 7 || hour >= 22) return Result.success()
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 33 && applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        if (!manager.areNotificationsEnabled()) return Result.success()
        manager.createNotificationChannel(NotificationChannel("reading", "Gentle reading reminders", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.notify(100, NotificationCompat.Builder(applicationContext, "reading").setSmallIcon(R.drawable.ic_book)
            .setContentTitle("A few pages, whenever you’re ready")
            .setContentText("Your book is ready whenever you have a few minutes.").setContentIntent(open).setAutoCancel(true).build())
        return Result.success()
    }
    companion object {
        fun schedule(context: Context, enabled: Boolean, hour: Int) {
            val work = WorkManager.getInstance(context)
            if (!enabled) { work.cancelUniqueWork("daily-reading"); return }
            val now = ZonedDateTime.now(); var next = now.withHour(hour.coerceIn(7,21)).withMinute(0).withSecond(0).withNano(0)
            if (next <= now) next = next.plusDays(1)
            val request = PeriodicWorkRequestBuilder<ReadingReminder>(24, TimeUnit.HOURS)
                .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS).build()
            work.enqueueUniquePeriodicWork("daily-reading", ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
