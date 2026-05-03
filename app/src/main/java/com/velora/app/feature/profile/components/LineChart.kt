package com.velora.app.feature.profile.components

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Smooth animated line chart drawn entirely on Canvas — zero third-party libraries.
 * Demonstrates: Path, cubic Bézier interpolation, Brush.verticalGradient, animateFloatAsState.
 */
@Composable
fun LineChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = lineColor.copy(alpha = 0.15f),
    lineWidth: Dp = 2.5.dp,
    dotRadius: Dp = 4.dp,
) {
    if (dataPoints.size < 2) return

    var targetProgress by remember { mutableFloatStateOf(0f) }
    val drawProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1400, easing = EaseOutCubic),
        label = "chart_progress",
    )
    LaunchedEffect(dataPoints) { targetProgress = 1f }

    val dotColor = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val pad = dotRadius.toPx() + 4.dp.toPx()
        val chartW = w - pad * 2
        val chartH = h - pad * 2
        val minVal = dataPoints.min()
        val maxVal = dataPoints.max()
        val range = (maxVal - minVal).coerceAtLeast(0.01f)

        fun xOf(i: Int) = pad + i * chartW / (dataPoints.size - 1)
        fun yOf(v: Float) = pad + chartH - (v - minVal) / range * chartH

        val pts = dataPoints.indices.map { i -> Offset(xOf(i), yOf(dataPoints[i])) }
        val visible = (pts.size * drawProgress).toInt().coerceAtLeast(1)

        val fillPath = Path().apply {
            moveTo(pts.first().x, h)
            pts.take(visible).forEachIndexed { i, pt ->
                if (i == 0) lineTo(pt.x, pt.y)
                else { val prev = pts[i - 1]; val cpX = (prev.x + pt.x) / 2f
                    cubicTo(cpX, prev.y, cpX, pt.y, pt.x, pt.y) }
            }
            lineTo(pts[visible - 1].x, h)
            close()
        }
        drawPath(fillPath, Brush.verticalGradient(listOf(fillColor, Color.Transparent), pad, h))

        val linePath = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            for (i in 1 until visible) {
                val prev = pts[i - 1]; val curr = pts[i]; val cpX = (prev.x + curr.x) / 2f
                cubicTo(cpX, prev.y, cpX, curr.y, curr.x, curr.y)
            }
        }
        drawPath(linePath, lineColor, style = Stroke(lineWidth.toPx(), cap = StrokeCap.Round))

        pts.take(visible).forEach { pt ->
            drawCircle(lineColor, dotRadius.toPx(), pt)
            drawCircle(dotColor, (dotRadius - 1.5.dp).toPx(), pt)
        }
    }
}

private val EaseOutCubic = Easing { t -> 1f - (1f - t) * (1f - t) * (1f - t) }
