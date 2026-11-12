// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.model

/** A file attached to an entry; [uri] is a path inside the app's files directory or a URI. */
data class Attachment(val uri: String, val mimeType: String? = null, val name: String? = null)
