package com.fredrickosuala.recomposition.labs.scopereduction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fredrickosuala.recomposition.core.LogRecomposition
import com.fredrickosuala.recomposition.core.RecompositionCounter
import com.fredrickosuala.recomposition.core.recomposeHighlight
import kotlinx.coroutines.delay

/**
 * Entry composable for the "Recomposition scope — state read too high" lab.
 *
 * A ticker increments every 300 ms. In the naive variant the parent reads the ticker
 * value, making the entire parent the recompose scope. Every tick recomposes the
 * parent AND all four children whose params then change. In the optimized variant
 * only the single leaf that calls the lambda is ever invalidated.
 */
@Composable
fun ScopeReductionLab(optimized: Boolean) {
    var ticker by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(300L)
            ticker++
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = "A ticker fires every 300 ms. Watch which counters spike.",
            style = MaterialTheme.typography.bodyMedium,
        )

        if (optimized) {
            // ✅ FIX: pass a lambda — the parent never reads ticker, so only the leaf
            //         that invokes the lambda becomes the recompose scope.
            OptimizedParent(tickProvider = { ticker })
        } else {
            // ⚠️ PROBLEM: ticker is read here (in NaiveParent) making the whole subtree
            //             recompose on every tick.
            NaiveParent(ticker = ticker)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NaiveParent(
    // ⚠️ PROBLEM: reading `ticker` here makes NaiveParent the recompose scope.
    // Every tick the parent runs again and all children that can't skip recompose too.
    ticker: Int,
) {
    LogRecomposition("NaiveParent")
    Card(modifier = Modifier.fillMaxWidth().recomposeHighlight()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            RecompositionCounter("NaiveParent", modifier = Modifier.align(Alignment.End))

            // Each sibling receives `ticker` even though it doesn't display it.
            // Because the param changes every tick, the siblings cannot be skipped.
            // In real apps this happens whenever a parent drills a volatile value
            // into children that only partially need it.
            NaiveSiblingCard("Sibling A", ticker)
            NaiveSiblingCard("Sibling B", ticker)
            NaiveSiblingCard("Sibling C", ticker)
            NaiveTickerCard(ticker)
        }
    }
}

@Composable
private fun NaiveSiblingCard(
    label: String,
    // ⚠️ PROBLEM: receives a frequently-changing value it never uses → can't skip.
    @Suppress("UNUSED_PARAMETER") ticker: Int,
) {
    LogRecomposition("NaiveSiblingCard($label)")
    Card(
        modifier = Modifier.fillMaxWidth().recomposeHighlight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            RecompositionCounter(label)
        }
    }
}

@Composable
private fun NaiveTickerCard(ticker: Int) {
    LogRecomposition("NaiveTickerCard")
    Card(
        modifier = Modifier.fillMaxWidth().recomposeHighlight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
        ) {
            Text("Tick: $ticker", style = MaterialTheme.typography.bodyMedium)
            RecompositionCounter("TickerCard")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OptimizedParent(
    // ✅ FIX: accept a lambda instead of a value. The parent never reads the ticker,
    //         so it is never added to the snapshot observation set for ticker changes.
    tickProvider: () -> Int,
) {
    LogRecomposition("OptimizedParent")
    Card(modifier = Modifier.fillMaxWidth().recomposeHighlight()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            RecompositionCounter("OptimizedParent", modifier = Modifier.align(Alignment.End))

            // Siblings have no ticker param → their inputs never change → Compose skips them.
            OptimizedSiblingCard("Sibling A")
            OptimizedSiblingCard("Sibling B")
            OptimizedSiblingCard("Sibling C")
            // Only this leaf invokes the lambda, making it the sole recompose scope.
            OptimizedTickerCard(tickProvider)
        }
    }
}

@Composable
private fun OptimizedSiblingCard(label: String) {
    // ✅ FIX: no ticker param → stable, unchanged inputs → Compose skips this every tick.
    LogRecomposition("OptimizedSiblingCard($label)")
    Card(
        modifier = Modifier.fillMaxWidth().recomposeHighlight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            RecompositionCounter(label)
        }
    }
}

@Composable
private fun OptimizedTickerCard(
    // ✅ FIX: only this leaf calls tickProvider(), so only THIS scope is invalidated on each tick.
    tickProvider: () -> Int,
) {
    LogRecomposition("OptimizedTickerCard")
    Card(
        modifier = Modifier.fillMaxWidth().recomposeHighlight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
        ) {
            // ✅ FIX: reading the value HERE makes only this composable's scope recompose.
            Text("Tick: ${tickProvider()}", style = MaterialTheme.typography.bodyMedium)
            RecompositionCounter("TickerCard")
        }
    }
}
