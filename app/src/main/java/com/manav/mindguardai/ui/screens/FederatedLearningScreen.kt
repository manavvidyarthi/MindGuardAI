package com.manav.mindguardai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manav.mindguardai.data.FederatedState
import com.manav.mindguardai.ui.components.MetricCard
import com.manav.mindguardai.ui.components.PrimaryActionButton
import com.manav.mindguardai.ui.components.ScreenTitle
import com.manav.mindguardai.ui.components.SignalBar
import com.manav.mindguardai.ui.components.SoftCard
import com.manav.mindguardai.ui.components.SmallPill
import com.manav.mindguardai.ui.theme.Palette
import java.util.Locale

@Composable
fun FederatedLearningScreen(
    state: FederatedState,
    onRunRound: () -> Unit,
    onReset: () -> Unit
) {
    val accuracyLabel = state.testAccuracy?.let { "${(it * 100).toInt()}%" } ?: "—"
    val lossLabel = state.testLoss?.let { String.format(Locale.US, "%.3f", it) } ?: "—"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 17.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        ScreenTitle(
            eyebrow = "Behind the model",
            title = "Learning, without\ngathering every note.",
            subtitle = "Explore the maths behind Federated Averaging using a tiny model and synthetic examples."
        )

        SoftCard(containerColor = Palette.SurfaceRaised) {
            SmallPill("EDUCATIONAL ML LAB", tint = Palette.Lavender)
            Text("Local updates → FedAvg", color = Palette.Text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(
                "Each simulated client trains a small logistic-regression model on its own synthetic examples. The app then averages the two parameter updates into one global model.",
                color = Palette.Muted,
                fontSize = 12.sp,
                lineHeight = 19.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("TRAINING ROUND", state.rounds.toString(), "completed", accent = Palette.Mint, modifier = Modifier.weight(1f))
            MetricCard("TOY TEST ACCURACY", accuracyLabel, "synthetic validation set", accent = Palette.Lavender, modifier = Modifier.weight(1f))
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Local client updates", color = Palette.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ClientCard(
                    title = "Client A",
                    subtitle = "8 synthetic examples",
                    parameters = state.clientAWeights,
                    tint = Palette.Mint,
                    modifier = Modifier.weight(1f)
                )
                ClientCard(
                    title = "Client B",
                    subtitle = "8 synthetic examples",
                    parameters = state.clientBWeights,
                    tint = Palette.Lavender,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Global model", color = Palette.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Validation loss · $lossLabel", color = Palette.Muted, fontSize = 11.sp)
                }
                SmallPill(if (state.rounds == 0) "NOT TRAINED" else "ROUND ${state.rounds}")
            }
            if (state.testAccuracy != null) {
                SignalBar(progress = state.testAccuracy.toFloat(), tint = Palette.Lavender)
                Text("${(state.testAccuracy * 100).toInt()}% correct on the tiny synthetic validation set", color = Palette.Muted, fontSize = 11.sp)
            } else {
                Text("Run a round to see model evaluation on held-out synthetic examples.", color = Palette.Muted, fontSize = 12.sp, lineHeight = 18.sp)
            }
            Text("Global weights", color = Palette.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(
                state.globalWeights.mapIndexed { index, weight -> "w$index ${String.format(Locale.US, "%.2f", weight)}" }.joinToString("   ·   "),
                color = Palette.Mint,
                fontSize = 11.sp,
                lineHeight = 18.sp
            )
        }

        PrimaryActionButton("Train local models + run FedAvg", onClick = onRunRound, leadingSymbol = "◎")
        TextButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
            Text("Reset experiment", color = Palette.Muted, fontWeight = FontWeight.SemiBold)
        }

        AnimatedVisibility(state.rounds > 0, enter = fadeIn() + slideInVertically { it / 6 }) {
            SoftCard {
                Text("What just happened?", color = Palette.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Two separate local training passes ran in this app. Their parameter vectors were combined using a sample-weighted average. Only this simulation's synthetic examples were used.",
                    color = Palette.Muted,
                    fontSize = 12.sp,
                    lineHeight = 19.sp
                )
            }
        }

        SoftCard {
            Text("Honest project note", color = Palette.Amber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                "This is real toy logistic-regression training and parameter averaging, but both clients run in one app and use hard-coded synthetic data. It is not production federated learning, has no secure aggregation or differential privacy, and does not measure mental-health accuracy.",
                color = Palette.Muted,
                fontSize = 11.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ClientCard(
    title: String,
    subtitle: String,
    parameters: List<Double>?,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    SoftCard(modifier = modifier, contentPadding = 14.dp) {
        Text(title, color = tint, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Palette.Muted, fontSize = 10.sp)
        Spacer(Modifier.height(2.dp))
        if (parameters == null) {
            Text("Waiting for first round", color = Palette.Muted, fontSize = 10.sp, lineHeight = 15.sp)
        } else {
            Text("Local weights", color = Palette.Text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            parameters.forEachIndexed { index, value ->
                Text("w$index  ${String.format(Locale.US, "%.2f", value)}", color = Palette.Muted, fontSize = 10.sp)
            }
        }
    }
}
