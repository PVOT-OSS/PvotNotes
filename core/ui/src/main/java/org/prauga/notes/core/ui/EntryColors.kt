// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.prauga.pvot.designsystem.theme.PvotSpacing

/** CSS3 keyword colours, so a colour survives iCalendar's COLOR property exactly. */
enum class EntryColor(val argb: Int, @StringRes val label: Int) {
    RED(0xFFFF6347.toInt(), R.string.ui_color_red),
    ORANGE(0xFFFFA500.toInt(), R.string.ui_color_orange),
    YELLOW(0xFFFFD700.toInt(), R.string.ui_color_yellow),
    GREEN(0xFF3CB371.toInt(), R.string.ui_color_green),
    TEAL(0xFF20B2AA.toInt(), R.string.ui_color_teal),
    BLUE(0xFF6495ED.toInt(), R.string.ui_color_blue),
    PURPLE(0xFFBA55D3.toInt(), R.string.ui_color_purple),
    PINK(0xFFFF69B4.toInt(), R.string.ui_color_pink),
}

@Composable
fun entryContainerColor(argb: Int): Color =
    Color(argb).copy(alpha = 0.3f).compositeOver(MaterialTheme.colorScheme.surface)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryColorRow(selected: Int?, onSelect: (Int?) -> Unit, modifier: Modifier = Modifier) {
    val outline = MaterialTheme.colorScheme.outline
    val ring = MaterialTheme.colorScheme.onSurface
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(PvotSpacing.M),
        verticalArrangement = Arrangement.spacedBy(PvotSpacing.S),
    ) {
        val label = stringResource(R.string.ui_color_none)
        Swatch(selected == null, ring, label, { onSelect(null) }) {
            Box(
                Modifier.size(AppDimens.Swatch)
                    .border(BorderStroke(AppDimens.Hairline, outline), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ui_ic_close),
                    null,
                    Modifier.size(AppDimens.IconS),
                    tint = outline,
                )
            }
        }
        EntryColor.entries.forEach { color ->
            val label = stringResource(color.label)
            Swatch(selected == color.argb, ring, label, { onSelect(color.argb) }) {
                Box(Modifier.size(AppDimens.Swatch).background(Color(color.argb), CircleShape))
            }
        }
    }
}

@Composable
private fun Swatch(
    selected: Boolean,
    ring: Color,
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier.size(AppDimens.TouchTarget)
                .border(
                    BorderStroke(
                        AppDimens.SelectionRing,
                        if (selected) ring else Color.Transparent,
                    ),
                    CircleShape,
                )
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
