package com.fredrickosuala.recomposition.labs.derivedstateof

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fredrickosuala.recomposition.core.LogRecomposition
import com.fredrickosuala.recomposition.core.RecompositionCounter
import com.fredrickosuala.recomposition.core.recomposeHighlight
import kotlinx.coroutines.launch

private val ITEMS = List(60) { "Item ${it + 1}" }

/**
 * Entry composable for the "derivedStateOf" lab.
 *
 * A 60-item list shows a "Scroll to top" button whenever the first visible item is
 * not item 0. The button visibility depends on a boolean, but that boolean is derived
 * from `firstVisibleItemIndex` which changes on every item boundary as the user scrolls.
 *
 * **Naive:** compute the boolean directly in the composable body. Every time
 * `firstVisibleItemIndex` changes (once per item boundary during scroll), the composable
 * containing the button recomposes — even when the RESULT of the comparison is unchanged.
 *
 * **Optimized:** wrap the computation in `derivedStateOf`. Compose now only notifies the
 * reading composable when the *result* changes (false → true or true → false). Scrolling
 * through items 1–59 produces zero recompositions for the button composable.
 */
@Composable
fun DerivedStateOfLab(optimized: Boolean) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "Scroll the list. The button appears once you leave item 1. " +
                "Watch the counter — the naive version recomposes on every item boundary.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(ITEMS) { item ->
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }

            if (optimized) {
                OptimizedScrollToTop(
                    listState = listState,
                    onScrollToTop = {
                        coroutineScope.launch { listState.animateScrollToItem(0) }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                )
            } else {
                NaiveScrollToTop(
                    listState = listState,
                    onScrollToTop = {
                        coroutineScope.launch { listState.animateScrollToItem(0) }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NaiveScrollToTop(
    listState: LazyListState,
    onScrollToTop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ⚠️ PROBLEM: reads firstVisibleItemIndex directly in the composition scope.
    // firstVisibleItemIndex changes on every item boundary while scrolling → this
    // composable recomposes each time, even when the boolean result stays `true`.
    val showButton = listState.firstVisibleItemIndex > 0

    LogRecomposition("NaiveScrollToTop")
    ScrollToTopContent(
        showButton = showButton,
        onScrollToTop = onScrollToTop,
        label = "NaiveScrollToTop",
        modifier = modifier,
    )
}

@Composable
private fun OptimizedScrollToTop(
    listState: LazyListState,
    onScrollToTop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ✅ FIX: derivedStateOf wraps the computation. Compose only invalidates this scope
    // when the *result* changes: false → true (scroll past item 0) or true → false
    // (scroll back to item 0). Scrolling through items 1–59 = zero recompositions here.
    val showButton by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    LogRecomposition("OptimizedScrollToTop")
    ScrollToTopContent(
        showButton = showButton,
        onScrollToTop = onScrollToTop,
        label = "OptimizedScrollToTop",
        modifier = modifier,
    )
}

@Composable
private fun ScrollToTopContent(
    showButton: Boolean,
    onScrollToTop: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.recomposeHighlight()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            RecompositionCounter(label)
            Spacer(Modifier.height(4.dp))
            if (showButton) {
                Button(onClick = onScrollToTop) {
                    Text("↑ Scroll to top")
                }
            } else {
                Text(
                    text = "Scroll down to see the button",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
