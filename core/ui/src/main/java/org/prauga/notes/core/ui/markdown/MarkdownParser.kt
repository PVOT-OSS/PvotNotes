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

sealed interface MarkdownSpan {
    data class Text(val text: String) : MarkdownSpan

    data class Bold(val children: List<MarkdownSpan>) : MarkdownSpan

    data class Italic(val children: List<MarkdownSpan>) : MarkdownSpan

    data class Strike(val children: List<MarkdownSpan>) : MarkdownSpan

    data class Code(val text: String) : MarkdownSpan

    data class Link(val text: String, val url: String) : MarkdownSpan
}

object MarkdownParser {
    private val heading = Regex("^(#{1,6})\\s+(.*)$")
    private val task = Regex("^(\\s*)[-*+]\\s+\\[([ xX])]\\s?(.*)$")
    private val bullet = Regex("^(\\s*)[-*+]\\s+(.*)$")
    private val numbered = Regex("^(\\s*)(\\d{1,9})[.)]\\s+(.*)$")
    private val rule = Regex("^\\s*([-*_])(\\s*\\1){2,}\\s*$")
    private val url = Regex("https?://[^\\s<>()]+[^\\s<>().,;:!?'\"]")

    fun parse(source: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val paragraph = mutableListOf<String>()
        val quote = mutableListOf<String>()
        var code: MutableList<String>? = null

        fun flush() {
            if (paragraph.isNotEmpty())
                blocks += MarkdownBlock.Paragraph(paragraph.joinToString("\n"))
            if (quote.isNotEmpty()) blocks += MarkdownBlock.Quote(quote.joinToString("\n"))
            paragraph.clear()
            quote.clear()
        }

        source.lines().forEachIndexed { index, line ->
            val open = code
            if (open != null) {
                if (line.trimStart().startsWith("```")) {
                    blocks += MarkdownBlock.Code(open.joinToString("\n"))
                    code = null
                } else {
                    open += line
                }
                return@forEachIndexed
            }
            if (line.trimStart().startsWith("```")) {
                flush()
                code = mutableListOf()
                return@forEachIndexed
            }
            if (line.isBlank()) {
                flush()
                return@forEachIndexed
            }
            if (line.trimStart().startsWith(">")) {
                if (paragraph.isNotEmpty()) flush()
                quote += line.trimStart().removePrefix(">").removePrefix(" ")
                return@forEachIndexed
            }
            if (quote.isNotEmpty()) flush()
            val block =
                heading.matchEntire(line)?.let {
                    MarkdownBlock.Heading(it.groupValues[1].length, it.groupValues[2].trim())
                }
                    ?: rule.matchEntire(line)?.let { MarkdownBlock.Rule }
                    ?: task.matchEntire(line)?.let {
                        MarkdownBlock.Task(
                            it.groupValues[2] != " ",
                            it.groupValues[3],
                            index,
                            indent(it.groupValues[1]),
                        )
                    }
                    ?: bullet.matchEntire(line)?.let {
                        MarkdownBlock.Bullet(it.groupValues[2], indent(it.groupValues[1]))
                    }
                    ?: numbered.matchEntire(line)?.let {
                        MarkdownBlock.Numbered(
                            it.groupValues[2],
                            it.groupValues[3],
                            indent(it.groupValues[1]),
                        )
                    }
            if (block == null) {
                paragraph += line
            } else {
                flush()
                blocks += block
            }
        }
        code?.let { blocks += MarkdownBlock.Code(it.joinToString("\n")) }
        flush()
        return blocks
    }

    fun parseInline(text: String): List<MarkdownSpan> {
        val spans = mutableListOf<MarkdownSpan>()
        val plain = StringBuilder()
        var i = 0

        fun emitPlain() {
            if (plain.isEmpty()) return
            var last = 0
            url.findAll(plain).forEach { match ->
                if (match.range.first > last)
                    spans += MarkdownSpan.Text(plain.substring(last, match.range.first))
                spans += MarkdownSpan.Link(match.value, match.value)
                last = match.range.last + 1
            }
            if (last < plain.length) spans += MarkdownSpan.Text(plain.substring(last))
            plain.clear()
        }

        fun delimited(marker: String, wrap: (List<MarkdownSpan>) -> MarkdownSpan): Boolean {
            if (!text.startsWith(marker, i)) return false
            val end = text.indexOf(marker, i + marker.length)
            if (end <= i + marker.length || text[i + marker.length].isWhitespace()) return false
            if (marker.length == 1 && marker == "_" && i > 0 && text[i - 1].isLetterOrDigit())
                return false
            emitPlain()
            spans += wrap(parseInline(text.substring(i + marker.length, end)))
            i = end + marker.length
            return true
        }

        while (i < text.length) {
            val c = text[i]
            if (c == '\\' && i + 1 < text.length && text[i + 1] in "\\`*_~[]()#+-.!>") {
                plain.append(text[i + 1])
                i += 2
                continue
            }
            if (c == '`') {
                val end = text.indexOf('`', i + 1)
                if (end > i + 1) {
                    emitPlain()
                    spans += MarkdownSpan.Code(text.substring(i + 1, end))
                    i = end + 1
                    continue
                }
            }
            if (c == '[') {
                val close = text.indexOf("](", i + 1)
                val end = if (close > i) text.indexOf(')', close + 2) else -1
                if (close > i + 1 && end > close + 2) {
                    emitPlain()
                    spans +=
                        MarkdownSpan.Link(
                            text.substring(i + 1, close),
                            text.substring(close + 2, end),
                        )
                    i = end + 1
                    continue
                }
            }
            if (
                delimited("**") { MarkdownSpan.Bold(it) } ||
                    delimited("__") { MarkdownSpan.Bold(it) } ||
                    delimited("~~") { MarkdownSpan.Strike(it) } ||
                    delimited("*") { MarkdownSpan.Italic(it) } ||
                    delimited("_") { MarkdownSpan.Italic(it) }
            ) {
                continue
            }
            plain.append(c)
            i++
        }
        emitPlain()
        return spans
    }

    fun toggleTask(source: String, line: Int): String {
        val lines = source.lines().toMutableList()
        val target = lines.getOrNull(line) ?: return source
        val match = task.matchEntire(target) ?: return source
        val box = match.groups[2]?.range?.first ?: return source
        lines[line] =
            target.replaceRange(box, box + 1, if (match.groupValues[2] == " ") "x" else " ")
        return lines.joinToString("\n")
    }

    private fun indent(whitespace: String): Int = whitespace.replace("\t", "    ").length / 2
}
