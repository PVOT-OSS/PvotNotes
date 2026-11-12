// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon

class JournalApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        val newEntry =
            ShortcutInfo.Builder(this, SHORTCUT_NEW_ENTRY)
                .setShortLabel(getString(R.string.new_entry))
                .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(
                    Intent(MainActivity.ACTION_NEW_ENTRY).setClass(this, MainActivity::class.java)
                )
                .build()
        getSystemService(ShortcutManager::class.java)?.dynamicShortcuts = listOf(newEntry)
    }

    companion object {
        const val SHORTCUT_NEW_ENTRY = "new_entry"
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as JournalApplication).container
