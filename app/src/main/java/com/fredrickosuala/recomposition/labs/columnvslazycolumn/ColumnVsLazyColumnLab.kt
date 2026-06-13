package com.fredrickosuala.recomposition.labs.columnvslazycolumn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fredrickosuala.recomposition.core.JankMeter
import com.fredrickosuala.recomposition.core.RecompositionCounter
import com.fredrickosuala.recomposition.core.recomposeHighlight

private const val LIST_SIZE = 100
private val LIST_ITEMS = List(LIST_SIZE) { it }

/**
 * Entry composable for the "Column vs LazyColumn" lab.
 *
 * **Naive:** `Column + Modifier.verticalScroll()` eagerly composes all [LIST_SIZE] items
 * on first render, holding every composable in memory at all times. Press "Recompose"
 * and ALL 100 row counters increment — including rows far off-screen.
 *
 * **Optimized:** `LazyColumn` only composes items near the viewport. Off-screen items
 * are not in composition. Pressing "Recompose" increments counters for only the
 * currently-visible rows; scrolling reveals items composing for the first time.
 *
 * The JankMeter spinner confirms main-thread load: watch it stutter when switching
 * to the naive variant as all 100 items compose simultaneously.
 */
@Composable
fun ColumnVsLazyColumnLab(optimized: Boolean) {
    var recomposeCount by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Press 'Recompose' and count how many row counters increment. " +
                "Naive: all $LIST_SIZE rows. Optimized: only visible rows. " +
                "The JankMeter shows main-thread impact at render time.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
        ) {
            JankMeter()
            Button(onClick = { recomposeCount++ }) {
                Text("Recompose ($recomposeCount)")
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
            if (optimized) {
                OptimizedLazyList(recomposeCount = recomposeCount)
            } else {
                NaiveScrollableColumn(recomposeCount = recomposeCount)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NaiveScrollableColumn(recomposeCount: Int) {
    // ⚠️ PROBLEM: verticalScroll forces Column to eagerly compose ALL LIST_SIZE items upfront.
    // Every item stays in composition forever — even those hundreds of pixels off-screen.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()), // ⚠️ PROBLEM: eager composition of all items
    ) {
        LIST_ITEMS.forEach { index ->
            ListItemRow(index = index, recomposeCount = recomposeCount)
        }
    }
}

@Composable
private fun OptimizedLazyList(recomposeCount: Int) {
    // ✅ FIX: LazyColumn only composes items near the viewport.
    // Off-screen items are decomposed and recomposed only when scrolled back into view.
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(LIST_ITEMS, key = { it }) { index -> // ✅ FIX: only visible items enter composition
            ListItemRow(index = index, recomposeCount = recomposeCount)
        }
    }
}

@Composable
private fun ListItemRow(
    index: Int,
    // Passing recomposeCount as a parameter ensures this composable recomposes when the
    // button is pressed (any stable parameter change prevents skipping).
    // In NaiveScrollableColumn: all 100 rows recompose when recomposeCount changes.
    // In OptimizedLazyList: only the currently-visible rows recompose.
    recomposeCount: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .recomposeHighlight(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            RecompositionCounter("Row $index")
            Text(
                text = "Item $index",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
        }
    }
}
