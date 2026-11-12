// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders

import android.content.Intent
import android.content.pm.ShortcutManager
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.prauga.pvot.designsystem.platform.PvotBaseActivity
import com.prauga.pvot.designsystem.theme.PvotAppTheme

class MainActivity : PvotBaseActivity() {
    private var request by mutableStateOf<TaskEditor?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) request = intent.toEditorRequest()
        setPvotContent {
            PvotAppTheme {
                RemindersNavigation(request = request, onRequestHandled = { request = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        request = intent.toEditorRequest()
    }

    private fun Intent.toEditorRequest(): TaskEditor? =
        when (action) {
            ACTION_NEW_TASK -> {
                getSystemService(ShortcutManager::class.java)
                    ?.reportShortcutUsed(RemindersApplication.SHORTCUT_NEW_TASK)
                TaskEditor()
            }
            ACTION_OPEN_TASK ->
                getLongExtra(EXTRA_TASK_ID, 0).takeIf { it > 0 }?.let { TaskEditor(id = it) }
            Intent.ACTION_SEND ->
                TaskEditor(
                    title = getStringExtra(Intent.EXTRA_SUBJECT).orEmpty(),
                    text = getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty(),
                )
            else -> null
        }

    companion object {
        const val ACTION_NEW_TASK = "org.prauga.reminders.action.NEW_TASK"
        const val ACTION_OPEN_TASK = "org.prauga.reminders.action.OPEN_TASK"
        const val EXTRA_TASK_ID = "task_id"
    }
}
