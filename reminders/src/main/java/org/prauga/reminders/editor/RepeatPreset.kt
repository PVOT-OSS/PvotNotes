// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.reminders.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.prauga.reminders.R

internal enum class RepeatPreset(val rule: String?, val label: Int) {
    NONE(null, R.string.repeat_none),
    DAILY("FREQ=DAILY", R.string.repeat_daily),
    WEEKDAYS("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR", R.string.repeat_weekdays),
    WEEKLY("FREQ=WEEKLY", R.string.repeat_weekly),
    MONTHLY("FREQ=MONTHLY", R.string.repeat_monthly),
    YEARLY("FREQ=YEARLY", R.string.repeat_yearly),
}
