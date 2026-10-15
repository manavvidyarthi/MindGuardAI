package com.manav.mindguardai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

internal object MG {
    val bg = Color(0xFF080D18)
    val bgRaised = Color(0xFF0D1727)
    val surface = Color(0xFF121E31)
    val surfaceRaised = Color(0xFF192840)
    val surfaceSoft = Color(0xFF24334D)
    val mint = Color(0xFF68E6C3)
    val lavender = Color(0xFFA8A2FF)
    val blue = Color(0xFF8DB9FF)
    val coral = Color(0xFFFF9C91)
    val amber = Color(0xFFFFD18A)
    val text = Color(0xFFF3F6FC)
    val muted = Color(0xFF9AAAC1)
    val track = Color(0xFF293852)
    val border = Color(0x23FFFFFF)
}

private enum class Tab(val title: String) {
    HOME("Home"),
    CHECKIN("Check-in"),
    FAMILY("Family"),
    FL("FL Lab")
}

private data class CheckIn(
    val time: Long,
    val mood: Int,
    val stress: Int,
    val sleep: Int,
    val energy: Int
) {
    fun summary(): String =
        "Mood $mood/5 | Stress $stress/10 | Sleep ${sleep}h | Energy $energy/10"

    fun day(): String =
        SimpleDateFormat("EE", Locale.getDefault()).format(Date(time))

    fun date(): String =
        SimpleDateFormat("d MMM | h:mm a", Locale.getDefault()).format(Date(time))
}

// ---------- Apple-style motion helpers ----------

/** iOS-like springy press: shrinks slightly while pressed, bounces back, light haptic. */
private fun Modifier.bouncyClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press-scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled
        ) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
}

/** Staggered spring entrance: each item floats up and fades in a little after the previous one. */
private fun Modifier.staggerIn(index: Int): Modifier = composed {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(60L + index * 55L)
        visible = true
    }

    val p by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessLow
        ),
        label = "stagger-in"
    )

    graphicsLayer {
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * 48f
        val s = 0.97f + 0.03f * p
        scaleX = s
        scaleY = s
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = MG.mint,
                    onPrimary = MG.bg,
                    secondary = MG.lavender,
                    background = MG.bg,
                    surface = MG.surface,
                    onSurface = MG.text,
                    onBackground = MG.text
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MG.bg
                ) {
                    MindGuardApp()
                }
            }
        }
    }
}

@Composable
private fun MindGuardApp() {
    val context = LocalContext.current

    var tabName by rememberSaveable {
        mutableStateOf(Tab.HOME.name)
    }

    val tab =
        Tab.values().firstOrNull { it.name == tabName } ?: Tab.HOME

    var entries by remember {
        mutableStateOf(loadEntries(context))
    }

    var checkInCount by remember {
        mutableStateOf(loadCount(context, entries.size))
    }

    var mood by rememberSaveable { mutableStateOf(3) }
    var stress by rememberSaveable { mutableStateOf(5f) }
    var sleep by rememberSaveable { mutableStateOf(7f) }
    var energy by rememberSaveable { mutableStateOf(6f) }

    // Federated stress model (persisted on device)
    var model by remember {
        mutableStateOf(ModelStore.load(context))
    }

    // Smartwatch data through Health Connect (Samsung Health, Pixel Watch, Fitbit...)
    val watch = remember { HealthWatch(context) }
    val scope = rememberCoroutineScope()
    var watchState by remember {
        mutableStateOf(WatchUiState(watch.availability()))
    }

    fun syncWatch() {
        val availability = watch.availability()
        watchState = watchState.copy(availability = availability)
        if (availability != WatchAvailability.AVAILABLE) return

        scope.launch {
            watchState = watchState.copy(loading = true, error = null)
            watchState = try {
                if (watch.grantedCount() == 0) {
                    watchState.copy(connected = false, loading = false)
                } else {
                    watchState.copy(connected = true, loading = false, snapshot = watch.read())
                }
            } catch (e: Exception) {
                watchState.copy(loading = false, error = "Couldn't read Health Connect. Try again.")
            }
        }
    }

    val watchPermissionLauncher = rememberLauncherForActivityResult(
        HealthWatch.permissionContract()
    ) { syncWatch() }

    LaunchedEffect(Unit) { syncWatch() }

    // While the watch is connected, re-read it every 10 minutes automatically
    LaunchedEffect(watchState.connected) {
        while (watchState.connected) {
            delay(10 * 60_000L)
            syncWatch()
        }
    }

    // Watch-only federated stress model (pre-trained on first launch)
    var watchModel by remember { mutableStateOf(WatchStressStore.loadOrPretrain(context)) }
    val watchEstimate = remember(watchState.snapshot, watchModel) {
        watchState.snapshot?.let { WatchStress.estimate(watchModel, it) }
    }

    // Google account (Firebase Auth) + family circle
    val family = remember { FamilyCircle() }
    var account by remember { mutableStateOf(runCatching { family.account() }.getOrNull()) }
    var skippedLogin by rememberSaveable { mutableStateOf(false) }
    var loginBusy by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var familyState by remember { mutableStateOf(FamilyState()) }

    fun signIn() {
        if (loginBusy) return
        scope.launch {
            loginBusy = true
            loginError = null
            try {
                account = family.signInWithGoogle(context)
                skippedLogin = false
            } catch (e: Exception) {
                loginError = friendlySignInError(e)
            } finally {
                loginBusy = false
            }
        }
    }

    var showSettings by rememberSaveable { mutableStateOf(false) }

    fun signOut() {
        scope.launch {
            runCatching { family.signOut(context) }
            account = null
            skippedLogin = false
            showSettings = false
        }
    }

    LaunchedEffect(account?.uid) {
        val acc = account
        familyState = if (acc == null) {
            FamilyState(ready = true)
        } else {
            try {
                family.restore(acc)
            } catch (e: Exception) {
                FamilyState(ready = true, error = e.message)
            }
        }
    }

    LaunchedEffect(familyState.circleId) {
        val cid = familyState.circleId ?: return@LaunchedEffect
        try {
            family.members(cid)
                .catch { e ->
                    if ((e.message ?: "").contains("PERMISSION_DENIED")) {
                        // someone removed me from this circle
                        runCatching { family.clearMyCircle() }
                        familyState = familyState.copy(
                            circleId = null,
                            members = emptyList(),
                            error = "You were removed from the family circle."
                        )
                    } else {
                        familyState = familyState.copy(error = e.message)
                    }
                }
                .collect { familyState = familyState.copy(members = it) }
        } catch (e: Exception) {
            familyState = familyState.copy(error = e.message)
        }
    }

    // Every new watch reading (or a change in what I share) goes to my family
    LaunchedEffect(familyState.circleId, watchState.snapshot, watchEstimate, familyState.share) {
        if (familyState.circleId == null || account == null) return@LaunchedEffect
        runCatching { family.pushLive(familyState.share, watchEstimate?.stress, watchState.snapshot) }
    }

    fun familyAction(work: suspend (FamilyState, Account) -> FamilyState) {
        val acc = account
        if (acc == null) {
            familyState = familyState.copy(error = "Sign in with Google to use Family.")
            return
        }
        if (familyState.busy) return
        scope.launch {
            familyState = familyState.copy(busy = true, error = null)
            familyState = try {
                work(familyState, acc).copy(busy = false)
            } catch (e: Exception) {
                familyState.copy(busy = false, error = e.message ?: "Something went wrong")
            }
        }
    }

    fun shareText(text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    // Login screen first (can be skipped; only Family needs an account)
    if (account == null && !skippedLogin) {
        LoginScreen(
            busy = loginBusy,
            error = loginError,
            onGoogle = { signIn() },
            onSkip = { skippedLogin = true }
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        // Background sits behind everything (edge-to-edge, under status bar too)
        BackgroundArtwork(Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                BottomNavigation(selected = tab) {
                    tabName = it.name
                }
            }
        ) { inset ->
            AnimatedContent(
                targetState = tab,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inset),
                transitionSpec = {
                    val dir =
                        if (targetState.ordinal > initialState.ordinal) 1 else -1

                    (slideInHorizontally(
                        spring(
                            dampingRatio = 0.86f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) { full -> dir * full / 4 } +
                            fadeIn(tween(220, delayMillis = 50)))
                        .togetherWith(
                            slideOutHorizontally(
                                spring(
                                    dampingRatio = 0.86f,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) { full -> -dir * full / 6 } +
                                    fadeOut(tween(150))
                        )
                        .using(SizeTransform(clip = false))
                },
                label = "tab-transition"
            ) { screen ->
                when (screen) {
                    Tab.HOME -> HomeScreen(
                        entries = entries,
                        totalCount = checkInCount,
                        model = model,
                        watch = watchState,
                        watchEstimate = watchEstimate,
                        onCheckIn = {
                            tabName = Tab.CHECKIN.name
                        },
                        onFL = {
                            tabName = Tab.FL.name
                        },
                        onSettings = { showSettings = true },
                        onWatchConnect = {
                            watchPermissionLauncher.launch(HealthWatch.PERMISSIONS)
                        },
                        onWatchInstall = {
                            runCatching { context.startActivity(watch.installIntent()) }
                        },
                        onWatchSync = { syncWatch() }
                    )

                    Tab.CHECKIN -> CheckInScreen(
                        mood = mood,
                        stress = stress,
                        sleep = sleep,
                        energy = energy,
                        onMood = { mood = it },
                        onStress = { stress = it },
                        onSleep = { sleep = it },
                        onEnergy = { energy = it },
                        model = model,
                        watchSleep = watchState.snapshot?.sleepHours,
                        onSave = {
                                newMood, newStress,
                                newSleep, newEnergy ->

                            val entry = CheckIn(
                                time = System.currentTimeMillis(),
                                mood = newMood,
                                stress = newStress,
                                sleep = newSleep,
                                energy = newEnergy
                            )

                            entries = (entries + entry).takeLast(30)
                            checkInCount += 1

                            saveEntries(
                                context,
                                entries,
                                checkInCount
                            )

                            // Your phone joins 3 federated rounds with the new check-in
                            model = FederatedEngine.train(
                                model,
                                entriesToSamples(entries),
                                rounds = 3
                            )
                            ModelStore.save(context, model)

                            // Wearing the watch? Its signals + your rating teach the watch model
                            watchState.snapshot
                                ?.let { WatchFeatures.of(it) }
                                ?.let { (x, _) ->
                                    WatchStressStore.addSample(context, x, StressFeatures.label(newStress))
                                    watchModel = WatchStress.train(
                                        watchModel,
                                        WatchStressStore.samples(context),
                                        rounds = 3
                                    )
                                    WatchStressStore.save(context, watchModel)
                                }
                        }
                    )

                    Tab.FAMILY -> ShareScreen(
                        entries = entries,
                        model = model,
                        watch = watchState,
                        watchEstimate = watchEstimate,
                        family = familyState,
                        accountEmail = account?.email,
                        onSignIn = { signIn() },
                        onSignOut = { signOut() },
                        onInvite = { email ->
                            familyAction { st, acc ->
                                val (cid, sent) = family.inviteMember(acc, email)
                                st.copy(circleId = cid, sent = sent)
                            }
                        },
                        onAccept = { otp ->
                            familyAction { st, acc ->
                                st.copy(circleId = family.acceptInvite(acc, otp), pending = null)
                            }
                        },
                        onShareOtp = { sent ->
                            shareText(
                                "Join my MindGuard family 💚\nSign in to MindGuard with ${sent.email}, " +
                                        "open Family → + and enter OTP: ${sent.otp}\n(valid for 24 hours)",
                                "Send OTP"
                            )
                        },
                        onShareSettings = { newShare ->
                            familyState = familyState.copy(share = newShare)
                            scope.launch { runCatching { family.saveShare(newShare) } }
                        },
                        onLeave = {
                            familyAction { st, _ ->
                                st.circleId?.let { family.leave(it) }
                                st.copy(circleId = null, members = emptyList(), sent = null)
                            }
                        },
                        onShare = { report -> shareText(report, "Share my wellbeing report") }
                    )

                    Tab.FL -> FederatedScreen(
                        model = model,
                        userSampleCount = entries.size,
                        onRound = { privacy ->
                            model = FederatedEngine.runRound(
                                model,
                                entriesToSamples(entries),
                                privacy
                            )
                            ModelStore.save(context, model)
                        },
                        onReset = {
                            model = FedModel()
                            ModelStore.clear(context)
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showSettings,
            enter = slideInHorizontally(
                spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)
            ) { it } + fadeIn(tween(200)),
            exit = slideOutHorizontally(tween(220)) { it } + fadeOut(tween(180))
        ) {
            SettingsScreen(
                account = account,
                family = familyState,
                onBack = { showSettings = false },
                onSignIn = { signIn() },
                onSignOut = { signOut() },
                onShareSettings = { newShare ->
                    familyState = familyState.copy(share = newShare)
                    scope.launch { runCatching { family.saveShare(newShare) } }
                },
                onLeave = {
                    familyAction { st, _ ->
                        st.circleId?.let { family.leave(it) }
                        st.copy(circleId = null, members = emptyList(), sent = null)
                    }
                },
                onRemove = { member ->
                    familyAction { st, _ ->
                        st.circleId?.let { family.removeMember(it, member.uid) }
                        st.copy(members = st.members.filter { it.uid != member.uid })
                    }
                }
            )
        }
    }
}

@Composable
private fun BackgroundArtwork(
    modifier: Modifier = Modifier
) {
    // Slow ambient drift of the glow orbs (Apple-style living background)
    val drift = rememberInfiniteTransition(label = "bg-drift")

    val t by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg-t"
    )

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(
                    MG.bg,
                    Color(0xFF0B1525),
                    MG.bg
                )
            )
        )
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 6.dp, end = 4.dp)
                .size(240.dp)
                .graphicsLayer {
                    translationX = -60f * t
                    translationY = 40f * t
                }
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0x253FE0B5),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 42.dp)
                .size(195.dp)
                .graphicsLayer {
                    translationX = 70f * t
                    translationY = -50f * t
                }
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0x209D96FF),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
    }
}

