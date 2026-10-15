package com.manav.mindguardai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manav.mindguardai.ui.theme.Palette

@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Palette.Background, Color(0xFF0A1424), Palette.Background)
                )
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 8.dp)
                .size(210.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x2639D7B4), Color(0x0039D7B4))
                    ), CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 60.dp, start = 0.dp)
                .size(180.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x1B9B93FF), Color(0x009B93FF))
                    ), CircleShape
                )
        )
    }
}

@Composable
fun BrandMark(size: Dp = 46.dp) {
    val transition = rememberInfiniteTransition(label = "brand-glow")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(animation = tween(1800), repeatMode = RepeatMode.Reverse),
        label = "brand-pulse"
    )
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size * pulse)
                .background(Palette.Mint.copy(alpha = 0.12f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(size * 0.83f)
                .shadow(10.dp, CircleShape)
                .background(
                    Brush.linearGradient(listOf(Palette.Mint, Color(0xFF78BFFF))),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("m", fontSize = (size.value * 0.48f).sp, color = Palette.Background, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun ScreenTitle(eyebrow: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            eyebrow.uppercase(),
            color = Palette.Mint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp
        )
        Text(
            title,
            color = Palette.Text,
            fontSize = 30.sp,
            lineHeight = 35.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.6).sp
        )
        Text(subtitle, color = Palette.Muted, fontSize = 13.sp, lineHeight = 20.sp)
    }
}

@Composable
fun SectionHeading(title: String, action: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            color = Palette.Text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (action != null) Text(action, color = Palette.Mint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    containerColor: Color = Palette.Surface,
    contentPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .border(1.dp, Palette.Border, shape)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
fun SmallPill(text: String, tint: Color = Palette.Mint, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.18f), CircleShape)
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Text(text, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.3.sp)
    }
}

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingSymbol: String? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Palette.Mint,
            contentColor = Palette.Background,
            disabledContainerColor = Palette.SurfaceSoft,
            disabledContentColor = Palette.Muted
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 1.dp)
    ) {
        if (leadingSymbol != null) {
            Text(leadingSymbol, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(9.dp))
        }
        Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun MetricCard(title: String, value: String, caption: String, accent: Color = Palette.Mint, modifier: Modifier = Modifier) {
    SoftCard(modifier = modifier, contentPadding = 15.dp) {
        Text(title, color = Palette.Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, color = accent, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
        Text(caption, color = Palette.Muted, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

@Composable
fun ActionRow(
    symbol: String,
    title: String,
    subtitle: String,
    tint: Color = Palette.Mint,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Palette.Surface.copy(alpha = 0.92f))
            .border(1.dp, Palette.Border, shape)
            .clickable(onClick = onClick)
            .padding(15.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(symbol, color = tint, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Palette.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Palette.Muted, fontSize = 11.sp, lineHeight = 16.sp)
        }
        Text("›", color = tint, fontSize = 27.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
fun LevelSlider(
    title: String,
    value: Float,
    suffix: String,
    leftLabel: String,
    rightLabel: String,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 1f..10f,
    steps: Int = 8
) {
    SoftCard(contentPadding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Palette.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${value.toInt()}$suffix", color = Palette.Mint, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        val animatedProgress by animateFloatAsState(
            targetValue = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f),
            animationSpec = tween(150), label = "slider-progress"
        )
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(leftLabel, color = Palette.Muted, fontSize = 10.sp)
            Text(rightLabel, color = Palette.Muted, fontSize = 10.sp)
        }
        Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Palette.Track)) {
            Box(Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().background(Palette.Mint, CircleShape))
        }
    }
}

@Composable
fun MiniBar(value: Float, modifier: Modifier = Modifier, tint: Color = Palette.Mint) {
    val animatedValue by animateFloatAsState(targetValue = value.coerceIn(0.06f, 1f), animationSpec = tween(450), label = "mini-bar")
    Box(modifier = modifier.height(76.dp), contentAlignment = Alignment.BottomCenter) {
        Box(Modifier.fillMaxWidth().fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(Palette.Track.copy(alpha = 0.8f)))
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedValue)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(tint, tint.copy(alpha = 0.55f))))
        )
    }
}

@Composable
fun SignalBar(progress: Float, tint: Color = Palette.Mint) {
    val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), animationSpec = tween(650), label = "signal-progress")
    Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Palette.Track)) {
        Box(Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().clip(CircleShape).background(tint))
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Palette.Muted, textAlign = TextAlign.Center, fontSize = 12.sp, lineHeight = 18.sp)
    }
}
