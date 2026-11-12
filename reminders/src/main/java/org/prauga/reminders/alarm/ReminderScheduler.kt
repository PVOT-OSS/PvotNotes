// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.prauga.notes.core.data.EntryRepository
import org.prauga.notes.core.model.Component

/**
 * Keeps one alarm set for the next reminder of any open task. [sync] notifies whatever came due
 * since the last check (also after a reboot or while the alarm was late), then sets the next one.
 */
class ReminderScheduler(
    private val context: Context,
    private val repository: EntryRepository,
    private val notifier: ReminderNotifier,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val prefs = context.getSharedPreferences("alarms", Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val lock = Mutex()

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    suspend fun sync() =
        lock.withLock {
            val now = clock.instant()
            val zone = ZoneId.systemDefault()
            val tasks = repository.openWithAlarms(Component.VTODO)
            val all = triggers(tasks, zone)
            val lastCheck =
                prefs.getLong(KEY_LAST_CHECK, -1).takeIf { it >= 0 }?.let(Instant::ofEpochMilli)
                    ?: now
            val due = dueBetween(all, lastCheck, now).map { it.taskId }.toSet()
            tasks.filter { it.id in due }.forEach { notifier.notify(it) }
            prefs.edit { putLong(KEY_LAST_CHECK, now.toEpochMilli()) }
            val next = nextAfter(all, now)
            val pending = alarmIntent(null)
            if (next == null) {
                alarmManager.cancel(pending)
            } else {
                set(next.at, pending)
            }
        }

    fun snooze(taskId: Long, minutes: Long = SNOOZE_MINUTES) {
        set(clock.instant().plusSeconds(minutes * 60), alarmIntent(taskId))
    }

    private fun set(at: Instant, pending: PendingIntent) {
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                at.toEpochMilli(),
                pending,
            )
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), pending)
        }
    }

    private fun alarmIntent(taskId: Long?): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        if (taskId != null) intent.putExtra(AlarmReceiver.EXTRA_TASK_ID, taskId)
        val requestCode = taskId?.let { (it % Int.MAX_VALUE).toInt() + 1 } ?: 0
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private companion object {
        const val KEY_LAST_CHECK = "last_check"
        const val SNOOZE_MINUTES = 10L
    }
}
