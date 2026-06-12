package com.fredrickosuala.recomposition.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fredrickosuala.recomposition.model.Lab

/**
 * Wrapper for every lab: provides a TopAppBar with back navigation and a debug-log
 * toggle, a bottom bar to switch between the naive and optimised variants, and three
 * explainer cards below the demo.
 *
 * A [RecompositionLogState] is created here and provided via [LocalRecompositionLog] so
 * that any composable in the lab tree can call [LogRecomposition] without prop-drilling.
 * The overlay appears in the top-right corner when the "LOG" action is active.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabScaffold(
    lab: Lab,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var optimized by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }
    val logState = rememberRecompositionLogState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lab.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showLog = !showLog }) {
                        Text(
                            text = "LOG",
                            color = if (showLog) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        bottomBar = {
            NaiveOptimizedToggle(optimized = optimized, onToggle = { optimized = it })
        },
        modifier = modifier,
    ) { paddingValues ->
        CompositionLocalProvider(LocalRecompositionLog provides logState) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    contentPadding = paddingValues,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item { lab.content(optimized) }
                    item { Spacer(Modifier.height(16.dp)) }
                    item {
                        ExplainerCard(
                            title = "⚠️ Problem",
                            body = lab.problemStatement,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    item {
                        ExplainerCard(
                            title = "🔍 How to detect",
                            body = lab.howToDetect,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    item {
                        ExplainerCard(
                            title = "✅ The fix",
                            body = lab.theFix,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                RecompositionLogOverlay(
                    logState = logState,
                    visible = showLog,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(
                            top = paddingValues.calculateTopPadding() + 8.dp,
                            end = 8.dp,
                        )
                        .widthIn(max = 280.dp),
                )
            }
        }
    }
}

@Composable
private fun NaiveOptimizedToggle(
    optimized: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Naive",
                style = MaterialTheme.typography.labelLarge,
                color = if (!optimized) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Switch(
                checked = optimized,
                onCheckedChange = onToggle,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Text(
                text = "Optimized",
                style = MaterialTheme.typography.labelLarge,
                color = if (optimized) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExplainerCard(title: String, body: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
