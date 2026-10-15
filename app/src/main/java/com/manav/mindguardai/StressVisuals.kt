package com.manav.mindguardai

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

internal fun stressColor(stress: Float): Color = when {
    stress < 4f -> MG.mint
    stress < 6.5f -> MG.amber
    else -> MG.coral
}

internal fun stressLevel(stress: Float): String = when {
    stress < 4f -> "Low"
    stress < 6.5f -> "Moderate"
    else -> "High"
}

/** Apple-Fitness-style arc gauge that sweeps in with a spring and glows at the tip. */
@Composable
internal fun StressGauge(
    stress: Float?,
    modifier: Modifier = Modifier,
    diameter: Dp = 190.dp,
    caption: String = "AI ESTIMATE"
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(150)
        started = true
    }

    val target =
        if (started && stress != null) ((stress - 1f) / 9f).coerceIn(0f, 1f) else 0f

    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "gauge-sweep"
    )

    val shown by animateFloatAsState(
        targetValue = if (started && stress != null) stress else 1f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "gauge-number"
    )

    val glowT = rememberInfiniteTransition(label = "gauge-glow")
    val glow by glowT.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "glow"
    )

    val tipColor by animateColorAsState(
        if (stress == null) MG.muted else stressColor(stress),
        tween(500),
        label = "gauge-color"
    )

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeW = size.minDimension * 0.075f
            val inset = strokeW * 1.5f
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            // soft halo behind the dial
            drawCircle(
                Brush.radialGradient(
                    listOf(tipColor.copy(alpha = 0.12f * glow), Color.Transparent),
                    center = center,
                    radius = size.minDimension / 2
                ),
                radius = size.minDimension / 2,
                center = center
            )

            // track
            drawArc(
                color = MG.track,
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeW, cap = StrokeCap.Round)
            )

            // tick marks
            for (i in 0..9) {
                val a = Math.toRadians((150.0 + 240.0 * i / 9.0))
                val rOuter = arcSize.width / 2 - strokeW * 1.1f
                val rInner = rOuter - strokeW * 0.45f
                drawLine(
                    color = MG.muted.copy(alpha = 0.35f),
                    start = Offset(
                        center.x + rInner * cos(a).toFloat(),
                        center.y + rInner * sin(a).toFloat()
                    ),
                    end = Offset(
                        center.x + rOuter * cos(a).toFloat(),
                        center.y + rOuter * sin(a).toFloat()
                    ),
                    strokeWidth = strokeW * 0.12f,
                    cap = StrokeCap.Round
                )
            }

            if (progress > 0.005f) {
                // gradient fill: mint -> amber -> coral
                rotate(150f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            0f to MG.mint,
                            0.33f to MG.amber,
                            0.667f to MG.coral,
                            1f to MG.coral
                        ),
                        startAngle = 0f,
                        sweepAngle = 240f * progress.coerceIn(0f, 1.05f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(strokeW, cap = StrokeCap.Round)
                    )
                }

                // glowing tip
                val angle = Math.toRadians((150f + 240f * progress).toDouble())
                val r = arcSize.width / 2
                val tip = Offset(
                    center.x + r * cos(angle).toFloat(),
                    center.y + r * sin(angle).toFloat()
                )
                drawCircle(tipColor.copy(alpha = 0.30f * glow), strokeW * 1.7f, tip)
                drawCircle(Color.White, strokeW * 0.5f, tip)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (stress == null) "--" else String.format(Locale.US, "%.1f", shown),
                color = MG.text,
                fontSize = (diameter.value * 0.19f).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            )
            Text(
                "/ 10",
                color = MG.muted,
                fontSize = maxOf(9f, diameter.value * 0.065f).sp
            )
            Spacer(Modifier.height(3.dp))
            Text(
                if (stress == null) caption else stressLevel(stress).uppercase(),
                color = tipColor,
                fontSize = maxOf(8f, diameter.value * 0.055f).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

/** Breathing glow orb with ripples + slowly spinning gradient (hero decoration). */
@Composable
internal fun BreathingOrb(
    modifier: Modifier = Modifier,
    tint: Color = MG.mint,
    diameter: Dp = 150.dp
) {
    val t = rememberInfiniteTransition(label = "orb")

    val breathe by t.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            tween(3200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val spin by t.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "spin"
    )

    val ripple by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "ripple"
    )

    Canvas(modifier.size(diameter)) {
        val r = size.minDimension / 2

        repeat(2) { k ->
            val p = (ripple + k * 0.5f) % 1f
            drawCircle(
                tint.copy(alpha = (1f - p) * 0.22f),
                radius = r * (0.35f + 0.65f * p),
                center = center,
                style = Stroke(1.5.dp.toPx())
            )
        }

        rotate(spin) {
            drawCircle(
                Brush.sweepGradient(
                    listOf(
                        tint.copy(alpha = 0f),
                        tint.copy(alpha = 0.45f),
                        MG.lavender.copy(alpha = 0.45f),
                        tint.copy(alpha = 0f)
                    ),
                    center = center
                ),
                radius = r * 0.44f * breathe,
                center = center
            )
        }

        drawCircle(
            Brush.radialGradient(
                listOf(tint.copy(alpha = 0.55f), Color.Transparent),
                center = center,
                radius = r * 0.42f * breathe
            ),
            radius = r * 0.42f * breathe,
            center = center
        )
    }
}

