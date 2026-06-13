package com.fredrickosuala.recomposition.labs.drawphaseread

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.State
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fredrickosuala.recomposition.core.LogRecomposition
import com.fredrickosuala.recomposition.core.RecompositionCounter
import com.fredrickosuala.recomposition.core.recomposeHighlight

/**
 * Entry composable for the "Draw-phase read for fast-changing color" lab.
 *
 * A hue value cycles continuously from 0 to 360 (one full revolution per 2 seconds).
 * The resulting color is painted behind a card.
 *
 * **Naive:** `hue` is read via the `by` delegate in the composable body → every animation
 * frame emits a new state value → the composable recomposes every ~16 ms (~60 fps).
 *
 * **Optimized:** keep the `State<Float>` object but do NOT call `.value` in the composable
 * body. Read it inside `Modifier.drawBehind { }` instead. The state change only invalidates
 * the **draw layer** — composition and layout are skipped entirely. The counter stays flat
 * while the background color continues to animate.
 */
@Composable
fun DrawPhaseReadLab(optimized: Boolean) {
    // One shared infinite transition — animation runs the same way in both variants.
    val infiniteTransition = rememberInfiniteTransition(label = "hue")
    val hueState: State<Float> = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "hue",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = "The background color cycles continuously. " +
                "Watch the recomposition counter in each variant.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(12.dp))

        if (optimized) {
            // ✅ FIX: hueState is NOT read here (no `by` delegate). Its value is read only
            //         inside drawBehind (draw phase) → only the draw layer is invalidated.
            OptimizedColorBox(hueState)
        } else {
            // ⚠️ PROBLEM: hueState.value is read in the composable body via the `by` delegate →
            //             every animation frame invalidates the composition scope.
            NaiveColorBox(hueState)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NaiveColorBox(hueState: State<Float>) {
    // ⚠️ PROBLEM: the `by` delegate calls hueState.value in this composable's scope.
    //             Every new animation frame emits a new float → this scope recomposes
    //             at the display refresh rate (~60 fps).
    val hue by hueState

    LogRecomposition("NaiveColorBox")
    Card(modifier = Modifier.fillMaxWidth().recomposeHighlight()) {
        Column(modifier = Modifier.padding(12.dp)) {
            RecompositionCounter("NaiveColorBox")
            Spacer(Modifier.height(8.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    // ⚠️ PROBLEM: `color` is derived from a composition-scope state read.
                    //             Modifier.background() is fine; the problem is WHERE color comes from.
                    .drawBehind { drawRect(Color.hsl(hue, 0.55f, 0.50f)) },
            ) {
                Text(
                    text = "Recomposing every frame",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OptimizedColorBox(hueState: State<Float>) {
    // ✅ FIX: hueState is captured here but its .value is NOT called.
    //         The composable body does not register as a snapshot observer for hue changes.

    LogRecomposition("OptimizedColorBox")
    Card(modifier = Modifier.fillMaxWidth().recomposeHighlight()) {
        Column(modifier = Modifier.padding(12.dp)) {
            RecompositionCounter("OptimizedColorBox")
            Spacer(Modifier.height(8.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    // ✅ FIX: hueState.value is read INSIDE the drawBehind lambda. This lambda
                    //         executes during the draw phase; the snapshot observation is scoped
                    //         to the draw layer, not composition. Only the draw layer is
                    //         re-executed on each frame — composition is never invalidated.
                    .drawBehind { drawRect(Color.hsl(hueState.value, 0.55f, 0.50f)) },
            ) {
                Text(
                    text = "Counter flat — draw layer only",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
