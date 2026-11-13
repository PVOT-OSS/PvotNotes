// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.alarm

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
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.prauga.notes.core.model.Entry
import org.prauga.reminders.MainActivity
import org.prauga.reminders.R
import org.prauga.reminders.list.DayWords
import org.prauga.reminders.list.dueLabel
import timber.log.Timber

class ReminderNotifier(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannel() {
        val channel =
            NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_HIGH,
                )
                .apply {
                    description = context.getString(R.string.notification_channel_description)
                }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun canNotify(): Boolean = manager.areNotificationsEnabled()

    fun notify(task: Entry) {
        if (!canNotify()) return
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val zone = ZoneId.systemDefault()
        val words =
            DayWords(
                context.getString(R.string.today),
                context.getString(R.string.tomorrow),
                context.getString(R.string.yesterday),
            )
        val text =
            task.due?.let {
                context.getString(
                    R.string.notification_due,
                    dueLabel(it, LocalDate.now(zone), zone, words, Locale.getDefault()),
                )
            }
        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_reminder)
                .setContentTitle(task.summary.ifBlank { context.getString(R.string.app_name) })
                .setContentText(text)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(openIntent(task.id))
                .addAction(
                    0,
                    context.getString(R.string.done),
                    actionIntent(task.id, NotificationActionReceiver.ACTION_DONE, 1),
                )
                .addAction(
                    0,
                    context.getString(R.string.snooze),
                    actionIntent(task.id, NotificationActionReceiver.ACTION_SNOOZE, 2),
                )
                .build()
        try {
            manager.notify(notificationId(task.id), notification)
        } catch (e: SecurityException) {
            Timber.w(e, "Notification permission revoked")
        }
    }

    fun cancel(taskId: Long) = manager.cancel(notificationId(taskId))

    private fun openIntent(taskId: Long): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode(taskId, 0),
            Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_OPEN_TASK)
                .putExtra(MainActivity.EXTRA_TASK_ID, taskId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun actionIntent(taskId: Long, action: String, slot: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode(taskId, slot),
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(action)
                .putExtra(MainActivity.EXTRA_TASK_ID, taskId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun notificationId(taskId: Long) = (taskId % Int.MAX_VALUE).toInt()

    private fun requestCode(taskId: Long, slot: Int) =
        ((taskId % (Int.MAX_VALUE / 3)) * 3 + slot).toInt()

    private companion object {
        const val CHANNEL_ID = "reminders"
    }
}