internal enum class FLPhase(val label: String, val detail: String) {
    IDLE("Ready", "Run a round to watch federated learning happen step by step."),
    LOCAL("1 · Local training", "Every device trains on its own data. Nothing is uploaded."),
    UPLOAD("2 · Send weights", "Only 5 model numbers travel to the server, never raw check-ins."),
    AGGREGATE("3 · FedAvg", "The server averages the weights, weighted by each device's sample count."),
    BROADCAST("4 · Share global model", "The improved global model returns to every device.")
}

internal data class NetNode(
    val label: String,
    val sub: String,
    val color: Color,
    val isReal: Boolean
)

/** Live network diagram: devices around a server, with packets flowing during each phase. */
@Composable
internal fun FederatedNetworkView(
    nodes: List<NetNode>,
    phase: FLPhase,
    round: Int,
    modifier: Modifier = Modifier
) {
    val t = rememberInfiniteTransition(label = "fl-net")

    val flow by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "flow"
    )

    val spin by t.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "spin"
    )

    val pulse by t.animateFloat(
        0.92f, 1.08f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse"
    )

    val active by animateFloatAsState(
        if (phase == FLPhase.IDLE) 0f else 1f,
        tween(300),
        label = "phase-alpha"
    )

    BoxWithConstraints(modifier.fillMaxWidth().height(340.dp)) {
        val w = maxWidth
        val h = maxHeight
        // leave room for labels above the top device and below the bottom one
        val ring = minOf(w / 2 - 58.dp, h / 2 - 98.dp)

        val angles = nodes.indices.map { i -> Math.toRadians(-90.0 + 360.0 * i / nodes.size) }
        val positions = angles.map { a ->
            Pair(w / 2 + ring * cos(a).toFloat(), h / 2 + ring * sin(a).toFloat())
        }

        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            val pts = positions.map { Offset(it.first.toPx(), it.second.toPx()) }
            val nodeR = 22.dp.toPx()

            // animated dashed links
            pts.forEachIndexed { i, p ->
                drawLine(
                    nodes[i].color.copy(alpha = 0.18f + 0.2f * active),
                    p, c,
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(10f, 10f),
                        phase = -flow * 20f
                    )
                )
            }

            // weight packets
            if (phase == FLPhase.UPLOAD || phase == FLPhase.BROADCAST) {
                pts.forEachIndexed { i, p ->
                    repeat(3) { k ->
                        val f = (flow + k / 3f) % 1f
                        val pos =
                            if (phase == FLPhase.UPLOAD) lerp(p, c, f) else lerp(c, p, f)
                        val col = if (phase == FLPhase.UPLOAD) nodes[i].color else MG.lavender
                        drawCircle(col.copy(alpha = 0.25f * active), 9.dp.toPx(), pos)
                        drawCircle(col.copy(alpha = active), 4.dp.toPx(), pos)
                    }
                }
            }

            // server
            val serverR = 34.dp.toPx() * (if (phase == FLPhase.AGGREGATE) pulse else 1f)
            if (phase == FLPhase.AGGREGATE) {
                repeat(2) { k ->
                    val f = (flow + k * 0.5f) % 1f
                    drawCircle(
                        MG.lavender.copy(alpha = (1f - f) * 0.5f),
                        radius = serverR * (1f + f * 1.2f),
                        center = c,
                        style = Stroke(2.dp.toPx())
                    )
                }
            }
            drawCircle(
                Brush.radialGradient(
                    listOf(MG.lavender.copy(alpha = 0.32f), Color.Transparent),
                    center = c,
                    radius = serverR * 2.2f
                ),
                radius = serverR * 2.2f,
                center = c
            )
            drawCircle(MG.surfaceRaised, serverR, c)
            drawCircle(MG.lavender, serverR, c, style = Stroke(2.dp.toPx()))

            // devices
            pts.forEachIndexed { i, p ->
                val col = nodes[i].color
                drawCircle(col.copy(alpha = 0.14f), nodeR * 1.5f, p)
                drawCircle(MG.surface, nodeR, p)
                drawCircle(col, nodeR, p, style = Stroke(2.dp.toPx()))

                if (phase == FLPhase.LOCAL) {
                    drawArc(
                        color = col,
                        startAngle = spin,
                        sweepAngle = 110f,
                        useCenter = false,
                        topLeft = Offset(p.x - nodeR * 1.35f, p.y - nodeR * 1.35f),
                        size = Size(nodeR * 2.7f, nodeR * 2.7f),
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                if (nodes[i].isReal) {
                    drawCircle(col, nodeR * 0.38f, p)
                } else {
                    drawCircle(col.copy(alpha = 0.5f), nodeR * 0.22f, p)
                }
            }
        }

        Column(
            Modifier
                .offset(x = w / 2 - 40.dp, y = h / 2 - 14.dp)
                .width(80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "SERVER",
                color = MG.lavender,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                "R$round",
                color = MG.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        positions.forEachIndexed { i, (x, y) ->
            // top device: label above it, so it never runs into the server
            val isTop = sin(angles[i]) < -0.5
            val labelY = if (isTop) y - 76.dp else y + 38.dp

            Column(
                Modifier
                    .offset(x = x - 50.dp, y = labelY)
                    .width(100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    nodes[i].label,
                    color = if (nodes[i].isReal) MG.mint else MG.text,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Text(
                    nodes[i].sub,
                    color = MG.muted,
                    fontSize = 8.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** Learning curve that draws itself point by point as new rounds arrive. */
@Composable
internal fun ErrorChart(
    history: List<Double>,
    modifier: Modifier = Modifier
) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    val animCount by animateFloatAsState(
        targetValue = if (started) history.size.toFloat() else 1f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "chart-count"
    )

    Canvas(modifier.fillMaxWidth().height(120.dp)) {
        val n = history.size
        if (n == 0) return@Canvas

        val maxV = (history.maxOrNull() ?: 1.0).coerceAtLeast(0.5)
        val stepX = if (n > 1) size.width / (n - 1) else 0f

        fun pt(i: Int) = Offset(
            if (n > 1) i * stepX else size.width / 2,
            size.height - (history[i] / maxV).toFloat() * size.height * 0.85f - 6f
        )

        for (g in 1..3) {
            val y = size.height * g / 4f
            drawLine(MG.track, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }

        val visible = animCount.coerceIn(1f, n.toFloat())
        val full = floor(visible - 1f).toInt().coerceIn(0, n - 1)
        val frac = (visible - 1f) - full

        val line = Path()
        val first = pt(0)
        line.moveTo(first.x, first.y)
        var last = first

        for (i in 1..full) {
            last = pt(i)
            line.lineTo(last.x, last.y)
        }

        if (frac > 0f && full + 1 < n) {
            last = lerp(pt(full), pt(full + 1), frac)
            line.lineTo(last.x, last.y)
        }

        val fill = Path().apply {
            addPath(line)
            lineTo(last.x, size.height)
            lineTo(first.x, size.height)
            close()
        }

        drawPath(
            fill,
            Brush.verticalGradient(listOf(MG.mint.copy(alpha = 0.28f), Color.Transparent))
        )
        drawPath(
            line,
            MG.mint,
            style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(MG.mint.copy(alpha = 0.3f), 8.dp.toPx(), last)
        drawCircle(Color.White, 3.5.dp.toPx(), last)
    }
}

/** Horizontal bars that grow in one after another (explainability). */
@Composable
internal fun FactorBars(
    factors: List<Factor>,
    modifier: Modifier = Modifier
) {
    val maxImpact = factors.maxOfOrNull { abs(it.impact) }?.coerceAtLeast(0.01) ?: 1.0

    Column(modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        factors.forEachIndexed { i, f ->
            var started by remember(f.name) { mutableStateOf(false) }
            LaunchedEffect(f.name) {
                delay(120L + i * 90L)
                started = true
            }

            val frac by animateFloatAsState(
                targetValue = if (started) (abs(f.impact) / maxImpact).toFloat() else 0f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
                label = "factor-${f.name}"
            )

            val raises = f.impact >= 0
            val col = if (raises) MG.coral else MG.mint

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        f.name,
                        color = MG.text,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        String.format(Locale.US, "%+.2f", f.impact),
                        color = col,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MG.track)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(frac.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(listOf(col.copy(alpha = 0.6f), col))
                            )
                    )
                }
            }
        }
    }
}