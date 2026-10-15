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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manav.mindguardai.ui.components.PrimaryActionButton
import com.manav.mindguardai.ui.components.ScreenTitle
import com.manav.mindguardai.ui.components.SoftCard
import com.manav.mindguardai.ui.components.SmallPill
import com.manav.mindguardai.ui.theme.Palette

@Composable
fun FamilyScreen(
    sharing: Boolean,
    onSharingChange: (Boolean) -> Unit,
    latestSummary: String,
    showPreview: Boolean,
    onPreview: () -> Unit,
    onShareUsingPhone: () -> Unit
) {
    val message = "Hi, I've been checking in with myself and could use a little support. Would you have time to talk with me today?\n\nMy latest reflection: $latestSummary"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 17.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(17.dp)
    ) {
        ScreenTitle(
            eyebrow = "Support, on your terms",
            title = "You don't have\nto do it alone.",
            subtitle = "Choose whether to prepare a message for someone you trust. Nothing is sent automatically."
        )

        SoftCard {
            SmallPill("YOU ARE IN CONTROL", tint = Palette.Blue)
            Text("Make reaching out easier", color = Palette.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "Turn on the option below only when you want to prepare a supportive message. Your family is not notified by this app.",
                color = Palette.Muted,
                fontSize = 12.sp,
                lineHeight = 19.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("I choose to share a message", color = Palette.Text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (sharing) "Opt-in is on" else "Opt-in is off", color = Palette.Muted, fontSize = 11.sp)
                }
                Switch(
                    checked = sharing,
                    onCheckedChange = onSharingChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Palette.Background,
                        checkedTrackColor = Palette.Mint,
                        uncheckedThumbColor = Palette.Muted,
                        uncheckedTrackColor = Palette.Track
                    )
                )
            }
            PrimaryActionButton(
                text = "Preview supportive message",
                onClick = onPreview,
                enabled = sharing,
                leadingSymbol = "↗"
            )
        }

        AnimatedVisibility(
            visible = showPreview && sharing,
            enter = fadeIn() + slideInVertically { it / 6 }
        ) {
            SoftCard(containerColor = Palette.SurfaceRaised) {
                SmallPill("MESSAGE PREVIEW · NOT SENT")
                Text("A simple way to start", color = Palette.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(message, color = Palette.Text, fontSize = 13.sp, lineHeight = 21.sp)
                Spacer(Modifier.height(2.dp))
                PrimaryActionButton(
                    text = "Open phone share sheet",
                    onClick = onShareUsingPhone,
                    leadingSymbol = "↗"
                )
                Text(
                    "Your phone will ask which app to use. You choose the recipient and press send yourself.",
                    color = Palette.Muted,
                    fontSize = 10.sp,
                    lineHeight = 16.sp
                )
            }
        }

        SoftCard {
            Text("One important promise", color = Palette.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Support should never mean secret monitoring. MindGuard does not read another person's accounts or send alerts in the background.",
                color = Palette.Muted,
                fontSize = 12.sp,
                lineHeight = 19.sp
            )
        }
    }
}
