// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.notes.core.ui.AppDimens

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxBlocks: Int = Int.MAX_VALUE,
    onToggleTask: ((line: Int) -> Unit)? = null,
) {
    val blocks = remember(markdown) { MarkdownParser.parse(markdown) }
    val shown = blocks.take(maxBlocks)
    val compact = maxBlocks != Int.MAX_VALUE
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (compact) PvotSpacing.XS else PvotSpacing.S),
    ) {
        shown.forEach { block -> Block(block, style.copy(color = color), compact, onToggleTask) }
    }
}

@Composable
private fun Block(
    block: MarkdownBlock,
    style: TextStyle,
    compact: Boolean,
    onToggleTask: ((Int) -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val maxLines = if (compact) 4 else Int.MAX_VALUE
    when (block) {
        is MarkdownBlock.Heading -> {
            val headingStyle =
                when {
                    compact -> typography.titleSmall
                    block.level == 1 -> typography.headlineSmall
                    block.level == 2 -> typography.titleLarge
                    else -> typography.titleMedium
                }
            InlineText(
                block.text,
                headingStyle.copy(color = style.color, fontWeight = FontWeight.SemiBold),
                maxLines,
            )
        }
        is MarkdownBlock.Paragraph -> InlineText(block.text, style, maxLines)
        is MarkdownBlock.Bullet -> ListRow(block.indent, "•", block.text, style, maxLines)
        is MarkdownBlock.Numbered ->
            ListRow(block.indent, "${block.number}.", block.text, style, maxLines)
        is MarkdownBlock.Task ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = (block.indent * 20).dp),
            ) {
                Checkbox(
                    checked = block.checked,
                    onCheckedChange = onToggleTask?.let { toggle -> { toggle(block.line) } },
                    modifier = if (compact) Modifier.size(AppDimens.CompactCheckbox) else Modifier,
                )
                Box(Modifier.width(PvotSpacing.XS))
                InlineText(
                    block.text,
                    if (block.checked) {
                        style.copy(
                            color = style.color.copy(alpha = 0.55f),
                            textDecoration = TextDecoration.LineThrough,
                        )
                    } else {
                        style
                    },
                    maxLines,
                )
            }
        is MarkdownBlock.Quote ->
            Row(Modifier.height(IntrinsicSize.Min)) {
                Box(
                    Modifier.width(AppDimens.QuoteBar)
                        .fillMaxHeight()
                        .background(colors.primary.copy(alpha = 0.6f))
                )
                InlineText(
                    block.text,
                    style.copy(color = colors.onSurfaceVariant, fontStyle = FontStyle.Italic),
                    maxLines,
                    Modifier.padding(start = PvotSpacing.M),
                )
            }
        is MarkdownBlock.Code ->
            Text(
                text = block.text,
                style =
                    style.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = typography.bodyMedium.fontSize,
                    ),
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier.fillMaxWidth()
                        .background(colors.surfaceVariant, RoundedCornerShape(PvotSpacing.S))
                        .padding(horizontal = PvotSpacing.M, vertical = PvotSpacing.S),
            )
        MarkdownBlock.Rule -> HorizontalDivider(Modifier.padding(vertical = PvotSpacing.XS))
    }
}

@Composable
private fun ListRow(indent: Int, marker: String, text: String, style: TextStyle, maxLines: Int) {
    Row(modifier = Modifier.padding(start = (indent * 20).dp)) {
        Text(marker, style = style, modifier = Modifier.width(AppDimens.ListMarker))
        InlineText(text, style, maxLines)
    }
}

@Composable
private fun InlineText(
    text: String,
    style: TextStyle,
    maxLines: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val annotated =
        remember(text, colors.primary, colors.surfaceVariant) {
            buildAnnotatedString {
                append(MarkdownParser.parseInline(text), colors.primary, colors.surfaceVariant)
            }
        }
    Text(
        text = annotated,
        style = style,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

private fun AnnotatedString.Builder.append(
    spans: List<MarkdownSpan>,
    linkColor: Color,
    codeBackground: Color,
) {
    spans.forEach { span ->
        when (span) {
            is MarkdownSpan.Text -> append(span.text)
            is MarkdownSpan.Bold ->
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(span.children, linkColor, codeBackground)
                }
            is MarkdownSpan.Italic ->
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(span.children, linkColor, codeBackground)
                }
            is MarkdownSpan.Strike ->
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    append(span.children, linkColor, codeBackground)
                }
            is MarkdownSpan.Code ->
                withStyle(
                    SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)
                ) {
                    append(span.text)
                }
            is MarkdownSpan.Link ->
                withLink(
                    LinkAnnotation.Url(
                        span.url,
                        TextLinkStyles(
                            SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
                        ),
                    )
                ) {
                    append(span.text)
                }
        }
    }
}
