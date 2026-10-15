package com.manav.mindguardai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manav.mindguardai.data.WellbeingSnapshot
import com.manav.mindguardai.ui.components.ActionRow
import com.manav.mindguardai.ui.components.BrandMark
import com.manav.mindguardai.ui.components.EmptyState
import com.manav.mindguardai.ui.components.MetricCard
import com.manav.mindguardai.ui.components.MiniBar
import com.manav.mindguardai.ui.components.PrimaryActionButton
import com.manav.mindguardai.ui.components.SectionHeading
import com.manav.mindguardai.ui.components.SoftCard
import com.manav.mindguardai.ui.components.SmallPill
import com.manav.mindguardai.ui.theme.Palette

@Composable
fun HomeScreen(
    snapshot: WellbeingSnapshot,
    onCheckIn: () -> Unit,
    onAnalyze: () -> Unit,
    onFamily: () -> Unit,
    onLearningLab: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 15.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(19.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandMark(48.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("MindGuard", color = Palette.Text, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                Text("Your personal wellbeing space", color = Palette.Muted, fontSize = 11.sp)
            }
            SmallPill("PRIVATE BY DESIGN")
        }

        HeroCard(onCheckIn)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard(
                title = "CHECK-INS",
                value = snapshot.count.toString(),
                caption = if (snapshot.count == 1) "reflection saved" else "reflections saved",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "LATEST MOOD",
                value = snapshot.latest?.let { "${it.mood}/5" } ?: "—",
                caption = snapshot.latest?.dateLabel() ?: "your first one starts here",
                accent = Palette.Lavender,
                modifier = Modifier.weight(1f)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            SectionHeading("Your recent rhythm", "Last 7 entries")
            SoftCard(contentPadding = 17.dp) {
                if (snapshot.entries.isEmpty()) {
                    EmptyState("Your mood rhythm will appear here after your first check-in. No sample data is shown as your own.")
                } else {
                    val recent = snapshot.entries.takeLast(7)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        recent.forEach { entry ->
                            val tint = when {
                                entry.mood <= 2 -> Palette.Coral
                                entry.mood == 3 -> Palette.Lavender
                                else -> Palette.Mint
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Text("${entry.mood}", color = Palette.Text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                MiniBar(value = entry.mood / 5f, tint = tint, modifier = Modifier.fillMaxWidth())
                                Text(entry.dayLabel(), color = Palette.Muted, fontSize = 9.sp)
                            }
                        }
                    }
                    Text("A reflection journal, not a judgement of your mental health.", color = Palette.Muted, fontSize = 10.sp)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            SectionHeading("Choose your next step")
            ActionRow("♡", "Daily check-in", "Mood, stress, energy and sleep in under a minute.", onClick = onCheckIn)
            ActionRow("✦", "Reflect on your words", "Explore language in text you choose to enter.", tint = Palette.Lavender, onClick = onAnalyze)
            ActionRow("↗", "Family support", "Create a message only when you choose to share.", tint = Palette.Blue, onClick = onFamily)
            ActionRow("◎", "Federated learning lab", "Explore a small on-device learning simulation.", tint = Palette.Amber, onClick = onLearningLab)
        }

        Text(
            "MindGuard is an educational demo, not a medical device or a diagnostic tool. Your entries are stored locally on this device.",
            color = Palette.Muted.copy(alpha = 0.82f),
            fontSize = 10.sp,
            lineHeight = 15.sp,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
        )
    }
}

@Composable
private fun HeroCard(onCheckIn: () -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF203A4B), Color(0xFF22304D), Color(0xFF312D52))))
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-22).dp)
                .size(150.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x5476EBCB), Color(0x0076EBCB))),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 38.dp, y = 42.dp)
                .size(135.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0x38A7A2FF), Color(0x00A7A2FF))),
                    CircleShape
                )
        )
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            SmallPill("YOUR SPACE, YOUR PACE", tint = Palette.Mint)
            Text(
                "Make space\nfor how you feel.",
                color = Palette.Text,
                fontSize = 31.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.9).sp
            )
            Text(
                "Small check-ins. Gentle insights. No judgement.",
                color = Color(0xFFD0DBE9),
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(1.dp))
            PrimaryActionButton("Start a check-in", onCheckIn, leadingSymbol = "+")
        }
    }
}
