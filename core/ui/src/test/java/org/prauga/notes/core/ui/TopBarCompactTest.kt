// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopBarCompactTest {
    private val threshold = 20f

    @Test
    fun `the bar folds only once the content scrolls past the threshold`() {
        assertFalse(topBarCompact(compact = false, index = 0, offset = 0, thresholdPx = threshold))
        assertFalse(topBarCompact(compact = false, index = 0, offset = 20, thresholdPx = threshold))
        assertTrue(topBarCompact(compact = false, index = 0, offset = 21, thresholdPx = threshold))
        assertTrue(topBarCompact(compact = false, index = 1, offset = 0, thresholdPx = threshold))
    }

    @Test
    fun `a folded bar stays folded until the content is back at the top`() {
        assertTrue(topBarCompact(compact = true, index = 0, offset = 5, thresholdPx = threshold))
        assertFalse(topBarCompact(compact = true, index = 0, offset = 0, thresholdPx = threshold))
    }
}
