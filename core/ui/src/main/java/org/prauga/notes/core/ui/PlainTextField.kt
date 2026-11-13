// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.ui

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun PlainTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {},
) {
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    val current = if (field.text == value) field else TextFieldValue(value, TextRange(value.length))
    PlainTextField(
        value = current,
        onValueChange = {
            field = it
            if (it.text != value) onValueChange(it.text)
        },
        placeholder = placeholder,
        style = style,
        modifier = modifier,
        singleLine = singleLine,
        capitalization = capitalization,
        imeAction = imeAction,
        onImeAction = onImeAction,
    )
}

@Composable
fun PlainTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: () -> Unit = {},
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = style,
        singleLine = singleLine,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions =
            KeyboardOptions(
                capitalization = capitalization,
                autoCorrectEnabled = capitalization != KeyboardCapitalization.None,
                imeAction = imeAction,
            ),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        modifier = modifier,
        decorationBox = { inner ->
            if (value.text.isEmpty()) {
                Text(
                    placeholder,
                    style =
                        style.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        ),
                )
            }
            inner()
        },
    )
}
