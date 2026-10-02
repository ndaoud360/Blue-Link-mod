package com.bluedeck.ui.design

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bluedeck.ui.theme.DeckColors
import com.bluedeck.ui.theme.DeckTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Openings reported by the vehicle, drawn onto the top-down car. */
data class CarOpenings(
    val frontLeft: Boolean = false,
    val frontRight: Boolean = false,
    val rearLeft: Boolean = false,
    val rearRight: Boolean = false,
    val trunk: Boolean = false,
    val hood: Boolean = false
) {
    val any get() = frontLeft || frontRight || rearLeft || rearRight || trunk || hood
}

/**
 * Original stylised top-down EV silhouette (not a reproduction of any specific
 * model artwork). Front faces up when [horizontal] is false, right when true.
 */
@Composable
fun CarTopView(
    modifier: Modifier = Modifier,
    horizontal: Boolean = false,
    openings: CarOpenings = CarOpenings(),
    airflowOn: Boolean = false,
    contentDescription: String = "Vehicle"
) {
    val c = DeckTheme.colors
    val transition = rememberInfiniteTransition(label = "car")
    val flow by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "airflow"
    )
    val glow by animateFloatAsState(if (airflowOn) 1f else 0f, tween(600), label = "airGlow")
    Canvas(modifier.semantics { this.contentDescription = contentDescription }) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val length: Float
        val width: Float
        if (horizontal) {
            length = min(size.width * 0.92f, size.height / 0.46f)
            width = length * 0.44f
        } else {
            length = min(size.height * 0.94f, size.width / 0.46f)
            width = length * 0.44f
        }
        if (horizontal) {
            rotate(90f, center) { drawCar(c, center, length, width, openings, flow, glow) }
        } else {
            drawCar(c, center, length, width, openings, flow, glow)
        }
    }
}

