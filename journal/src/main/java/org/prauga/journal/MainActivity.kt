// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal

import android.content.Intent
import android.content.pm.ShortcutManager
import android.net.Uri
import android.os.Bundle
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import com.prauga.pvot.designsystem.platform.PvotBaseActivity
import com.prauga.pvot.designsystem.theme.PvotAppTheme

class MainActivity : PvotBaseActivity() {
    private var request by mutableStateOf<EntryEditor?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) request = intent.toEditorRequest()
        setPvotContent {
            PvotAppTheme {
                // PvotScreenScaffold sets no content colour; without this, untinted icons and text
                // are
                // black, invisible in the dark theme.
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onBackground
                ) {
                    JournalNavigation(request = request, onRequestHandled = { request = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        request = intent.toEditorRequest()
    }

    private fun Intent.toEditorRequest(): EntryEditor? =
        when (action) {
            ACTION_NEW_ENTRY -> {
                getSystemService(ShortcutManager::class.java)
                    ?.reportShortcutUsed(JournalApplication.SHORTCUT_NEW_ENTRY)
                EntryEditor()
            }
            Intent.ACTION_SEND ->
                EntryEditor(
                    title = getStringExtra(Intent.EXTRA_SUBJECT).orEmpty(),
                    text = getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty(),
                    image =
                        IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
                            ?.takeIf { type?.startsWith("image/") == true }
                            ?.toString(),
                )
            else -> null
        }

    companion object {
        const val ACTION_NEW_ENTRY = "org.prauga.journal.action.NEW_ENTRY"
    }
}
