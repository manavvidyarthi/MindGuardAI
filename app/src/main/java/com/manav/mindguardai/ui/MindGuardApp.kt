package com.manav.mindguardai.ui

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.manav.mindguardai.data.CheckInEntry
import com.manav.mindguardai.data.FederatedLearningEngine
import com.manav.mindguardai.data.FederatedState
import com.manav.mindguardai.data.TextAnalysisResult
import com.manav.mindguardai.data.TextAnalyzer
import com.manav.mindguardai.data.WellbeingSnapshot
import com.manav.mindguardai.data.WellbeingStore
import com.manav.mindguardai.ui.components.AmbientBackground
import com.manav.mindguardai.ui.screens.AnalyzeScreen
import com.manav.mindguardai.ui.screens.CheckInScreen
import com.manav.mindguardai.ui.screens.FamilyScreen
import com.manav.mindguardai.ui.screens.FederatedLearningScreen
import com.manav.mindguardai.ui.screens.HomeScreen
import com.manav.mindguardai.ui.theme.Palette

private enum class AppTab(val title: String, val symbol: String) {
    HOME("Home", "⌂"),
    CHECK_IN("Check-in", "♡"),
    ANALYZE("Analyze", "✦"),
    FAMILY("Family", "↗"),
    FL_LAB("FL Lab", "◎")
}

@Composable
fun MindGuardApp() {
    val context = LocalContext.current
    var selectedTabName by rememberSaveable { mutableStateOf(AppTab.HOME.name) }
    val selectedTab = AppTab.values().firstOrNull { it.name == selectedTabName } ?: AppTab.HOME

    var snapshot by remember { mutableStateOf<WellbeingSnapshot>(WellbeingStore.load(context)) }
    var mood by rememberSaveable { mutableStateOf(3) }
    var stress by rememberSaveable { mutableStateOf(5f) }
    var sleep by rememberSaveable { mutableStateOf(7f) }
    var energy by rememberSaveable { mutableStateOf(6f) }
    var journalText by rememberSaveable { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf<TextAnalysisResult?>(null) }
    var familySharing by remember { mutableStateOf(WellbeingStore.isFamilyOptedIn(context)) }
    var familyPreview by rememberSaveable { mutableStateOf(false) }
    var federatedState by remember { mutableStateOf(FederatedState()) }

    val latestSummary = snapshot.latest?.summary() ?: "No check-in saved yet"
    val shareMessage = "Hi, I've been checking in with myself and could use a little support. Would you have time to talk with me today?\n\nMy latest reflection: $latestSummary"

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            MindGuardBottomNavigation(
                selected = selectedTab,
                onSelect = { selectedTabName = it.name }
            )
        }
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            AmbientBackground(Modifier.matchParentSize())
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(durationMillis = 260),
                label = "mindguard-tab-transition"
            ) { tab ->
                when (tab) {
                    AppTab.HOME -> HomeScreen(
                        snapshot = snapshot,
                        onCheckIn = { selectedTabName = AppTab.CHECK_IN.name },
                        onAnalyze = { selectedTabName = AppTab.ANALYZE.name },
                        onFamily = { selectedTabName = AppTab.FAMILY.name },
                        onLearningLab = { selectedTabName = AppTab.FL_LAB.name }
                    )
                    AppTab.CHECK_IN -> CheckInScreen(
                        mood = mood,
                        stress = stress,
                        sleep = sleep,
                        energy = energy,
                        onMoodChange = { mood = it },
                        onStressChange = { stress = it },
                        onSleepChange = { sleep = it },
                        onEnergyChange = { energy = it },
                        savedMessage = snapshot.latest?.summary() ?: "Your reflection is saved on this device.",
                        onSave = { selectedMood, selectedStress, selectedSleep, selectedEnergy ->
                            snapshot = WellbeingStore.saveCheckIn(
                                context,
                                CheckInEntry(
                                    timestamp = System.currentTimeMillis(),
                                    mood = selectedMood.coerceIn(1, 5),
                                    stress = selectedStress.coerceIn(1, 10),
                                    sleepHours = selectedSleep.coerceIn(0, 12),
                                    energy = selectedEnergy.coerceIn(1, 10)
                                )
                            )
                        }
                    )
                    AppTab.ANALYZE -> AnalyzeScreen(
                        text = journalText,
                        onTextChange = {
                            journalText = it
                            analysisResult = null
                        },
                        result = analysisResult,
                        onAnalyze = { analysisResult = TextAnalyzer.analyze(journalText) }
                    )
                    AppTab.FAMILY -> FamilyScreen(
                        sharing = familySharing,
                        onSharingChange = {
                            familySharing = it
                            WellbeingStore.setFamilyOptIn(context, it)
                            familyPreview = false
                        },
                        latestSummary = latestSummary,
                        showPreview = familyPreview,
                        onPreview = { familyPreview = true },
                        onShareUsingPhone = {
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareMessage)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share a supportive message"))
                        }
                    )
                    AppTab.FL_LAB -> FederatedLearningScreen(
                        state = federatedState,
                        onRunRound = { federatedState = FederatedLearningEngine.runRound(federatedState) },
                        onReset = { federatedState = FederatedLearningEngine.reset() }
                    )
                }
            }
        }
    }
}

@Composable
private fun MindGuardBottomNavigation(selected: AppTab, onSelect: (AppTab) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 7.dp, bottom = 5.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(27.dp))
                .background(Palette.BackgroundRaised.copy(alpha = 0.97f))
                .border(1.dp, Palette.Border, RoundedCornerShape(27.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTab.values().forEach { tab ->
                val isSelected = selected == tab
                val bg by animateColorAsState(
                    targetValue = if (isSelected) Palette.Mint.copy(alpha = 0.14f) else Color.Transparent,
                    animationSpec = tween(180),
                    label = "nav-item-color"
                )
                val foreground by animateColorAsState(
                    targetValue = if (isSelected) Palette.Mint else Palette.Muted,
                    animationSpec = tween(180),
                    label = "nav-item-foreground"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(bg)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 8.dp, horizontal = 1.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(tab.symbol, color = foreground, fontSize = 19.sp, fontWeight = FontWeight.Medium)
                    Text(
                        tab.title,
                        color = foreground,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
