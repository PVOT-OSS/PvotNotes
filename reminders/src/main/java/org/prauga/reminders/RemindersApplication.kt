// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.prauga.notes.core.model.Component
import timber.log.Timber

class RemindersApplication : Application() {
    internal val container: AppContainer by lazy { AppContainer(this) }

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
            Timber.plant(Timber.DebugTree())
        container.notifier.createChannel()
        val newTask =
            ShortcutInfo.Builder(this, SHORTCUT_NEW_TASK)
                .setShortLabel(getString(R.string.new_reminder))
                .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(
                    Intent(MainActivity.ACTION_NEW_TASK).setClass(this, MainActivity::class.java)
                )
                .build()
        getSystemService(ShortcutManager::class.java)?.dynamicShortcuts = listOf(newTask)
        // Any change to tasks (from any screen or receiver) re-arms the next reminder.
        container.applicationScope.launch {
            container.repository.observeEntries(Component.VTODO).debounce(300).collect {
                container.scheduler.sync()
            }
        }
    }

    companion object {
        const val SHORTCUT_NEW_TASK = "new_task"
    }
}

internal val Context.appContainer: AppContainer
    get() = (applicationContext as RemindersApplication).container
