// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import com.prauga.pvot.designsystem.navigation.PvotTopBarDefaults

/**
 * Whether a staggered grid has scrolled far enough for `PvotTopBar` to fold, with the same rule as
 * PvotLib's `rememberPvotTopBarCompact` for lazy lists: past the threshold to fold, back at the
 * very top to unfold.
 */
@Composable
fun rememberTopBarCompact(state: LazyStaggeredGridState): Boolean =
    rememberCompact(state) { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }

/** The same for a screen that scrolls with `Modifier.verticalScroll`. */
@Composable
fun rememberTopBarCompact(state: ScrollState): Boolean = rememberCompact(state) { 0 to state.value }

@Composable
private fun rememberCompact(key: Any, position: () -> Pair<Int, Int>): Boolean {
    val thresholdPx = with(LocalDensity.current) { PvotTopBarDefaults.CompactThreshold.toPx() }
    var compact by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key, thresholdPx) {
        snapshotFlow(position).collect { (index, offset) ->
            compact = topBarCompact(compact, index, offset, thresholdPx)
        }
    }
    return compact
}

internal fun topBarCompact(compact: Boolean, index: Int, offset: Int, thresholdPx: Float): Boolean =
    if (compact) index > 0 || offset > 0 else index > 0 || offset > thresholdPx