private fun DrawScope.drawCar(
    c: DeckColors,
    center: Offset,
    length: Float,
    width: Float,
    openings: CarOpenings,
    flow: Float,
    glow: Float
) {
    val top = center.y - length / 2f
    val bottom = center.y + length / 2f
    val hw = width / 2f
    val cx = center.x
    fun y(f: Float) = top + length * f

    // Soft ground shadow
    drawOval(
        color = c.shadow.copy(alpha = if (c.isDark) 0.55f else 0.35f),
        topLeft = Offset(cx - hw * 1.1f, y(0.04f)),
        size = Size(hw * 2.2f, length * 0.96f)
    )

    // Wheels (peek out from under the body)
    val wheelW = width * 0.13f
    val wheelL = length * 0.13f
    listOf(0.15f, 0.70f).forEach { fy ->
        listOf(-1f, 1f).forEach { side ->
            drawRoundRect(
                color = Color(0xFF0E1011),
                topLeft = Offset(cx + side * (hw - wheelW * 0.35f) - wheelW / 2f, y(fy)),
                size = Size(wheelW, wheelL),
                cornerRadius = CornerRadius(wheelW / 2.5f)
            )
        }
    }

    val body = Path().apply {
        moveTo(cx, y(0f))
        cubicTo(cx + hw * 0.62f, y(0f), cx + hw * 0.98f, y(0.035f), cx + hw, y(0.17f))
        cubicTo(cx + hw * 1.02f, y(0.42f), cx + hw * 1.03f, y(0.62f), cx + hw * 0.98f, y(0.84f))
        cubicTo(cx + hw * 0.94f, y(0.97f), cx + hw * 0.55f, y(1f), cx, y(1f))
        cubicTo(cx - hw * 0.55f, y(1f), cx - hw * 0.94f, y(0.97f), cx - hw * 0.98f, y(0.84f))
        cubicTo(cx - hw * 1.03f, y(0.62f), cx - hw * 1.02f, y(0.42f), cx - hw, y(0.17f))
        cubicTo(cx - hw * 0.98f, y(0.035f), cx - hw * 0.62f, y(0f), cx, y(0f))
        close()
    }
    drawPath(
        body,
        Brush.horizontalGradient(
            0f to c.carBody.copy(alpha = 0.78f),
            0.5f to c.carBody,
            1f to c.carBody.copy(alpha = 0.78f),
            startX = cx - hw,
            endX = cx + hw
        )
    )
    // Specular ridge
    clipPath(body) {
        drawRect(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.18f), Color.Transparent, Color.White.copy(alpha = 0.06f)),
                startY = top, endY = bottom
            )
        )
    }
    drawPath(body, Color.White.copy(alpha = if (c.isDark) 0.18f else 0.30f), style = Stroke(width = 1.4f))

    // Mirrors
    listOf(-1f, 1f).forEach { side ->
        drawOval(
            c.carBody,
            topLeft = Offset(cx + side * hw * 1.02f - width * 0.05f, y(0.30f)),
            size = Size(width * 0.10f, length * 0.035f)
        )
    }

    // Glasshouse: one continuous canopy (windshield + glass roof + rear glass)
    val glass = Path().apply {
        val gw = hw * 0.76f
        moveTo(cx, y(0.27f))
        cubicTo(cx + gw * 0.8f, y(0.27f), cx + gw, y(0.31f), cx + gw, y(0.40f))
        lineTo(cx + gw * 0.96f, y(0.74f))
        cubicTo(cx + gw * 0.9f, y(0.85f), cx + gw * 0.5f, y(0.87f), cx, y(0.87f))
        cubicTo(cx - gw * 0.5f, y(0.87f), cx - gw * 0.9f, y(0.85f), cx - gw * 0.96f, y(0.74f))
        lineTo(cx - gw, y(0.40f))
        cubicTo(cx - gw, y(0.31f), cx - gw * 0.8f, y(0.27f), cx, y(0.27f))
        close()
    }
    drawPath(
        glass,
        Brush.verticalGradient(
            listOf(c.carGlass.copy(alpha = 0.92f), c.carGlass, c.carGlass.copy(alpha = 0.85f)),
            startY = y(0.27f), endY = y(0.87f)
        )
    )
    // Roof rails and B-pillar hints
    listOf(0.43f, 0.62f).forEach { fy ->
        drawLine(c.carBody.copy(alpha = 0.55f), Offset(cx - hw * 0.74f, y(fy)), Offset(cx + hw * 0.74f, y(fy)), strokeWidth = 2f)
    }
    // Light bars
    drawLine(Color.White.copy(alpha = 0.55f), Offset(cx - hw * 0.62f, y(0.012f)), Offset(cx + hw * 0.62f, y(0.012f)), strokeWidth = 2.2f, cap = StrokeCap.Round)
    drawLine(Color(0xFFE35D5D).copy(alpha = 0.7f), Offset(cx - hw * 0.7f, y(0.99f)), Offset(cx + hw * 0.7f, y(0.99f)), strokeWidth = 2.2f, cap = StrokeCap.Round)

    // Openings
    val alert = c.caution
    fun door(side: Float, from: Float, to: Float) {
        val hinge = Offset(cx + side * hw * 1.0f, y(from))
        val tip = Offset(cx + side * (hw + width * 0.32f), y(to - 0.03f))
        drawLine(alert, hinge, tip, strokeWidth = 5f, cap = StrokeCap.Round)
    }
    if (openings.frontLeft) door(-1f, 0.36f, 0.52f)
    if (openings.frontRight) door(1f, 0.36f, 0.52f)
    if (openings.rearLeft) door(-1f, 0.53f, 0.68f)
    if (openings.rearRight) door(1f, 0.53f, 0.68f)
    if (openings.hood) {
        drawArc(alert, 200f, 140f, false, Offset(cx - hw * 0.8f, y(-0.05f)), Size(hw * 1.6f, length * 0.18f), style = Stroke(5f, cap = StrokeCap.Round))
    }
    if (openings.trunk) {
        drawArc(alert, 20f, 140f, false, Offset(cx - hw * 0.8f, y(0.87f)), Size(hw * 1.6f, length * 0.18f), style = Stroke(5f, cap = StrokeCap.Round))
    }

    // Airflow: four chevrons converging into the cabin, as in the concept
    if (glow > 0.01f) {
        val vents = listOf(
            Offset(cx - hw * 0.42f, y(0.42f)) to 45f,
            Offset(cx + hw * 0.42f, y(0.42f)) to 135f,
            Offset(cx - hw * 0.42f, y(0.70f)) to -45f,
            Offset(cx + hw * 0.42f, y(0.70f)) to -135f
        )
        vents.forEachIndexed { i, (pos, angle) ->
            val phase = (flow + i * 0.25f) % 1f
            val alpha = (sin(phase * PI).toFloat()) * glow
            val rad = angle * PI.toFloat() / 180f
            val travel = width * 0.07f * phase
            val p = Offset(pos.x + cos(rad) * travel, pos.y + sin(rad) * travel)
            val s = width * 0.06f
            val a1 = rad + 2.5f
            val a2 = rad - 2.5f
            drawLine(Color.White.copy(alpha = alpha), p, Offset(p.x + cos(a1) * s, p.y + sin(a1) * s), 3f, StrokeCap.Round)
            drawLine(Color.White.copy(alpha = alpha), p, Offset(p.x + cos(a2) * s, p.y + sin(a2) * s), 3f, StrokeCap.Round)
        }
    }
}

