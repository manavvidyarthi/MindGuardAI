package com.manav.mindguardai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manav.mindguardai.ui.components.LevelSlider
import com.manav.mindguardai.ui.components.PrimaryActionButton
import com.manav.mindguardai.ui.components.ScreenTitle
import com.manav.mindguardai.ui.components.SoftCard
import com.manav.mindguardai.ui.theme.Palette

private data class MoodChoice(val value: Int, val emoji: String, val label: String)

private val moodChoices = listOf(
    MoodChoice(1, "😔", "Low"),
    MoodChoice(2, "😕", "Meh"),
    MoodChoice(3, "😐", "Okay"),
    MoodChoice(4, "🙂", "Good"),
    MoodChoice(5, "😄", "Great")
)

@Composable
fun CheckInScreen(
    mood: Int,
    stress: Float,
    sleep: Float,
    energy: Float,
    onMoodChange: (Int) -> Unit,
    onStressChange: (Float) -> Unit,
    onSleepChange: (Float) -> Unit,
    onEnergyChange: (Float) -> Unit,
    savedMessage: String,
    onSave: (Int, Int, Int, Int) -> Unit
) {
    var saved by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 17.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        ScreenTitle(
            eyebrow = "A moment for you",
            title = "How are you,\nreally?",
            subtitle = "There is no perfect answer. Just choose what feels closest right now."
        )

        SoftCard {
            Text("Your mood", color = Palette.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Pick the face that best matches this moment.", color = Palette.Muted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                moodChoices.forEach { choice ->
                    MoodOption(
                        choice = choice,
                        selected = mood == choice.value,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onMoodChange(choice.value)
                            saved = false
                        }
                    )
                }
            }
        }

        LevelSlider(
            title = "Stress level",
            value = stress,
            suffix = "/10",
            leftLabel = "Calm",
            rightLabel = "Very high",
            onValueChange = { onStressChange(it); saved = false }
        )
        LevelSlider(
            title = "Energy",
            value = energy,
            suffix = "/10",
            leftLabel = "Drained",
            rightLabel = "Energised",
            onValueChange = { onEnergyChange(it); saved = false }
        )
        LevelSlider(
            title = "Sleep last night",
            value = sleep,
            suffix = " h",
            leftLabel = "0 hours",
            rightLabel = "12 hours",
            onValueChange = { onSleepChange(it); saved = false },
            valueRange = 0f..12f,
            steps = 11
        )

        PrimaryActionButton(
            text = "Save my reflection",
            onClick = {
                onSave(mood, stress.toInt(), sleep.toInt(), energy.toInt())
                saved = true
            },
            leadingSymbol = "✓"
        )

        AnimatedVisibility(
            visible = saved,
            enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 5 }
        ) {
            SoftCard(containerColor = Palette.SurfaceRaised) {
                Text("Reflection saved ✨", color = Palette.Mint, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(savedMessage, color = Palette.Text, fontSize = 12.sp, lineHeight = 18.sp)
                Text("Your check-ins stay on this device in this demo.", color = Palette.Muted, fontSize = 10.sp)
            }
        }

        Text(
            "Your mood is information, not a grade. If today feels difficult, you deserve support regardless of any number here.",
            color = Palette.Muted,
            fontSize = 11.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp)
        )
    }
}

@Composable
private fun MoodOption(
    choice: MoodChoice,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) Palette.Mint.copy(alpha = 0.13f) else Palette.BackgroundRaised,
        animationSpec = tween(180),
        label = "mood-option-background"
    )
    val border by animateColorAsState(
        targetValue = if (selected) Palette.Mint.copy(alpha = 0.78f) else Palette.Border,
        animationSpec = tween(180),
        label = "mood-option-border"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(17.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(17.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(choice.emoji, fontSize = 23.sp)
        Text(
            choice.label,
            color = if (selected) Palette.Mint else Palette.Muted,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
