package com.fredrickosuala.recomposition.labs.expensiveworkinlazy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fredrickosuala.recomposition.core.LogRecomposition

private val ALL_ITEMS: List<String> = List(300) { i ->
    val fruits = listOf(
        "Apple", "Banana", "Cherry", "Date", "Elderberry", "Fig", "Grape",
        "Honeydew", "Kiwi", "Lemon", "Mango", "Nectarine", "Orange", "Papaya",
        "Quince", "Raspberry", "Strawberry", "Tangerine", "Ugli", "Watermelon",
    )
    "${fruits[i % fruits.size]}-${i.toString().padStart(3, '0')}"
}

/**
 * Entry composable for the "Expensive work in composition" lab.
 *
 * A 300-item list is filtered and sorted whenever the composable body runs.
 * Filtering 300 strings is fast alone, but it compounds with every unrelated recomposition.
 *
 * **Naive:** filter+sort is computed directly in the composable body — outside any
 * `remember`. It re-runs on every recomposition: typing, button presses, or any other
 * state change that causes the parent to recompose.
 *
 * **Optimized:** `remember(query)` caches the result. The block only re-executes when
 * `query` changes. Unrelated recompositions skip the computation entirely.
 * In production, go one step further: move this to the ViewModel with a background
 * dispatcher via `Flow.map { }` so the main thread is never touched.
 */
@Composable
fun ExpensiveWorkInLazyLab(optimized: Boolean) {
    var query by remember { mutableStateOf("") }
    var recomposeCount by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Type to filter. Then press 'Recompose' without changing the query. " +
                "Naive: sort re-runs every recomposition. Optimized: sort only re-runs when query changes.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Filter (try 'apple')") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        // Changing recomposeCount recomposes this composable → children recompose.
        // Because both NaiveFilteredList and OptimizedFilteredList take List<String>
        // (an unstable type), they are non-skippable — they always recompose when their
        // parent recomposes. The sort badge then reveals whether the computation ran.
        Button(
            onClick = { recomposeCount++ },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Trigger recomposition ($recomposeCount)")
        }

        Spacer(Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
            if (optimized) {
                OptimizedFilteredList(allItems = ALL_ITEMS, query = query)
            } else {
                NaiveFilteredList(allItems = ALL_ITEMS, query = query)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NaiveFilteredList(
    allItems: List<String>,
    query: String,
) {
    LogRecomposition("NaiveFilteredList")
    val sortRunCount = remember { intArrayOf(0) }

    // ⚠️ PROBLEM: filter+sort is not in remember, so it executes on every recomposition —
    // including when completely unrelated state (the button counter) changes.
    val filtered = allItems
        .filter { query.isEmpty() || it.contains(query, ignoreCase = true) }
        .sortedBy { it }
    sortRunCount[0]++

    Column(modifier = Modifier.fillMaxSize()) {
        SortRunBadge(count = sortRunCount[0], modifier = Modifier.padding(bottom = 4.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered) { item -> FilteredItemRow(item) }
        }
    }
}

@Composable
private fun OptimizedFilteredList(
    allItems: List<String>,
    query: String,
) {
    LogRecomposition("OptimizedFilteredList")
    val sortRunCount = remember { intArrayOf(0) }

    // ✅ FIX: remember(query) caches the sorted result. The block only re-executes when
    // query changes. Pressing "Trigger recomposition" recomposes this composable but the
    // sort is skipped — sortRunCount stays flat while the sort count in the naive version climbs.
    val filtered = remember(query) {
        sortRunCount[0]++
        allItems
            .filter { query.isEmpty() || it.contains(query, ignoreCase = true) }
            .sortedBy { it }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SortRunBadge(count = sortRunCount[0], modifier = Modifier.padding(bottom = 4.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered) { item -> FilteredItemRow(item) }
        }
    }
}

@Composable
private fun FilteredItemRow(item: String, modifier: Modifier = Modifier) {
    Text(
        text = item,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

@Composable
private fun SortRunBadge(count: Int, modifier: Modifier = Modifier) {
    Text(
        text = "Filter+sort ran: ${count}×",
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.tertiary,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
