// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.ui.markdown

sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock

    data class Paragraph(val text: String) : MarkdownBlock

    data class Bullet(val text: String, val indent: Int) : MarkdownBlock

    data class Numbered(val number: String, val text: String, val indent: Int) : MarkdownBlock

    data class Task(val checked: Boolean, val text: String, val line: Int, val indent: Int) :
        MarkdownBlock

    data class Quote(val text: String) : MarkdownBlock

    data class Code(val text: String) : MarkdownBlock

    data object Rule : MarkdownBlock
}
