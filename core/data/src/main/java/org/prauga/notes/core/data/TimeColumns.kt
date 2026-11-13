// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.data

import androidx.room.ColumnInfo

data class TimeColumns(
    val value: String,
    val zone: String?,
    @ColumnInfo(name = "is_date") val isDate: Boolean,
)
