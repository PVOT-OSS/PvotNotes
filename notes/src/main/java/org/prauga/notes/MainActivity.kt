// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes

import android.content.Intent
import android.content.pm.ShortcutManager
import android.os.Bundle
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.prauga.pvot.designsystem.platform.PvotBaseActivity
import com.prauga.pvot.designsystem.theme.PvotAppTheme

class MainActivity : PvotBaseActivity() {
    private var request by mutableStateOf<NoteEditor?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) request = intent.toEditorRequest()
        setPvotContent {
            PvotAppTheme {
                // PvotScreenScaffold sets no content colour: untinted icons would be black on the
                // dark theme.
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onBackground
                ) {
                    NotesNavigation(request = request, onRequestHandled = { request = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        request = intent.toEditorRequest()
    }

    private fun Intent.toEditorRequest(): NoteEditor? =
        when (action) {
            ACTION_NEW_NOTE -> {
                getSystemService(ShortcutManager::class.java)
                    ?.reportShortcutUsed(NotesApplication.SHORTCUT_NEW_NOTE)
                NoteEditor()
            }
            Intent.ACTION_SEND ->
                NoteEditor(
                    title = getStringExtra(Intent.EXTRA_SUBJECT).orEmpty(),
                    text = getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty(),
                )
            else -> null
        }

    companion object {
        const val ACTION_NEW_NOTE = "org.prauga.notes.action.NEW_NOTE"
    }
}
