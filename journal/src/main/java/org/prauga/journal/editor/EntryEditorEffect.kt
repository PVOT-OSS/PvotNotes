// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.journal.editor

internal sealed interface EntryEditorEffect {
    data object Closed : EntryEditorEffect
}
