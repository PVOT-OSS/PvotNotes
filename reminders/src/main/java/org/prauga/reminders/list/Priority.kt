// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.list

/** iCalendar PRIORITY: 1 is highest, 9 lowest, 0 or none undefined. */
internal enum class Priority(val value: Int?) {
    NONE(null),
    LOW(9),
    MEDIUM(5),
    HIGH(1);

    companion object {
        fun of(value: Int?): Priority =
            when (value) {
                null,
                0 -> NONE
                in 1..4 -> HIGH
                5 -> MEDIUM
                else -> LOW
            }
    }
}