@Composable
private fun BrandMark(size: Dp = 48.dp) {
    val transition = rememberInfiniteTransition(
        label = "brand-pulse"
    )

    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brand-scale"
    )

    Box(
        Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(size * pulse)
                .background(
                    Color(0x1D68E6C3),
                    CircleShape
                )
        )

        Box(
            Modifier
                .size(size * 0.82f)
                .background(
                    Brush.linearGradient(
                        listOf(MG.mint, MG.blue)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "m",
                color = MG.bg,
                fontSize = (size.value * 0.48f).sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

/** SF-Symbols-style line icons drawn with Canvas (no extra icon dependency needed). */
@Composable
private fun TabIcon(
    tab: Tab,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(
            width = w * 0.09f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        when (tab) {
            Tab.HOME -> {
                val p = Path().apply {
                    moveTo(w * 0.12f, h * 0.48f)
                    lineTo(w * 0.5f, h * 0.15f)
                    lineTo(w * 0.88f, h * 0.48f)
                    moveTo(w * 0.24f, h * 0.40f)
                    lineTo(w * 0.24f, h * 0.85f)
                    lineTo(w * 0.76f, h * 0.85f)
                    lineTo(w * 0.76f, h * 0.40f)
                }
                drawPath(p, tint, style = stroke)
            }

            Tab.CHECKIN -> {
                drawCircle(tint, radius = w * 0.38f, style = stroke)
                val p = Path().apply {
                    moveTo(w * 0.32f, h * 0.52f)
                    lineTo(w * 0.45f, h * 0.65f)
                    lineTo(w * 0.69f, h * 0.38f)
                }
                drawPath(p, tint, style = stroke)
            }

            Tab.FAMILY -> {
                drawCircle(tint, radius = w * 0.13f, center = Offset(w * 0.36f, h * 0.32f), style = stroke)
                drawCircle(tint, radius = w * 0.10f, center = Offset(w * 0.71f, h * 0.36f), style = stroke)
                val p = Path().apply {
                    moveTo(w * 0.12f, h * 0.84f)
                    cubicTo(w * 0.12f, h * 0.56f, w * 0.60f, h * 0.56f, w * 0.60f, h * 0.84f)
                    moveTo(w * 0.64f, h * 0.58f)
                    cubicTo(w * 0.80f, h * 0.54f, w * 0.90f, h * 0.66f, w * 0.90f, h * 0.80f)
                }
                drawPath(p, tint, style = stroke)
            }

            Tab.FL -> {
                val a = Offset(w * 0.5f, h * 0.2f)
                val b = Offset(w * 0.2f, h * 0.78f)
                val c = Offset(w * 0.8f, h * 0.78f)
                listOf(a to b, b to c, c to a).forEach { (s, e) ->
                    drawLine(
                        tint, s, e,
                        strokeWidth = stroke.width * 0.8f,
                        cap = StrokeCap.Round
                    )
                }
                listOf(a, b, c).forEach {
                    drawCircle(tint, radius = w * 0.13f, center = it)
                }
            }
        }
    }
}

private val NavItemHeight = 54.dp

@Composable
private fun BottomNavigation(
    selected: Tab,
    onSelect: (Tab) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val tabs = Tab.values()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = 12.dp,
                end = 12.dp,
                top = 6.dp,
                bottom = 7.dp
            )
            .clip(RoundedCornerShape(26.dp))
            .background(Color(0xF20D1727))
            .border(
                1.dp,
                MG.border,
                RoundedCornerShape(26.dp)
            )
            .padding(5.dp)
    ) {
        val itemWidth = maxWidth / tabs.size

        // Sliding capsule that springs between tabs (iOS segmented-control feel)
        val indicatorX by animateDpAsState(
            targetValue = itemWidth * selected.ordinal,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "nav-indicator"
        )

        Box(
            Modifier
                .offset(x = indicatorX)
                .width(itemWidth)
                .height(NavItemHeight)
                .clip(RoundedCornerShape(19.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x3368E6C3), Color(0x1268E6C3))
                    )
                )
                .border(
                    1.dp,
                    MG.mint.copy(alpha = 0.22f),
                    RoundedCornerShape(19.dp)
                )
        )

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { item ->
                val active = selected == item

                val iconScale by animateFloatAsState(
                    targetValue = if (active) 1.12f else 0.94f,
                    animationSpec = spring(
                        dampingRatio = 0.45f,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "nav-icon-${item.name}"
                )

                val tint by animateColorAsState(
                    targetValue = if (active) MG.mint else MG.muted,
                    animationSpec = tween(220),
                    label = "nav-tint-${item.name}"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(NavItemHeight)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!active) {
                                haptic.performHapticFeedback(
                                    HapticFeedbackType.TextHandleMove
                                )
                            }
                            onSelect(item)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    TabIcon(
                        item,
                        tint,
                        Modifier.graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        item.title,
                        color = tint,
                        fontSize = 9.sp,
                        fontWeight = if (active) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Medium
                        },
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenTitle(
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            eyebrow.uppercase(),
            color = MG.mint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        Text(
            title,
            color = MG.text,
            fontSize = 30.sp,
            lineHeight = 35.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.7).sp
        )

        Text(
            subtitle,
            color = MG.muted,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun SectionHeading(
    title: String,
    subtitle: String? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            title,
            color = MG.text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )

        if (subtitle != null) {
            Text(
                subtitle,
                color = MG.muted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun SmallPill(
    text: String,
    tint: Color = MG.mint
) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.10f))
            .border(
                1.dp,
                tint.copy(alpha = 0.20f),
                CircleShape
            )
            .padding(
                horizontal = 10.dp,
                vertical = 6.dp
            )
    ) {
        Text(
            text,
            color = tint,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.45.sp
        )
    }
}

@Composable
private fun SoftCard(
    modifier: Modifier = Modifier,
    color: Color = MG.surface,
    padding: Dp = 17.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(23.dp))
            .background(color)
            .border(
                1.dp,
                MG.border,
                RoundedCornerShape(23.dp)
            )
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

@Composable
private fun MainButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    symbol: String? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button-press"
    )

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        enabled = enabled,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .height(53.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MG.mint,
            contentColor = MG.bg,
            disabledContainerColor = MG.surfaceSoft,
            disabledContentColor = MG.muted
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        if (symbol != null) {
            Text(
                symbol,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
        }

        Text(
            text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    caption: String,
    tint: Color = MG.mint,
    modifier: Modifier = Modifier
) {
    SoftCard(
        modifier = modifier,
        padding = 14.dp
    ) {
        Text(
            title,
            color = MG.muted,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            value,
            color = tint,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.4).sp
        )

        Text(
            caption,
            color = MG.muted,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun HomeScreen(
    entries: List<CheckIn>,
    totalCount: Int,
    model: FedModel,
    watch: WatchUiState,
    watchEstimate: WatchEstimate?,
    onCheckIn: () -> Unit,
    onFL: () -> Unit,
    onSettings: () -> Unit,
    onWatchConnect: () -> Unit,
    onWatchInstall: () -> Unit,
    onWatchSync: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 19.dp,
                end = 19.dp,
                top = 15.dp,
                bottom = 23.dp
            ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            Modifier.staggerIn(0),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            BrandMark()

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    "MindGuard AI",
                    color = MG.text,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                )

                Text(
                    "Your personal wellbeing space",
                    color = MG.muted,
                    fontSize = 11.sp
                )
            }

            RoundIconButton(icon = "settings", tint = MG.muted, onClick = onSettings)
        }

        // Watch on? Show the automatic reading first
        if (watch.connected && watch.snapshot != null && watchEstimate != null) {
            WatchStressReveal(
                estimate = watchEstimate,
                snap = watch.snapshot,
                modifier = Modifier.staggerIn(1)
            )
        }

        Box(Modifier.staggerIn(1)) {
            HeroCard(onCheckIn)
        }

        StressInsightCard(
            model = model,
            latest = entries.lastOrNull(),
            bodyIndex = watchEstimate?.stress,
            onTrain = onFL,
            modifier = Modifier.staggerIn(2)
        )

        WatchCard(
            state = watch,
            estimate = watchEstimate,
            onConnect = onWatchConnect,
            onInstall = onWatchInstall,
            onSync = onWatchSync,
            modifier = Modifier.staggerIn(3)
        )

        Row(
            Modifier.staggerIn(4),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            MetricCard(
                "CHECK-INS",
                totalCount.toString(),
                "reflections saved",
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                "LATEST MOOD",
                entries.lastOrNull()?.let { "${it.mood}/5" } ?: "--",
                entries.lastOrNull()?.date() ?: "start with a check-in",
                MG.lavender,
                Modifier.weight(1f)
            )
        }

        Column(
            Modifier.staggerIn(5),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeading(
                "Your recent rhythm",
                "Last 7 saved check-ins"
            )

            SoftCard(padding = 16.dp) {
                if (entries.isEmpty()) {
                    Text(
                        "Your mood history will appear here after your first check-in. " +
                                "Sample values are not shown as your own data.",
                        color = MG.muted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                } else {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        entries.takeLast(7).forEachIndexed { index, entry ->
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "${entry.mood}",
                                    color = MG.text,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                MoodBar(
                                    entry.mood / 5f,
                                    when {
                                        entry.mood <= 2 -> MG.coral
                                        entry.mood == 3 -> MG.lavender
                                        else -> MG.mint
                                    },
                                    delayMs = 250L + index * 70L
                                )

                                Text(
                                    entry.day(),
                                    color = MG.muted,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                }
            }
        }

        Text(
            "Student project, not a medical device. Estimates are not a diagnosis. " +
                    "Your check-ins and watch data stay on this phone.",
            color = MG.muted,
            fontSize = 10.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.staggerIn(6)
        )
    }
}

@Composable
private fun HeroCard(onCheckIn: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(29.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF1C3949),
                        Color(0xFF202E4B),
                        Color(0xFF302D50)
                    )
                )
            )
            .border(
                1.dp,
                Color.White.copy(alpha = 0.10f),
                RoundedCornerShape(29.dp)
            )
    ) {
        BreathingOrb(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 24.dp, y = (-18).dp),
            diameter = 170.dp
        )

        Column(
            Modifier.padding(21.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallPill("YOUR SPACE, YOUR PACE")

            Text(
                "Make space\nfor how you feel.",
                color = MG.text,
                fontSize = 30.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp
            )

            Text(
                "Small check-ins. Gentle insights. No judgement.",
                color = Color(0xFFD1DBE9),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )

            MainButton(
                "Start today's check-in",
                onCheckIn,
                symbol = "+"
            )
        }
    }
}

