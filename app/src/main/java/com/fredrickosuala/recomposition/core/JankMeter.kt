package com.fredrickosuala.recomposition.core

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * A continuously-spinning arc that makes main-thread jank visually perceptible.
 *
 * The animation is driven by Choreographer frame callbacks (Vsync). When the main thread
 * is blocked — e.g. by a heavy recomposition, synchronous I/O, or a long-running lambda
 * on the UI thread — frame callbacks are delayed and the spinner visibly stutters or
 * freezes for the duration of the block.
 *
 * **How to use:** place this composable in the UI before reproducing a jank scenario
 * (e.g. toggling a naive vs. optimized lab). A smooth spinner means no jank; a
 * visible stutter or freeze confirms the main thread was blocked.
 *
 * **Limitation:** [rememberInfiniteTransition] interpolates values between delivered frames.
 * If one frame is skipped, the spinner *jumps* rather than pauses; very short blocks
 * (<16 ms at 60 Hz) may not be visible at all. For production-grade frame analysis,
 * prefer Android Studio's Frame Rendering view or `adb shell dumpsys gfxinfo`.
 */
@Composable
fun JankMeter(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "JankMeter")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
        ),
        label = "rotation",
    )
    val color = MaterialTheme.colorScheme.primary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Canvas(modifier = Modifier.size(36.dp)) {
            rotate(rotation) {
                drawArc(
                    color = color,
                    startAngle = 0f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        Text(text = "Jank meter", style = MaterialTheme.typography.labelSmall)
    }
}
