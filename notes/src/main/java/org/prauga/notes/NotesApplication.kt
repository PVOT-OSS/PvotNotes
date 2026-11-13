// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import timber.log.Timber

class NotesApplication : Application() {
    internal val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
            Timber.plant(Timber.DebugTree())
        val newNote =
            ShortcutInfo.Builder(this, SHORTCUT_NEW_NOTE)
                .setShortLabel(getString(R.string.new_note))
                .setIcon(Icon.createWithResource(this, R.drawable.ic_shortcut_new_note))
                .setIntent(
                    Intent(MainActivity.ACTION_NEW_NOTE).setClass(this, MainActivity::class.java)
                )
                .build()
        getSystemService(ShortcutManager::class.java)?.dynamicShortcuts = listOf(newNote)
    }

    companion object {
        const val SHORTCUT_NEW_NOTE = "new_note"
    }
}

internal val Context.appContainer: AppContainer
    get() = (applicationContext as NotesApplication).container