/** Vertical cell from the "Battery Status" concept. Fill animates; a shine travels up while charging. */
@Composable
fun BatteryCell(
    percent: Int,
    charging: Boolean,
    modifier: Modifier = Modifier,
    targetPercent: Int? = null
) {
    val c = DeckTheme.colors
    val level by animateFloatAsState((percent.coerceIn(0, 100)) / 100f, tween(900, easing = FastOutSlowInEasing), label = "cell")
    val transition = rememberInfiniteTransition(label = "cellShine")
    val shine by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "shine"
    )
    val fillColor = when {
        percent <= 10 -> c.danger
        percent <= 20 -> c.caution
        else -> c.accent
    }
    Box(
        modifier.semantics {
            this.contentDescription = "Battery $percent percent" + if (charging) ", charging" else ""
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val capH = size.height * 0.05f
            val bodyTop = capH + 4.dp.toPx()
            val bodyRect = Rect(0f, bodyTop, size.width, size.height)
            val radius = CornerRadius(size.width * 0.16f)
            // Terminal
            drawRoundRect(
                c.hairline,
                topLeft = Offset(size.width * 0.34f, 0f),
                size = Size(size.width * 0.32f, capH + 6f),
                cornerRadius = CornerRadius(capH / 2f)
            )
            // Shell
            drawRoundRect(c.cardPressed, bodyRect.topLeft, bodyRect.size, radius)
            drawRoundRect(c.hairline, bodyRect.topLeft, bodyRect.size, radius, style = Stroke(2.dp.toPx()))
            // Fill
            val inset = 6.dp.toPx()
            val inner = Rect(inset, bodyTop + inset, size.width - inset, size.height - inset)
            val fillTop = inner.bottom - inner.height * level
            val clip = Path().apply {
                addRoundRect(androidx.compose.ui.geometry.RoundRect(inner, CornerRadius(size.width * 0.11f)))
            }
            clipPath(clip) {
                drawRect(
                    Brush.verticalGradient(listOf(fillColor.copy(alpha = 0.55f), fillColor.copy(alpha = 0.95f)), startY = fillTop, endY = inner.bottom),
                    topLeft = Offset(inner.left, fillTop),
                    size = Size(inner.width, inner.bottom - fillTop)
                )
                // Cell separators
                for (i in 1..5) {
                    val yy = inner.top + inner.height * i / 6f
                    drawLine(c.background.copy(alpha = 0.18f), Offset(inner.left, yy), Offset(inner.right, yy), 1.5f)
                }
                if (charging) {
                    val band = inner.height * 0.22f
                    val yy = inner.bottom - (inner.height + band) * shine
                    drawRect(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.28f), Color.Transparent), startY = yy, endY = yy + band),
                        topLeft = Offset(inner.left, yy),
                        size = Size(inner.width, band)
                    )
                }
            }
            // Target marker
            if (targetPercent != null && targetPercent in 1..99) {
                val ty = inner.bottom - inner.height * (targetPercent / 100f)
                drawLine(
                    c.text.copy(alpha = 0.55f),
                    Offset(inner.left - 2f, ty), Offset(inner.right + 2f, ty),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$percent%", style = MaterialTheme.typography.displaySmall, color = c.text)
            if (charging) Icon(Icons.Filled.Bolt, null, tint = c.text.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
        }
    }
}

