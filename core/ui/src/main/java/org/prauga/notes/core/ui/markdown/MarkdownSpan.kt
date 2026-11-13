// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.ui.markdown

sealed interface MarkdownSpan {
    data class Text(val text: String) : MarkdownSpan

    data class Bold(val children: List<MarkdownSpan>) : MarkdownSpan

    data class Italic(val children: List<MarkdownSpan>) : MarkdownSpan

    data class Strike(val children: List<MarkdownSpan>) : MarkdownSpan

    data class Code(val text: String) : MarkdownSpan

    data class Link(val text: String, val url: String) : MarkdownSpan
}
