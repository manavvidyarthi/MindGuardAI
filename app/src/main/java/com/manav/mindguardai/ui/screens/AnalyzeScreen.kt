package com.manav.mindguardai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manav.mindguardai.data.TextAnalysisResult
import com.manav.mindguardai.ui.components.PrimaryActionButton
import com.manav.mindguardai.ui.components.ScreenTitle
import com.manav.mindguardai.ui.components.SignalBar
import com.manav.mindguardai.ui.components.SoftCard
import com.manav.mindguardai.ui.components.SmallPill
import com.manav.mindguardai.ui.theme.Palette

@Composable
fun AnalyzeScreen(
    text: String,
    onTextChange: (String) -> Unit,
    result: TextAnalysisResult?,
    onAnalyze: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 17.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenTitle(
            eyebrow = "Private text reflection",
            title = "Give your thoughts\na little space.",
            subtitle = "Enter text you choose to share with the app. Nothing is uploaded by this demo."
        )

        SoftCard {
            SmallPill("ON-DEVICE · KEYWORD DEMO")
            Text(
                "Your words, your choice",
                color = Palette.Text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "This checks a small list of stress-related words. It does not access Instagram, WhatsApp, or other accounts, and it cannot diagnose a condition.",
                color = Palette.Muted,
                fontSize = 12.sp,
                lineHeight = 19.sp
            )
        }

        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp)
                .clip(RoundedCornerShape(22.dp)),
            label = { Text("A note to yourself") },
            placeholder = { Text("Today I felt… One thing on my mind is…") },
            minLines = 5,
            shape = RoundedCornerShape(22.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Palette.Mint,
                unfocusedBorderColor = Palette.Border,
                focusedLabelColor = Palette.Mint,
                unfocusedLabelColor = Palette.Muted,
                focusedTextColor = Palette.Text,
                unfocusedTextColor = Palette.Text,
                cursorColor = Palette.Mint,
                focusedContainerColor = Palette.Surface,
                unfocusedContainerColor = Palette.Surface
            )
        )
        Text("${text.length} characters", color = Palette.Muted, fontSize = 10.sp, modifier = Modifier.padding(start = 3.dp))

        PrimaryActionButton(
            text = "Reflect on this text",
            onClick = onAnalyze,
            enabled = text.isNotBlank(),
            leadingSymbol = "✦"
        )

        AnimatedVisibility(
            visible = result != null,
            enter = fadeIn() + slideInVertically { it / 6 }
        ) {
            result?.let { analysis ->
                SoftCard(containerColor = Palette.SurfaceRaised) {
                    SmallPill(
                        text = analysis.level.uppercase(),
                        tint = if (analysis.urgentPhraseFound) Palette.Coral else if (analysis.matchedTerms >= 3) Palette.Amber else Palette.Mint
                    )
                    Text(analysis.title, color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 25.sp)
                    Text(analysis.detail, color = Palette.Muted, fontSize = 12.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("Keyword signal · not a mental-health score", color = Palette.Muted, fontSize = 10.sp)
                    SignalBar(
                        progress = analysis.signal,
                        tint = if (analysis.urgentPhraseFound) Palette.Coral else if (analysis.matchedTerms >= 3) Palette.Amber else Palette.Mint
                    )
                    Text("A gentle next step", color = Palette.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(analysis.suggestion, color = Palette.Text, fontSize = 12.sp, lineHeight = 19.sp)
                }
            }
        }

        Text(
            "This tool can miss context and may misread your words. Your own experience matters more than the result.",
            color = Palette.Muted,
            fontSize = 11.sp,
            lineHeight = 17.sp
        )
    }
}