@Composable
private fun MoodBar(
    value: Float,
    tint: Color,
    delayMs: Long = 0L
) {
    // Bars grow from zero with a soft spring when the screen appears
    var shown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delayMs)
        shown = true
    }

    val animated by animateFloatAsState(
        targetValue = if (shown) value.coerceIn(0.12f, 1f) else 0f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessLow
        ),
        label = "mood-bar"
    )

    Box(
        Modifier
            .width(19.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MG.track),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(animated.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(tint, tint.copy(alpha = 0.53f))
                    )
                )
        )
    }
}

private data class MoodOption(
    val value: Int,
    val emoji: String,
    val label: String
)

private val moodOptions = listOf(
    MoodOption(1, "😞", "Low"),
    MoodOption(2, "😕", "Meh"),
    MoodOption(3, "😐", "Okay"),
    MoodOption(4, "🙂", "Good"),
    MoodOption(5, "😄", "Great")
)

@Composable
private fun CheckInScreen(
    mood: Int,
    stress: Float,
    sleep: Float,
    energy: Float,
    onMood: (Int) -> Unit,
    onStress: (Float) -> Unit,
    onSleep: (Float) -> Unit,
    onEnergy: (Float) -> Unit,
    model: FedModel,
    watchSleep: Double?,
    onSave: (Int, Int, Int, Int) -> Unit
) {
    var saved by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 19.dp,
                end = 19.dp,
                top = 16.dp,
                bottom = 23.dp
            ),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        ScreenTitle(
            "A moment for you",
            "How are you,\nreally?",
            "There is no perfect answer. Choose what feels closest right now.",
            modifier = Modifier.staggerIn(0)
        )

        SoftCard(modifier = Modifier.staggerIn(1)) {
            Text(
                "Your mood",
                color = MG.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                "Pick the face that matches this moment.",
                color = MG.muted,
                fontSize = 11.sp
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                moodOptions.forEach { option ->
                    val selected = option.value == mood

                    val emojiScale by animateFloatAsState(
                        targetValue = if (selected) 1.22f else 1f,
                        animationSpec = spring(
                            dampingRatio = 0.42f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "emoji-${option.value}"
                    )

                    val bg by animateColorAsState(
                        if (selected) Color(0x2268E6C3) else MG.bgRaised,
                        tween(200),
                        label = "mood-bg-${option.value}"
                    )

                    val outline by animateColorAsState(
                        if (selected) MG.mint.copy(alpha = 0.7f) else MG.border,
                        tween(200),
                        label = "mood-border-${option.value}"
                    )

                    Column(
                        Modifier
                            .weight(1f)
                            .bouncyClick(pressedScale = 0.9f) {
                                onMood(option.value)
                                saved = false
                            }
                            .clip(RoundedCornerShape(15.dp))
                            .background(bg)
                            .border(
                                1.dp,
                                outline,
                                RoundedCornerShape(15.dp)
                            )
                            .padding(
                                vertical = 11.dp,
                                horizontal = 1.dp
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            option.emoji,
                            fontSize = 22.sp,
                            modifier = Modifier.graphicsLayer {
                                scaleX = emojiScale
                                scaleY = emojiScale
                            }
                        )

                        Text(
                            option.label,
                            color = if (selected) MG.mint else MG.muted,
                            fontSize = 9.sp,
                            fontWeight = if (selected) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Medium
                            }
                        )
                    }
                }
            }
        }

        SliderPanel(
            "Stress level", stress, "/10",
            "Calm", "Very high",
            onStress, 1f..10f, 8,
            modifier = Modifier.staggerIn(2)
        ) { saved = false }

        SliderPanel(
            "Energy", energy, "/10",
            "Drained", "Energised",
            onEnergy, 1f..10f, 8,
            modifier = Modifier.staggerIn(3)
        ) { saved = false }

        if (watchSleep != null) {
            GhostButton(
                "Use watch sleep: ${String.format(Locale.US, "%.1f", watchSleep)} h",
                {
                    onSleep(watchSleep.roundToInt().coerceIn(0, 12).toFloat())
                    saved = false
                }
            )
        }

        SliderPanel(
            "Sleep last night", sleep, " h",
            "0 hours", "12 hours",
            onSleep, 0f..12f, 11,
            modifier = Modifier.staggerIn(4)
        ) { saved = false }

        LiveEstimateCard(
            model = model,
            mood = mood,
            sleep = sleep.toInt(),
            energy = energy.toInt(),
            reportedStress = stress.toInt(),
            modifier = Modifier.staggerIn(5)
        )

        MainButton(
            "Save my reflection",
            {
                onSave(
                    mood,
                    stress.toInt(),
                    sleep.toInt(),
                    energy.toInt()
                )
                saved = true
            },
            symbol = "✓"
        )

        AnimatedVisibility(
            visible = saved,
            enter = fadeIn(tween(220)) +
                    expandVertically(
                        spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) +
                    scaleIn(initialScale = 0.94f),
            exit = fadeOut(tween(150)) + shrinkVertically()
        ) {
            SoftCard(color = MG.surfaceRaised) {
                Text(
                    "Reflection saved ✓",
                    color = MG.mint,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Your phone joined 3 federated rounds with this entry. " +
                            "Stress model is now at round ${model.rounds}.",
                    color = MG.muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

    }
}

@Composable
private fun SliderPanel(
    title: String,
    value: Float,
    suffix: String,
    left: String,
    right: String,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
    afterChange: () -> Unit
) {
    SoftCard(modifier = modifier, padding = 15.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                color = MG.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            Text(
                "${value.toInt()}$suffix",
                color = MG.mint,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = value,
            onValueChange = {
                onChange(it)
                afterChange()
            },
            valueRange = range,
            steps = steps
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(left, color = MG.muted, fontSize = 9.sp)
            Text(right, color = MG.muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun ShareScreen(
    entries: List<CheckIn>,
    model: FedModel,
    watch: WatchUiState,
    watchEstimate: WatchEstimate?,
    family: FamilyState,
    accountEmail: String?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onInvite: (String) -> Unit,
    onAccept: (String) -> Unit,
    onShareOtp: (SentInvite) -> Unit,
    onShareSettings: (ShareSettings) -> Unit,
    onLeave: () -> Unit,
    onShare: (String) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    val hasMyData = entries.isNotEmpty() || watch.snapshot?.hasAnyData == true
    val others = family.members.filter { !it.isMe }
    val me = family.members.firstOrNull { it.isMe }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 19.dp,
                end = 19.dp,
                top = 16.dp,
                bottom = 24.dp
            ),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Row(
            Modifier.staggerIn(0),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "FAMILY",
                    color = MG.mint,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    "Your people",
                    color = MG.text,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.7).sp
                )
            }

            RoundIconButton(icon = "share", tint = MG.lavender, enabled = hasMyData) {
                onShare(buildReport("", entries, model, watch.snapshot, watchEstimate))
            }
            Spacer(Modifier.width(10.dp))
            RoundIconButton(icon = "add", tint = MG.mint) { showAdd = true }
        }

        Text(
            "↗ shares your health report · + adds a family member by Gmail",
            color = MG.muted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.staggerIn(0)
        )

        // someone invited me
        family.pending?.let { inv ->
            Row(
                Modifier
                    .staggerIn(1)
                    .fillMaxWidth()
                    .bouncyClick { showAdd = true }
                    .clip(RoundedCornerShape(20.dp))
                    .background(MG.mint.copy(alpha = 0.12f))
                    .border(1.dp, MG.mint.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LiveDot()
                Column(Modifier.weight(1f)) {
                    Text(
                        "${inv.inviterName} invited you to their family",
                        color = MG.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Tap to enter the OTP and join", color = MG.mint, fontSize = 11.sp)
                }
                Text("›", color = MG.mint, fontSize = 26.sp)
            }
        }

        if (others.isEmpty()) {
            Column(
                Modifier
                    .staggerIn(2)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MG.surface)
                    .border(1.dp, MG.border, RoundedCornerShape(24.dp))
                    .padding(vertical = 26.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BreathingOrb(diameter = 130.dp, tint = MG.blue)
                Text(
                    when {
                        accountEmail == null -> "Sign in to add family"
                        family.circleId == null -> "No family added yet"
                        else -> "Waiting for family to join"
                    },
                    color = MG.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    when {
                        accountEmail == null ->
                            "Family sharing uses your Google account so only people you add can see your data."
                        family.circleId == null ->
                            "Tap + and enter Mummy's or Papa's Gmail. You'll get an OTP to send them."
                        else ->
                            "They sign in with their Gmail and enter the OTP. Their live readings appear here."
                    },
                    color = MG.muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center
                )
                if (accountEmail == null) {
                    GoogleButton(busy = false, onClick = onSignIn)
                }
            }
        } else {
            others.forEachIndexed { i, m -> MemberCard(m, i + 2) }
        }

        if (me != null) {
            Text("What your family sees", color = MG.muted, fontSize = 11.sp)
            MemberCard(me, others.size + 3)
        }

        family.error?.let {
            Text(it, color = MG.coral, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }

    if (showAdd) {
        AddFamilySheet(
            state = family,
            accountEmail = accountEmail,
            onDismiss = { showAdd = false },
            onSignIn = onSignIn,
            onSignOut = {
                showAdd = false
                onSignOut()
            },
            onInvite = onInvite,
            onAccept = onAccept,
            onShareOtp = onShareOtp,
            onShareSettings = onShareSettings,
            onLeave = {
                onLeave()
                showAdd = false
            }
        )
    }
}

/** Small round button with a line icon ("share" or "add"). */
@Composable
private fun RoundIconButton(
    icon: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val c = if (enabled) tint else MG.muted
    Box(
        Modifier
            .size(46.dp)
            .bouncyClick(enabled = enabled, pressedScale = 0.88f, onClick = onClick)
            .clip(CircleShape)
            .background(c.copy(alpha = 0.14f))
            .border(1.dp, c.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(20.dp)) {
            val w = size.width
            val h = size.height
            val st = Stroke(w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            if (icon == "settings") {
                drawCircle(c, radius = w * 0.17f, style = st)
                for (k in 0 until 8) {
                    val a = Math.toRadians(k * 45.0)
                    val cx = w / 2
                    val cy = h / 2
                    drawLine(
                        c,
                        Offset(cx + w * 0.30f * kotlin.math.cos(a).toFloat(), cy + w * 0.30f * kotlin.math.sin(a).toFloat()),
                        Offset(cx + w * 0.44f * kotlin.math.cos(a).toFloat(), cy + w * 0.44f * kotlin.math.sin(a).toFloat()),
                        st.width,
                        StrokeCap.Round
                    )
                }
                drawCircle(c, radius = w * 0.30f, style = Stroke(w * 0.09f))
            } else if (icon == "add") {
                drawLine(c, Offset(w * 0.5f, h * 0.12f), Offset(w * 0.5f, h * 0.88f), st.width, StrokeCap.Round)
                drawLine(c, Offset(w * 0.12f, h * 0.5f), Offset(w * 0.88f, h * 0.5f), st.width, StrokeCap.Round)
            } else {
                val p = Path().apply {
                    moveTo(w * 0.32f, h * 0.42f)
                    lineTo(w * 0.18f, h * 0.42f)
                    lineTo(w * 0.18f, h * 0.88f)
                    lineTo(w * 0.82f, h * 0.88f)
                    lineTo(w * 0.82f, h * 0.42f)
                    lineTo(w * 0.68f, h * 0.42f)
                    moveTo(w * 0.5f, h * 0.62f)
                    lineTo(w * 0.5f, h * 0.08f)
                    moveTo(w * 0.32f, h * 0.25f)
                    lineTo(w * 0.5f, h * 0.08f)
                    lineTo(w * 0.68f, h * 0.25f)
                }
                drawPath(p, c, style = st)
            }
        }
    }
}

/** Bottom sheet behind the + button: add a member by Gmail, accept an invite, sharing settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFamilySheet(
    state: FamilyState,
    accountEmail: String?,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onInvite: (String) -> Unit,
    onAccept: (String) -> Unit,
    onShareOtp: (SentInvite) -> Unit,
    onShareSettings: (ShareSettings) -> Unit,
    onLeave: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MG.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (accountEmail == null) {
                Text("Sign in first", color = MG.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Family uses Google sign-in so only people you add can see your readings.",
                    color = MG.muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                GoogleButton(busy = false, onClick = onSignIn)
            } else {

                // ---- an invite waiting for me ----
                state.pending?.let { inv ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MG.mint.copy(alpha = 0.10f))
                            .border(1.dp, MG.mint.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "${inv.inviterName} invited you",
                            color = MG.text,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Enter the 6-digit OTP they sent you.",
                            color = MG.muted,
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = otp,
                            onValueChange = { v -> otp = v.filter { it.isDigit() }.take(6) },
                            label = { Text("OTP") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            shape = RoundedCornerShape(16.dp),
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        MainButton(
                            if (state.busy) "Joining..." else "Join family",
                            { onAccept(otp) },
                            enabled = otp.length == 6 && !state.busy,
                            symbol = "✓"
                        )
                    }
                }

                // ---- add a member ----
                Text("Add member", color = MG.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Enter their Gmail. You'll get a 6-digit OTP to send them. They sign in with that " +
                            "Gmail, open Family and type the OTP to join.",
                    color = MG.muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim().take(80) },
                    label = { Text("Their Gmail (e.g. mummy@gmail.com)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(16.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                MainButton(
                    if (state.busy) "Please wait..." else "Get OTP for this Gmail",
                    { onInvite(email) },
                    enabled = email.contains("@") && !state.busy,
                    symbol = "+"
                )

                AnimatedVisibility(
                    visible = state.sent != null,
                    enter = fadeIn(tween(250)) + expandVertically() + scaleIn(initialScale = 0.9f),
                    exit = fadeOut(tween(150)) + shrinkVertically()
                ) {
                    val sent = state.sent
                    if (sent != null) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(MG.bgRaised)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("OTP for ${sent.email}", color = MG.muted, fontSize = 11.sp)
                            Text(
                                sent.otp.chunked(3).joinToString(" "),
                                color = MG.mint,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp
                            )
                            Text("Valid for 24 hours", color = MG.muted, fontSize = 10.sp)
                            MainButton("Send OTP on WhatsApp", { onShareOtp(sent) }, symbol = "↗")
                        }
                    }
                }

                // ---- sharing settings ----
                if (state.circleId != null) {
                    Text(
                        "What I share with family",
                        color = MG.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    val s = state.share
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ShareToggle("Stress", s.stress) { onShareSettings(s.copy(stress = !s.stress)) }
                        ShareToggle("Heart", s.heart) { onShareSettings(s.copy(heart = !s.heart)) }
                        ShareToggle("Sleep", s.sleep) { onShareSettings(s.copy(sleep = !s.sleep)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ShareToggle("Steps", s.steps) { onShareSettings(s.copy(steps = !s.steps)) }
                        ShareToggle("Blood pressure", s.bp) { onShareSettings(s.copy(bp = !s.bp)) }
                    }
                    Text("Turned-off items never leave your phone.", color = MG.muted, fontSize = 10.sp)
                }

                state.error?.let {
                    Text(it, color = MG.coral, fontSize = 11.sp, lineHeight = 16.sp)
                }

                // ---- account ----
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Signed in as $accountEmail",
                        color = MG.muted,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.circleId != null) {
                        Text(
                            "Leave",
                            color = MG.coral,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .bouncyClick(enabled = !state.busy, onClick = onLeave)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        "Sign out",
                        color = MG.lavender,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .bouncyClick(onClick = onSignOut)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/** The four-colour Google "G", drawn so no image asset is needed. */
@Composable
private fun GoogleLogo(diameter: Dp = 22.dp) {
    Canvas(Modifier.size(diameter)) {
        val sw = size.minDimension * 0.22f
        val inset = sw / 2
        val tl = Offset(inset, inset)
        val sz = Size(size.width - sw, size.height - sw)
        val st = Stroke(sw)
        drawArc(Color(0xFF4285F4), -10f, 55f, false, tl, sz, style = st)   // blue
        drawArc(Color(0xFF34A853), 45f, 90f, false, tl, sz, style = st)    // green
        drawArc(Color(0xFFFBBC05), 135f, 70f, false, tl, sz, style = st)   // yellow
        drawArc(Color(0xFFEA4335), 205f, 115f, false, tl, sz, style = st)  // red
        drawLine(
            Color(0xFF4285F4),
            Offset(size.width * 0.52f, size.height / 2),
            Offset(size.width - inset * 0.4f, size.height / 2),
            strokeWidth = sw
        )
    }
}

@Composable
private fun GoogleButton(
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .bouncyClick(enabled = !busy, onClick = onClick)
            .clip(CircleShape)
            .background(Color.White),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        GoogleLogo()
        Spacer(Modifier.width(12.dp))
        Text(
            if (busy) "Signing in..." else "Continue with Google",
            color = Color(0xFF1F1F1F),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LoginScreen(
    busy: Boolean,
    error: String?,
    onGoogle: () -> Unit,
    onSkip: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        BackgroundArtwork(Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(40.dp))

            Box(Modifier.staggerIn(0), contentAlignment = Alignment.Center) {
                BreathingOrb(diameter = 230.dp)
                BrandMark(size = 74.dp)
            }

            Spacer(Modifier.height(18.dp))

            Text(
                "MindGuard AI",
                color = MG.text,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp,
                modifier = Modifier.staggerIn(1)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Understand your stress.\nLook after your family.",
                color = MG.muted,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.staggerIn(2)
            )

            Spacer(Modifier.height(40.dp))

            GoogleButton(busy = busy, onClick = onGoogle, modifier = Modifier.staggerIn(3))

            AnimatedVisibility(visible = error != null) {
                Text(
                    error.orEmpty(),
                    color = MG.coral,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                "Continue without an account",
                color = MG.muted,
                fontSize = 13.sp,
                modifier = Modifier
                    .staggerIn(4)
                    .bouncyClick(onClick = onSkip)
                    .padding(12.dp)
            )

            Spacer(Modifier.height(20.dp))

            Text(
                "Your check-ins and watch data stay on this phone. " +
                        "Your account is only used for Family sharing.",
                color = MG.muted.copy(alpha = 0.8f),
                fontSize = 10.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun ago(ms: Long?): String {
    if (ms == null) return "no readings yet"
    val m = (System.currentTimeMillis() - ms) / 60_000
    return when {
        m < 15 -> "Live"
        m < 60 -> "$m min ago"
        m < 48 * 60 -> "${m / 60} h ago"
        else -> "${m / (60 * 24)} days ago"
    }
}

private val fieldColors
    @Composable get() = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MG.mint,
        unfocusedBorderColor = MG.border,
        focusedLabelColor = MG.mint,
        unfocusedLabelColor = MG.muted,
        focusedTextColor = MG.text,
        unfocusedTextColor = MG.text,
        cursorColor = MG.mint,
        focusedContainerColor = MG.surface,
        unfocusedContainerColor = MG.surface
    )

/** Pulsing green dot shown when a member's watch reported in the last 15 minutes. */
@Composable
private fun LiveDot() {
    val t = rememberInfiniteTransition(label = "live-dot")
    val p by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "live-pulse"
    )
    Canvas(Modifier.size(14.dp)) {
        drawCircle(MG.mint.copy(alpha = (1f - p) * 0.45f), radius = size.minDimension / 2 * (0.4f + 0.6f * p))
        drawCircle(MG.mint, radius = size.minDimension * 0.2f)
    }
}

@Composable
private fun MemberCard(member: FamilyMember, index: Int) {
    val stress = member.stress?.toFloat()
    val live = member.updatedAt != null &&
            System.currentTimeMillis() - member.updatedAt < 15 * 60_000
    val worried = !member.isMe && stress != null && stress >= 6.5f

    val borderAlpha by rememberInfiniteTransition(label = "worry")
        .animateFloat(
            0.25f, if (worried) 0.9f else 0.25f,
            infiniteRepeatable(tween(1100), RepeatMode.Reverse),
            label = "worry-border"
        )

    Row(
        Modifier
            .staggerIn(index)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MG.bgRaised)
            .border(
                1.5.dp,
                if (worried) MG.coral.copy(alpha = borderAlpha) else MG.border,
                RoundedCornerShape(20.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // avatar with a ring in their stress colour
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background((stress?.let { stressColor(it) } ?: MG.muted).copy(alpha = 0.18f))
                    .border(2.dp, stress?.let { stressColor(it) } ?: MG.track, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    member.name.take(1).uppercase(Locale.getDefault()),
                    color = MG.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (live) {
                Box(Modifier.align(Alignment.BottomEnd)) { LiveDot() }
            }
        }

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                member.name + if (member.isMe) " (you)" else "",
                color = MG.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                ago(member.updatedAt),
                color = if (live) MG.mint else MG.muted,
                fontSize = 10.sp,
                fontWeight = if (live) FontWeight.Bold else FontWeight.Normal
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (member.heartRate != null) {
                    BeatingHeart(member.heartRate, size = 18.dp)
                    Text("${member.heartRate} bpm", color = MG.text, fontSize = 11.sp)
                }
                member.sleepHours?.let {
                    Text("😴 ${String.format(Locale.US, "%.1f", it)}h", color = MG.text, fontSize = 11.sp)
                }
                member.steps?.let {
                    Text("👟 ${String.format(Locale.US, "%,d", it)}", color = MG.text, fontSize = 11.sp)
                }
            }
            if (member.systolic != null && member.diastolic != null) {
                Text(
                    "BP ${member.systolic}/${member.diastolic}" +
                            (member.hrv?.let { " · HRV ${it.roundToInt()} ms" } ?: ""),
                    color = MG.muted,
                    fontSize = 10.sp
                )
            } else if (member.hrv != null) {
                Text("HRV ${member.hrv.roundToInt()} ms", color = MG.muted, fontSize = 10.sp)
            }
            if (worried) {
                Text(
                    "Stress looks high. Maybe give them a call 💛",
                    color = MG.coral,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        StressGauge(stress, diameter = 84.dp, caption = "HIDDEN")
    }
}

@Composable
private fun ShareToggle(label: String, on: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (on) MG.mint.copy(alpha = 0.18f) else MG.bgRaised, tween(180), label = "chip-bg-$label"
    )
    Box(
        Modifier
            .bouncyClick(pressedScale = 0.92f, onClick = onClick)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (on) MG.mint.copy(alpha = 0.6f) else MG.border, CircleShape)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            (if (on) "✓ " else "") + label,
            color = if (on) MG.mint else MG.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Plain-text report that reads well in WhatsApp, SMS or email. Missing data is skipped. */
private fun buildReport(
    note: String,
    entries: List<CheckIn>,
    model: FedModel,
    snap: WatchSnapshot?,
    watchEstimate: WatchEstimate?
): String {
    fun f(v: Double) = String.format(Locale.US, "%.1f", v)

    val latest = entries.lastOrNull()
    val ai = latest?.let { FederatedEngine.predictStress(model, it.mood, it.sleep, it.energy) }
    val now = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date())

    return buildString {
        appendLine("🧠 MindGuard wellbeing report · $now")

        if (note.isNotBlank()) {
            appendLine()
            appendLine("\"${note.trim()}\"")
        }

        if (ai != null || watchEstimate != null) {
            appendLine()
            appendLine("STRESS")
            ai?.let { appendLine("• AI estimate (check-in): ${f(it)}/10, ${stressLevel(it.toFloat())}") }
            watchEstimate?.let {
                appendLine("• Watch reading: ${f(it.stress)}/10, ${stressLevel(it.stress.toFloat())}")
            }
            val driver = (watchEstimate?.factors
                ?: latest?.let { FederatedEngine.explain(model, it.mood, it.sleep, it.energy) })
                ?.firstOrNull { it.impact > 0.05 }
            driver?.let { appendLine("• Biggest driver: ${it.name.lowercase(Locale.US)}") }
        }

        if (snap != null && snap.hasAnyData) {
            appendLine()
            appendLine("WATCH")
            snap.heartRate?.let { hr ->
                appendLine("• Heart rate: $hr bpm" + (snap.restingHeartRate?.let { " (resting $it)" } ?: ""))
            }
            snap.hrvMs?.let { appendLine("• HRV: ${it.roundToInt()} ms") }
            if (snap.systolic != null && snap.diastolic != null) {
                appendLine("• Blood pressure: ${snap.systolic}/${snap.diastolic} mmHg")
            }
            snap.sleepHours?.let { appendLine("• Sleep: ${f(it)} h") }
            snap.stepsToday?.let { appendLine("• Steps today: ${String.format(Locale.US, "%,d", it)}") }
        }

        if (latest != null) {
            appendLine()
            appendLine("LATEST CHECK-IN · ${latest.date()}")
            appendLine("• ${latest.summary()}")
        }

        val week = entries.filter { it.time >= System.currentTimeMillis() - 7L * 24 * 3600 * 1000 }
        if (week.size >= 2) {
            appendLine()
            appendLine("LAST 7 DAYS (${week.size} check-ins)")
            appendLine(
                "• Avg mood ${f(week.map { it.mood }.average())}/5 · " +
                        "Avg stress ${f(week.map { it.stress }.average())}/10 · " +
                        "Avg sleep ${f(week.map { it.sleep }.average())} h"
            )
        }

        appendLine()
        appendLine("Could you check in with me when you have a moment?")
        append("— Sent from MindGuard (estimates, not a diagnosis)")
    }
}

@Composable
private fun StressInsightCard(
    model: FedModel,
    latest: CheckIn?,
    bodyIndex: Double?,
    onTrain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val predicted = latest?.let {
        FederatedEngine.predictStress(model, it.mood, it.sleep, it.energy)
    }

    SoftCard(modifier = modifier, color = MG.surfaceRaised) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallPill("AI STRESS INSIGHT", MG.lavender)
            Spacer(Modifier.weight(1f))
            SmallPill(
                if (model.trained) "FL ROUND ${model.rounds}" else "NOT TRAINED",
                if (model.trained) MG.mint else MG.muted
            )
        }

        when {
            latest == null -> {
                Text(
                    "Save your first check-in above. Your phone then trains the federated stress model " +
                            "on it and shows your AI stress insight here.",
                    color = MG.muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            predicted == null -> {
                Text(
                    "The federated model hasn't been trained yet. Run a few rounds in FL Lab.",
                    color = MG.muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                MainButton("Open FL Lab and train", onTrain, symbol = "L")
            }

            else -> {
                val p = predicted.toFloat()
                val factors = FederatedEngine
                    .explain(model, latest.mood, latest.sleep, latest.energy)
                    .take(3)

                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    StressGauge(p, diameter = 190.dp)
                }

                Text(
                    "${stressLevel(p)} stress pattern",
                    color = stressColor(p),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "From your latest check-in (mood ${latest.mood}/5, ${latest.sleep}h sleep, " +
                            "energy ${latest.energy}/10) the model estimates " +
                            "${String.format(Locale.US, "%.1f", p)}/10. " +
                            "You reported ${latest.stress}/10." +
                            (factors.firstOrNull()
                                ?.takeIf { it.impact > 0.05 }
                                ?.let { " Biggest driver: ${it.name.lowercase(Locale.US)}." }
                                ?: ""),
                    color = MG.muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Text(
                    "What's pushing it",
                    color = MG.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                FactorBars(factors)

                if (bodyIndex != null) {
                    val b = bodyIndex.toFloat()
                    Text(
                        "Watch reading: ${String.format(Locale.US, "%.1f", b)}/10 " +
                                "(${stressLevel(b).lowercase(Locale.US)}). " +
                                if (abs(b - p) < 2f) "Your body and your check-in agree."
                                else if (b > p) "Your body looks more stressed than your check-in suggests."
                                else "Your body looks calmer than your check-in suggests.",
                        color = stressColor(b),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveEstimateCard(
    model: FedModel,
    mood: Int,
    sleep: Int,
    energy: Int,
    reportedStress: Int,
    modifier: Modifier = Modifier
) {
    val live = FederatedEngine.predictStress(model, mood, sleep, energy)

    SoftCard(modifier = modifier, color = MG.surfaceRaised) {
        SmallPill("LIVE AI ESTIMATE", MG.lavender)

        if (live == null) {
            Text(
                "Save a check-in or train the model in FL Lab to see a live stress estimate " +
                        "while you move the sliders.",
                color = MG.muted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        } else {
            val p = live.toFloat()
            val diff = reportedStress - p

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StressGauge(p, diameter = 124.dp)

                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        "Reads your mood, sleep and energy",
                        color = MG.muted,
                        fontSize = 11.sp
                    )

                    Text(
                        "You set stress: $reportedStress/10",
                        color = MG.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        when {
                            abs(diff) < 1.5f -> "Close to the model's estimate."
                            diff > 0 -> "Higher than your other signals suggest. Something else may be on your mind."
                            else -> "Lower than the model expected from your other signals."
                        },
                        color = if (abs(diff) < 1.5f) MG.mint else MG.amber,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GhostButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .bouncyClick(enabled = enabled, onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(MG.surface)
            .border(
                1.dp,
                MG.mint.copy(alpha = if (enabled) 0.45f else 0.15f),
                RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) MG.mint else MG.muted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun FederatedScreen(
    model: FedModel,
    userSampleCount: Int,
    onRound: (Boolean) -> Unit,
    onReset: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf(FLPhase.IDLE) }
    var running by remember { mutableStateOf(false) }
    var privacy by rememberSaveable { mutableStateOf(false) }

    // Plays the 4 FL phases with animation; the real FedAvg round runs at the AGGREGATE step
    fun play(rounds: Int, speed: Float) {
        if (running) return
        running = true
        scope.launch {
            try {
                repeat(rounds) {
                    phase = FLPhase.LOCAL
                    delay((900 * speed).toLong())
                    phase = FLPhase.UPLOAD
                    delay((1000 * speed).toLong())
                    phase = FLPhase.AGGREGATE
                    onRound(privacy)
                    delay((800 * speed).toLong())
                    phase = FLPhase.BROADCAST
                    delay((900 * speed).toLong())
                }
            } finally {
                phase = FLPhase.IDLE
                running = false
            }
        }
    }

    val clientColors = listOf(MG.blue, MG.amber, MG.coral)
    val nodes = buildList {
        add(
            NetNode(
                "Your phone",
                if (userSampleCount > 0) "$userSampleCount real check-ins" else "no check-ins yet",
                MG.mint,
                true
            )
        )
        FederatedEngine.simulatedClients.forEachIndexed { i, c ->
            add(
                NetNode(
                    c.name,
                    "${c.samples} synthetic",
                    clientColors[i % clientColors.size],
                    false
                )
            )
        }
    }

    val report = model.lastReport

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 19.dp,
                end = 19.dp,
                top = 16.dp,
                bottom = 24.dp
            ),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        ScreenTitle(
            "Federated learning",
            "Train together,\nshare nothing raw.",
            "Your check-ins train a stress model on this phone. Only model weights travel, never your entries.",
            modifier = Modifier.staggerIn(0)
        )

        SoftCard(modifier = Modifier.staggerIn(1), padding = 14.dp) {
            AnimatedContent(
                targetState = phase,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically { it / 2 })
                        .togetherWith(fadeOut(tween(150)) + slideOutVertically { -it / 2 })
                },
                label = "fl-phase"
            ) { p ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SmallPill(
                        p.label.uppercase(),
                        if (p == FLPhase.IDLE) MG.muted else MG.lavender
                    )
                    Text(
                        p.detail,
                        color = MG.muted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            FederatedNetworkView(nodes, phase, model.rounds)

            Text(
                "Each device shares only ${StressFeatures.size} model weights. Raw check-ins shared: 0." +
                        if (report?.privacyNoise == true) " Last round was clipped and noised." else "",
                color = MG.mint,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }

        Row(
            Modifier.staggerIn(2),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            MetricCard(
                "ROUND",
                model.rounds.toString(),
                if (running) "training..." else "completed",
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                "WITHIN ±1.5",
                report?.let { "${(it.within * 100).roundToInt()}%" } ?: "--",
                "held-out set",
                MG.amber,
                Modifier.weight(1f)
            )
        }

        MainButton(
            if (running) "Federated round running..." else "Run 1 federated round",
            { play(1, 1f) },
            enabled = !running,
            symbol = "▶"
        )

        GhostButton(
            "Fast-train 5 rounds",
            { play(5, 0.4f) },
            enabled = !running
        )

        SoftCard(modifier = Modifier.staggerIn(3)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Privacy noise on updates",
                        color = MG.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Clips each device's update and adds Gaussian noise before averaging (DP-style demo).",
                        color = MG.muted,
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }

                Switch(
                    checked = privacy,
                    onCheckedChange = { privacy = it },
                    enabled = !running,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MG.bg,
                        checkedTrackColor = MG.mint,
                        uncheckedThumbColor = MG.muted,
                        uncheckedTrackColor = MG.track
                    )
                )
            }
        }

        SoftCard(modifier = Modifier.staggerIn(4)) {
            SectionHeading(
                "Model error per round",
                model.maeHistory.lastOrNull()
                    ?.let { String.format(Locale.US, "Now ±%.1f stress points · lower is better", it) }
                    ?: "Lower is better"
            )

            if (model.maeHistory.isEmpty()) {
                Text(
                    "Run a round to start the learning curve.",
                    color = MG.muted,
                    fontSize = 11.sp
                )
            } else {
                ErrorChart(model.maeHistory)
            }
        }

        if (model.trained) {
            SoftCard {
                SectionHeading(
                    "What the global model learned",
                    "How strongly each signal raises predicted stress"
                )

                FactorBars(FederatedEngine.importance(model))

                if (model.personal != null) {
                    Text(
                        "Personalized for you: the global model is fine-tuned on your " +
                                "$userSampleCount check-ins on this phone (FedProx-style)." +
                                (report?.userMae
                                    ?.let { String.format(Locale.US, " Error on your entries: ±%.1f.", it) }
                                    ?: ""),
                        color = MG.lavender,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Box(
            Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Reset model",
                color = MG.muted,
                fontSize = 12.sp,
                modifier = Modifier
                    .bouncyClick(enabled = !running, onClick = onReset)
                    .clip(CircleShape)
                    .padding(
                        horizontal = 18.dp,
                        vertical = 9.dp
                    )
            )
        }

        SoftCard {
            Text(
                "Honest project note",
                color = MG.amber,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Your phone is a real client: it trains on your own check-ins and only its weights are averaged. " +
                        "The other three devices are simulated inside the app with synthetic data. " +
                        "A production system would run clients on separate phones with a server " +
                        "(e.g. Flower or TensorFlow Federated) and secure aggregation. " +
                        "The noise option is illustrative with no formal privacy guarantee, " +
                        "and stress estimates are not a diagnosis.",
                color = MG.muted,
                fontSize = 11.sp,
                lineHeight = 17.sp
            )
        }
    }
}

// ---------------- Smartwatch (Health Connect) ----------------

/** Heart that beats "lub-dub" at the real bpm from the watch. */
@Composable
private fun BeatingHeart(
    bpm: Long?,
    tint: Color = MG.coral,
    size: Dp = 46.dp
) {
    val beatMs = ((60_000L / (bpm ?: 70L).coerceIn(35L, 200L))).toInt()
    val spec = remember(beatMs) {
        infiniteRepeatable<Float>(
            keyframes {
                durationMillis = beatMs
                1f at 0
                1.18f at (beatMs * 0.12f).toInt()
                1f at (beatMs * 0.26f).toInt()
                1.10f at (beatMs * 0.38f).toInt()
                1f at (beatMs * 0.52f).toInt()
            }
        )
    }
    val beat by rememberInfiniteTransition(label = "heart")
        .animateFloat(1f, 1f, spec, label = "heart-beat")

    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = beat
                    scaleY = beat
                }
        ) {
            val w = this.size.width
            val h = this.size.height
            drawCircle(tint.copy(alpha = 0.16f * beat), radius = w * 0.5f)
            val heart = Path().apply {
                moveTo(w * 0.5f, h * 0.80f)
                cubicTo(w * 0.16f, h * 0.58f, w * 0.12f, h * 0.32f, w * 0.32f, h * 0.25f)
                cubicTo(w * 0.42f, h * 0.22f, w * 0.50f, h * 0.30f, w * 0.50f, h * 0.35f)
                cubicTo(w * 0.50f, h * 0.30f, w * 0.58f, h * 0.22f, w * 0.68f, h * 0.25f)
                cubicTo(w * 0.88f, h * 0.32f, w * 0.84f, h * 0.58f, w * 0.5f, h * 0.80f)
                close()
            }
            drawPath(heart, tint)
        }
    }
}

@Composable
private fun VitalTile(
    label: String,
    value: String,
    caption: String,
    tint: Color,
    index: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .staggerIn(index)
            .clip(RoundedCornerShape(16.dp))
            .background(MG.bgRaised)
            .border(1.dp, MG.border, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(label, color = MG.muted, fontSize = 10.sp, maxLines = 1)
        Text(
            value,
            color = if (value == "--") MG.muted else tint,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(caption, color = MG.muted, fontSize = 9.sp, maxLines = 1)
    }
}

private fun minutesAgo(t: Instant?): String {
    if (t == null) return "no recent reading"
    val m = Duration.between(t, Instant.now()).toMinutes()
    return when {
        m < 1 -> "just now"
        m < 60 -> "$m min ago"
        m < 48 * 60 -> "${m / 60} h ago"
        else -> "${m / (60 * 24)} days ago"
    }
}

@Composable
private fun WatchCard(
    state: WatchUiState,
    estimate: WatchEstimate?,
    onConnect: () -> Unit,
    onInstall: () -> Unit,
    onSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snap = state.snapshot

    SoftCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallPill("YOUR WATCH", MG.coral)
            Spacer(Modifier.weight(1f))
            SmallPill(
                when {
                    state.loading -> "SYNCING..."
                    state.connected && snap != null ->
                        "SYNCED " + java.text.SimpleDateFormat("h:mm a", Locale.getDefault())
                            .format(java.util.Date(snap.syncedAt.toEpochMilli()))
                    state.connected -> "CONNECTED"
                    else -> "NOT CONNECTED"
                },
                if (state.connected) MG.mint else MG.muted
            )
        }

        when {
            state.availability == WatchAvailability.UNSUPPORTED -> {
                Text(
                    "Health Connect isn't supported on this phone, so watch data can't be read here.",
                    color = MG.muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            state.availability == WatchAvailability.NEEDS_INSTALL -> {
                Text(
                    "Install Health Connect first. Samsung Health, Pixel Watch and Fitbit share " +
                            "your watch data through it.",
                    color = MG.muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                MainButton("Install Health Connect", onInstall, symbol = "↓")
                GhostButton("I've installed it, check again", onSync)
            }

            !state.connected -> {
                Text(
                    "Read heart rate, HRV, blood pressure, sleep and steps from your smartwatch. " +
                            "You choose exactly which ones on the next screen.",
                    color = MG.muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                MainButton("Connect watch data", onConnect, symbol = "♥")
            }

            else -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BeatingHeart(snap?.heartRate)

                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                snap?.heartRate?.toString() ?: "--",
                                color = MG.text,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1).sp
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "bpm",
                                color = MG.muted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Text(
                            "Heart rate · ${minutesAgo(snap?.heartRateAt)}",
                            color = MG.muted,
                            fontSize = 11.sp
                        )
                    }
                }

                val body = estimate?.stress?.toFloat()

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VitalTile(
                        "Resting HR",
                        snap?.restingHeartRate?.let { "$it bpm" } ?: "--",
                        "last 7 days",
                        MG.mint, 0, Modifier.weight(1f)
                    )
                    VitalTile(
                        "HRV",
                        snap?.hrvMs?.let { "${it.roundToInt()} ms" } ?: "--",
                        "higher = calmer",
                        MG.lavender, 1, Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VitalTile(
                        "Blood pressure",
                        if (snap?.systolic != null && snap.diastolic != null)
                            "${snap.systolic}/${snap.diastolic}" else "--",
                        "mmHg · last 30 days",
                        MG.blue, 2, Modifier.weight(1f)
                    )
                    VitalTile(
                        "Sleep",
                        snap?.sleepHours?.let { String.format(Locale.US, "%.1f h", it) } ?: "--",
                        "last night",
                        MG.amber, 3, Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VitalTile(
                        "Steps",
                        snap?.stepsToday?.let { String.format(Locale.US, "%,d", it) } ?: "--",
                        "today",
                        MG.mint, 4, Modifier.weight(1f)
                    )
                    VitalTile(
                        "Watch stress",
                        body?.let { String.format(Locale.US, "%.1f/10", it) } ?: "--",
                        body?.let { stressLevel(it) } ?: "needs 2+ signals",
                        body?.let { stressColor(it) } ?: MG.muted,
                        5, Modifier.weight(1f)
                    )
                }

                if (snap != null && snap.hasAnyData && estimate == null) {
                    Text(
                        "For a stress reading the watch needs at least two of: sleep, HRV, heart rate, steps.",
                        color = MG.amber,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }

                if (snap != null && !snap.hasAnyData) {
                    Text(
                        "Connected, but no readings yet. In Samsung Health turn on sync with " +
                                "Health Connect, wear the watch for a while, then sync again.",
                        color = MG.amber,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }

                state.error?.let {
                    Text(it, color = MG.coral, fontSize = 11.sp)
                }

                GhostButton(
                    if (state.loading) "Syncing..." else "Sync now",
                    onSync,
                    enabled = !state.loading
                )

                Text(
                    "Movement, caffeine and illness also raise heart rate, so the watch reading is a rough guide.",
                    color = MG.muted,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}


/** Watch face with a live ECG line and a spinning scan ring. */
@Composable
private fun WatchScanAnimation(diameter: Dp = 150.dp) {
    val t = rememberInfiniteTransition(label = "watch-scan")
    val sweep by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "ecg"
    )
    val spin by t.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(1700, easing = LinearEasing)),
        label = "ring"
    )
    val glow by t.animateFloat(
        0.4f, 1f,
        infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "glow"
    )

    Canvas(Modifier.size(diameter)) {
        val w = this.size.width
        val h = this.size.height
        val c = center

        drawCircle(
            Brush.radialGradient(
                listOf(MG.coral.copy(alpha = 0.22f * glow), Color.Transparent),
                c,
                w * 0.5f
            ),
            w * 0.5f,
            c
        )

        rotate(spin) {
            drawArc(
                color = MG.mint,
                startAngle = 0f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = Offset(w * 0.06f, h * 0.06f),
                size = Size(w * 0.88f, h * 0.88f),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // strap
        drawRoundRect(
            MG.surfaceSoft,
            Offset(w * 0.36f, h * 0.14f),
            Size(w * 0.28f, h * 0.72f),
            CornerRadius(w * 0.06f)
        )

        // watch body
        val tl = Offset(w * 0.25f, h * 0.27f)
        val bs = Size(w * 0.50f, h * 0.46f)
        drawRoundRect(MG.bg, tl, bs, CornerRadius(w * 0.12f))
        drawRoundRect(
            MG.mint.copy(alpha = 0.75f), tl, bs, CornerRadius(w * 0.12f),
            style = Stroke(2.dp.toPx())
        )

        // scrolling ECG
        clipRect(tl.x + 6f, tl.y, tl.x + bs.width - 6f, tl.y + bs.height) {
            val mid = c.y
            val period = bs.width * 0.6f
            var x = tl.x - period + sweep * period
            val ecg = Path().apply { moveTo(x, mid) }
            while (x < tl.x + bs.width + period) {
                ecg.lineTo(x + period * 0.30f, mid)
                ecg.lineTo(x + period * 0.38f, mid - bs.height * 0.08f)
                ecg.lineTo(x + period * 0.44f, mid + bs.height * 0.06f)
                ecg.lineTo(x + period * 0.50f, mid - bs.height * 0.32f)
                ecg.lineTo(x + period * 0.56f, mid + bs.height * 0.18f)
                ecg.lineTo(x + period * 0.62f, mid)
                ecg.lineTo(x + period, mid)
                x += period
            }
            drawPath(
                ecg, MG.coral,
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/** Automatic watch reading: scans each signal, then reveals the stress level. Replays on every sync. */
@Composable
private fun WatchStressReveal(
    estimate: WatchEstimate,
    snap: WatchSnapshot,
    modifier: Modifier = Modifier
) {
    val readings = listOf(
        "Heart rate" to (snap.heartRate?.let { "$it bpm" } ?: "no data"),
        "HRV" to (snap.hrvMs?.let { "${it.roundToInt()} ms" } ?: "no data"),
        "Sleep" to (snap.sleepHours?.let { String.format(Locale.US, "%.1f h", it) } ?: "no data"),
        "Steps" to (snap.stepsToday?.let { String.format(Locale.US, "%,d", it) } ?: "no data")
    )

    var scanning by remember(snap.syncedAt) { mutableStateOf(true) }
    var step by remember(snap.syncedAt) { mutableStateOf(0) }

    LaunchedEffect(snap.syncedAt) {
        scanning = true
        step = 0
        repeat(readings.size) {
            delay(480)
            step = it + 1
        }
        delay(400)
        scanning = false
    }

    val live = snap.heartRateAt?.let {
        Duration.between(it, Instant.now()).toMinutes() < 20
    } ?: false

    SoftCard(modifier = modifier, color = MG.surfaceRaised) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallPill(if (live) "WATCH ON WRIST · LIVE" else "FROM YOUR WATCH", MG.coral)
            Spacer(Modifier.weight(1f))
            SmallPill("AUTO", MG.mint)
        }

        AnimatedContent(
            targetState = scanning,
            transitionSpec = {
                (fadeIn(tween(320)) + scaleIn(initialScale = 0.92f))
                    .togetherWith(fadeOut(tween(180)))
                    .using(SizeTransform(clip = false))
            },
            label = "watch-reveal"
        ) { isScanning ->
            if (isScanning) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WatchScanAnimation()

                    Text(
                        "Reading your watch...",
                        color = MG.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    readings.forEachIndexed { i, (label, value) ->
                        val done = i < step
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (done) "✓" else "•",
                                color = if (done) MG.mint else MG.muted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(22.dp)
                            )
                            Text(
                                label,
                                color = if (done) MG.text else MG.muted,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            AnimatedVisibility(
                                visible = done,
                                enter = fadeIn(tween(200)) + slideInHorizontally { it / 2 }
                            ) {
                                Text(
                                    value,
                                    color = if (value == "no data") MG.muted else MG.mint,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            } else {
                val p = estimate.stress.toFloat()

                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "By watch reading, your stress level is",
                        color = MG.muted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    StressGauge(p, diameter = 200.dp, caption = "WATCH")

                    Text(
                        "${stressLevel(p)} stress",
                        color = stressColor(p),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        readings
                            .filter { it.second != "no data" }
                            .joinToString(" · ") { "${it.first} ${it.second}" },
                        color = MG.muted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Center
                    )

                    FactorBars(estimate.factors.take(3), Modifier.fillMaxWidth())

                    Text(
                        "Federated model reading ${estimate.signals} of 4 watch signals. " +
                                "Updates by itself while you wear the watch.",
                        color = MG.muted,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ---------------- Settings ----------------

@Composable
private fun SettingsScreen(
    account: Account?,
    family: FamilyState,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onShareSettings: (ShareSettings) -> Unit,
    onLeave: () -> Unit,
    onRemove: (FamilyMember) -> Unit
) {
    BackHandler(onBack = onBack)

    var confirmRemove by remember { mutableStateOf<FamilyMember?>(null) }

    val viewers = family.members.filter { !it.isMe }
    val s = family.share
    val sharedItems = listOfNotNull(
        "Stress".takeIf { s.stress },
        "Heart".takeIf { s.heart },
        "Sleep".takeIf { s.sleep },
        "Steps".takeIf { s.steps },
        "BP".takeIf { s.bp }
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(MG.bg)
    ) {
        BackgroundArtwork(Modifier.fillMaxSize())

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 19.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "‹",
                    color = MG.text,
                    fontSize = 34.sp,
                    modifier = Modifier
                        .bouncyClick(onClick = onBack)
                        .padding(end = 14.dp)
                )
                Text(
                    "Settings",
                    color = MG.text,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp
                )
            }

            // ---- account ----
            SoftCard(modifier = Modifier.staggerIn(0), color = MG.surfaceRaised) {
                SmallPill("ACCOUNT", MG.lavender)
                if (account == null) {
                    Text(
                        "You're using MindGuard without an account. Check-ins, watch and FL work fine; " +
                                "sign in to use Family.",
                        color = MG.muted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    GoogleButton(busy = false, onClick = onSignIn)
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(MG.mint, MG.lavender))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                account.name.take(1).uppercase(Locale.getDefault()),
                                color = MG.bg,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(account.name, color = MG.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(account.email, color = MG.muted, fontSize = 12.sp)
                        }
                    }
                }
            }

            // ---- who can see my data ----
            SoftCard(modifier = Modifier.staggerIn(1)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Who can see my health & stress",
                        color = MG.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    SmallPill(
                        if (viewers.isEmpty()) "NOBODY" else "${viewers.size} PEOPLE",
                        if (viewers.isEmpty()) MG.mint else MG.amber
                    )
                }

                if (viewers.isEmpty()) {
                    Text(
                        "Nobody. Your readings stay on this phone until you add family.",
                        color = MG.muted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                } else {
                    viewers.forEachIndexed { i, v ->
                        Row(
                            Modifier
                                .staggerIn(i + 2)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MG.bgRaised)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MG.blue.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    v.name.take(1).uppercase(Locale.getDefault()),
                                    color = MG.text,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(v.name, color = MG.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(v.email ?: "family member", color = MG.muted, fontSize = 11.sp)
                                Text(
                                    if (sharedItems.isEmpty()) "Sees: nothing (all sharing off)"
                                    else "Sees: " + sharedItems.joinToString(" · "),
                                    color = MG.mint,
                                    fontSize = 10.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(ago(v.updatedAt), color = MG.muted, fontSize = 10.sp)
                                Text(
                                    "Remove",
                                    color = MG.coral,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .bouncyClick(enabled = !family.busy) { confirmRemove = v }
                                        .padding(top = 6.dp, start = 8.dp, bottom = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ---- what they see ----
            if (family.circleId != null) {
                SoftCard(modifier = Modifier.staggerIn(2)) {
                    SectionHeading("What they can see", "Tap to turn on or off. Changes apply instantly.")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ShareToggle("Stress", s.stress) { onShareSettings(s.copy(stress = !s.stress)) }
                        ShareToggle("Heart", s.heart) { onShareSettings(s.copy(heart = !s.heart)) }
                        ShareToggle("Sleep", s.sleep) { onShareSettings(s.copy(sleep = !s.sleep)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ShareToggle("Steps", s.steps) { onShareSettings(s.copy(steps = !s.steps)) }
                        ShareToggle("Blood pressure", s.bp) { onShareSettings(s.copy(bp = !s.bp)) }
                    }
                    GhostButton(
                        if (family.busy) "Please wait..." else "Stop sharing with everyone (leave circle)",
                        onLeave,
                        enabled = !family.busy
                    )
                }
            }

            // ---- privacy note ----
            SoftCard(modifier = Modifier.staggerIn(3)) {
                SectionHeading("Stays on this phone")
                Text(
                    "Check-ins, the federated stress models and raw watch history never leave your phone. " +
                            "Only the items above are shared, and only with people in your circle.",
                    color = MG.muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            family.error?.let {
                Text(it, color = MG.coral, fontSize = 11.sp, lineHeight = 16.sp)
            }

            // ---- log out ----
            if (account != null) {
                Box(
                    Modifier
                        .staggerIn(4)
                        .fillMaxWidth()
                        .height(52.dp)
                        .bouncyClick(onClick = onSignOut)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MG.coral.copy(alpha = 0.12f))
                        .border(1.dp, MG.coral.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Log out", color = MG.coral, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                "MindGuard AI · student project · not a medical device",
                color = MG.muted.copy(alpha = 0.7f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    confirmRemove?.let { m ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            containerColor = MG.surfaceRaised,
            title = { Text("Remove ${m.name}?", color = MG.text) },
            text = {
                Text(
                    "${m.name} will stop seeing your health and stress right away, and you will stop " +
                            "seeing theirs. You can invite them again later with a new OTP.",
                    color = MG.muted
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(m)
                    confirmRemove = null
                }) {
                    Text("Remove", color = MG.coral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = null }) {
                    Text("Cancel", color = MG.text)
                }
            }
        )
    }
}

private fun entriesToSamples(entries: List<CheckIn>): List<StressSample> =
    entries.map {
        StressSample(
            StressFeatures.of(it.mood, it.sleep, it.energy),
            StressFeatures.label(it.stress)
        )
    }

private fun loadEntries(context: Context): List<CheckIn> {
    val raw = context
        .getSharedPreferences("mindguard_demo", Context.MODE_PRIVATE)
        .getString("entries", "")
        .orEmpty()

    return raw.split(';').mapNotNull { row ->
        val p = row.split(',')

        if (p.size != 5) {
            null
        } else {
            runCatching {
                CheckIn(
                    p[0].toLong(),
                    p[1].toInt(),
                    p[2].toInt(),
                    p[3].toInt(),
                    p[4].toInt()
                )
            }.getOrNull()
        }
    }.takeLast(30)
}

private fun loadCount(
    context: Context,
    fallback: Int
): Int =
    context
        .getSharedPreferences("mindguard_demo", Context.MODE_PRIVATE)
        .getInt("count", fallback)
        .coerceAtLeast(fallback)

private fun saveEntries(
    context: Context,
    entries: List<CheckIn>,
    count: Int
) {
    val serialized = entries.joinToString(";") {
        "${it.time},${it.mood},${it.stress},${it.sleep},${it.energy}"
    }

    context
        .getSharedPreferences("mindguard_demo", Context.MODE_PRIVATE)
        .edit()
        .putString("entries", serialized)
        .putInt("count", count)
        .apply()
}