/** Compact progress ring with a bolt — the concept's battery-card glyph. */
@Composable
fun ChargeRing(percent: Int, charging: Boolean, modifier: Modifier = Modifier, diameter: Dp = 40.dp) {
    val c = DeckTheme.colors
    val sweep by animateFloatAsState(percent.coerceIn(0, 100) * 3.6f, tween(900), label = "ring")
    val transition = rememberInfiniteTransition(label = "ringPulse")
    val pulse by transition.animateFloat(0.55f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "pulse")
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 3.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(c.hairline, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(c.accent, -90f, sweep, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Icon(
            Icons.Filled.Bolt, null,
            tint = if (charging) c.accent.copy(alpha = pulse) else c.textFaint,
            modifier = Modifier.size(diameter * 0.45f)
        )
    }
}

/**
 * 270° dial from the climate concept. Drag along the arc or tap to set; [onChange]
 * receives values snapped to [step]. Centre content is supplied by the caller.
 */
@Composable
fun ArcDial(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    enabled: Boolean = true,
    center: @Composable BoxScope.() -> Unit
) {
    val c = DeckTheme.colors
    val onChangeState = rememberUpdatedState(onChange)
    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    val animated by animateFloatAsState(fraction, tween(250), label = "dial")
    val startAngle = 135f
    val sweepTotal = 270f

    fun valueFor(pos: Offset, sz: androidx.compose.ui.unit.IntSize): Float? {
        val cx = sz.width / 2f
        val cy = sz.height / 2f
        var deg: Float = (atan2((pos.y - cy).toDouble(), (pos.x - cx).toDouble()) * 180.0 / PI).toFloat()
        if (deg < 0) deg += 360f
        var rel = deg - startAngle
        if (rel < 0) rel += 360f
        if (rel > sweepTotal) {
            // In the dead zone at the bottom: snap to the nearer end
            rel = if (rel - sweepTotal < (360f - rel)) sweepTotal else 0f
        }
        val raw = range.start + (rel / sweepTotal) * (range.endInclusive - range.start)
        val snapped = (raw / step).roundToInt() * step
        return snapped.coerceIn(range.start, range.endInclusive)
    }

    Box(
        modifier
            .then(
                if (enabled) Modifier
                    .pointerInput(range, step) {
                        detectTapGestures { pos -> valueFor(pos, size)?.let { onChangeState.value(it) } }
                    }
                    .pointerInput(range, step) {
                        detectDragGestures { change, _ ->
                            valueFor(change.position, size)?.let { onChangeState.value(it) }
                        }
                    }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val pad = stroke + 6.dp.toPx()
            val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
            val tl = Offset(pad, pad)
            drawArc(c.hairline, startAngle, sweepTotal, false, tl, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            val tint = if (active) c.accent else c.textFaint
            drawArc(tint, startAngle, sweepTotal * animated, false, tl, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            // Knob
            val angle = (startAngle + sweepTotal * animated) * PI.toFloat() / 180f
            val r = arcSize.width / 2f
            val knob = Offset(size.width / 2f + cos(angle) * r, size.height / 2f + sin(angle) * r)
            drawCircle(c.cardRaised, radius = stroke * 1.5f, center = knob)
            drawCircle(tint, radius = stroke * 1.5f, center = knob, style = Stroke(2.dp.toPx()))
        }
        center()
    }
}

/** Plug → car energy path; dashes travel while energy is flowing. */
@Composable
fun EnergyFlowLine(flowing: Boolean, modifier: Modifier = Modifier) {
    val c = DeckTheme.colors
    val transition = rememberInfiniteTransition(label = "energy")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "phase")
    Canvas(modifier) {
        val yMid = size.height / 2f
        val dash = 10.dp.toPx()
        val gap = 8.dp.toPx()
        val color = if (flowing) c.accent else c.hairline
        drawLine(
            color,
            Offset(0f, yMid), Offset(size.width, yMid),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), if (flowing) -(dash + gap) * phase else 0f)
        )
        if (flowing) {
            val head = size.width * phase
            drawCircle(c.accent.copy(alpha = 0.35f), radius = 7.dp.toPx(), center = Offset(head, yMid))
        }
    }
}
