package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.data.DeliveryEntity
import com.example.data.SessionEntity
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun StumpVisionApp(viewModel: StumpViewModel) {
    val state by viewModel.uiState.collectAsState()

    // Handle system back button so that it mimics the header back button and does not exit the app unexpectedly
    if (state.currentScreen != AppScreen.SessionsList) {
        BackHandler {
            val backTarget = when (state.currentScreen) {
                AppScreen.SetupGuide -> AppScreen.SessionsList
                AppScreen.LiveCapture -> AppScreen.SetupGuide
                AppScreen.DeliveryDetail -> AppScreen.Dashboard
                AppScreen.DrsReview -> AppScreen.DeliveryDetail
                AppScreen.Dashboard -> AppScreen.LiveCapture
                else -> AppScreen.SessionsList
            }
            viewModel.navigateTo(backTarget)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // App Header Dashboard HUD
            StumpHeader(state = state, onNavBack = {
                val backTarget = when (state.currentScreen) {
                    AppScreen.SetupGuide -> AppScreen.SessionsList
                    AppScreen.LiveCapture -> AppScreen.SetupGuide
                    AppScreen.DeliveryDetail -> AppScreen.Dashboard
                    AppScreen.DrsReview -> AppScreen.DeliveryDetail
                    AppScreen.Dashboard -> AppScreen.LiveCapture
                    else -> AppScreen.SessionsList
                }
                viewModel.navigateTo(backTarget)
            })

            // Content Screens Switcher
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = state.currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        AppScreen.SessionsList -> SessionsListScreen(
                            state = state,
                            viewModel = viewModel
                        )
                        AppScreen.SetupGuide -> SetupGuideScreen(
                            state = state,
                            viewModel = viewModel
                        )
                        AppScreen.LiveCapture -> LiveCaptureScreen(
                            state = state,
                            viewModel = viewModel
                        )
                        AppScreen.DeliveryDetail -> DeliveryDetailScreen(
                            state = state,
                            viewModel = viewModel
                        )
                        AppScreen.DrsReview -> DrsReviewScreen(
                            state = state,
                            viewModel = viewModel
                        )
                        AppScreen.Dashboard -> SessionDashboardScreen(
                            state = state,
                            viewModel = viewModel
                        )
                    }
                }
            }
            
            // 1. Exporting Progress Dialog Overlay
            if (state.isExportingVideo) {
                AlertDialog(
                    onDismissRequest = {},
                    confirmButton = {},
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                progress = { state.exportProgress },
                                color = BrightAmber,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "SAVING TRACKED VIDEO",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SmoothWhite,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    },
                    text = {
                        Column {
                            Text(
                                "Compiling frame-by-frame 3D Hawkeye trajectory analytics, overlay stats, and saving to gallery...",
                                color = LightSlate,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(
                                progress = { state.exportProgress },
                                color = CricketGreen,
                                trackColor = Color(0xFF1E293B),
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Status: ${if (state.exportProgress < 0.85f) "Rendering frames" else "Writing MP4 payload"}",
                                    fontSize = 10.sp,
                                    color = LightSlate,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "${(state.exportProgress * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrightAmber
                                )
                            }
                        }
                    },
                    containerColor = Color(0xFF0F172A),
                    properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
                )
            }

            // 2. Export Success Dialog Modal
            if (state.lastExportedUriString != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearExportState() },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.clearExportState() }
                        ) {
                            Text("DISMISS", color = BrightAmber, fontWeight = FontWeight.Bold)
                        }
                    },
                    icon = {
                        StumpEyeLogo(modifier = Modifier.size(64.dp))
                    },
                    title = {
                        Text(
                            text = "SAVED TO GALLERY!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = CricketGreen,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Your tracked delivery video has been saved safely directly to your device library (Movies/StumpVision folder)!",
                                color = SmoothWhite,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF020617), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CricketGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Format: MP4 H.264 • Portrait Full HUD",
                                    color = LightSlate,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    },
                    containerColor = Color(0xFF0F172A)
                )
            }
        }
    }
}


@Composable
fun StumpEyeLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = minOf(w, h) * 0.45f
        
        // 1. Solid pure white structured leather ball base
        drawCircle(
            color = Color(0xFFF8FAFC),
            radius = radius,
            center = Offset(cx, cy)
        )
        
        // 2. 3D Spherical shadow shading overlay (dark gradient from bottom-right)
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x1A334155)),
                center = Offset(cx - radius * 0.2f, cy - radius * 0.2f),
                radius = radius * 1.2f
            ),
            radius = radius,
            center = Offset(cx, cy)
        )
        
        // 3. Concentric Dual Circular cricket-seam stitch patterns enclosing the eyeball
        val redSeamColor = Color(0xFFB91C1C)
        val deepRedLineColor = Color(0xFF7F1D1D)
        
        // Central seam separator line
        drawCircle(
            color = deepRedLineColor,
            radius = radius * 0.825f,
            center = Offset(cx, cy),
            style = Stroke(width = 0.8f.dp.toPx())
        )
        
        // Outer stitch circle with dashed line to simulate stitches
        val outerDashEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
            floatArrayOf(radius * 0.06f, radius * 0.05f), 0f
        )
        drawCircle(
            color = redSeamColor,
            radius = radius * 0.86f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.2f.dp.toPx(), pathEffect = outerDashEffect)
        )
        
        // Inner stitch circle
        drawCircle(
            color = redSeamColor,
            radius = radius * 0.79f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.2f.dp.toPx(), pathEffect = outerDashEffect)
        )
        
        // 4. Eyeball Sclera base (White of the eye) inside the cricket ball seam
        val scleraRadius = radius * 0.75f
        drawCircle(
            color = Color(0xFFFAF9F6),
            radius = scleraRadius,
            center = Offset(cx, cy)
        )
        
        // Translucent sclera shading for spherical depth
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(Color.Transparent, Color(0x1F091A3A)),
                center = Offset(cx, cy),
                radius = scleraRadius
            ),
            radius = scleraRadius,
            center = Offset(cx, cy)
        )
        
        // 5. Exquisite bloodshot ocular veins for realistic lens look
        val veinColor = Color(0xFFEF4444).copy(alpha = 0.3f)
        val veinStroke = Stroke(width = 0.4f.dp.toPx(), cap = StrokeCap.Round)
        
        // Left eye vein path
        drawPath(
            path = Path().apply {
                moveTo(cx - scleraRadius + 4.dp.toPx(), cy - radius * 0.1f)
                quadraticTo(cx - radius * 0.5f, cy - radius * 0.05f, cx - radius * 0.4f, cy + radius * 0.02f)
            },
            color = veinColor,
            style = veinStroke
        )
        drawPath(
            path = Path().apply {
                moveTo(cx - scleraRadius + 6.dp.toPx(), cy + radius * 0.08f)
                quadraticTo(cx - radius * 0.48f, cy + radius * 0.02f, cx - radius * 0.41f, cy + radius * 0.05f)
                lineTo(cx - radius * 0.38f, cy + radius * 0.12f)
            },
            color = veinColor,
            style = veinStroke
        )
        
        // Right eye vein path
        drawPath(
            path = Path().apply {
                moveTo(cx + scleraRadius - 4.dp.toPx(), cy - radius * 0.1f)
                quadraticTo(cx + radius * 0.5f, cy - radius * 0.05f, cx + radius * 0.4f, cy + radius * 0.02f)
            },
            color = veinColor,
            style = veinStroke
        )
        
        // 6. Vibrant Multi-Tonal Blue Iris
        // Sapphire Blue (outer iris rim)
        val irisRadius = radius * 0.5f
        drawCircle(
            color = Color(0xFF1E3A8A),
            radius = irisRadius,
            center = Offset(cx, cy)
        )
        // Vibrant Blue (middle tier)
        drawCircle(
            color = Color(0xFF2563EB),
            radius = radius * 0.41f,
            center = Offset(cx, cy)
        )
        // Crystal Light Sky Blue (inner glowing tier)
        drawCircle(
            color = Color(0xFF38BDF8),
            radius = radius * 0.32f,
            center = Offset(cx, cy)
        )
        
        // Radial iris fiber accents circle
        val irisFibersEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
            floatArrayOf(radius * 0.02f, radius * 0.03f), 0f
        )
        drawCircle(
            color = Color(0xFF93C5FD).copy(alpha = 0.4f),
            radius = radius * 0.36f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.2f.dp.toPx(), pathEffect = irisFibersEffect)
        )
        
        // 7. Pitch Black pupil center
        val pupilRadius = radius * 0.24f
        drawCircle(
            color = Color(0xFF0B0F19),
            radius = pupilRadius,
            center = Offset(cx, cy)
        )
        
        // 8. Three Golden Wooden Wickets (stumps) slanted perfectly inside the center pupil
        val lStumpTop = Offset(cx - radius * 0.13f, cy - radius * 0.21f)
        val lStumpBot = Offset(cx - radius * 0.20f, cy + radius * 0.21f)
        
        val mStumpTop = Offset(cx, cy - radius * 0.26f)
        val mStumpBot = Offset(cx - radius * 0.07f, cy + radius * 0.26f)
        
        val rStumpTop = Offset(cx + radius * 0.13f, cy - radius * 0.21f)
        val rStumpBot = Offset(cx + radius * 0.06f, cy + radius * 0.21f)
        
        val stumpW = radius * 0.046f
        val stumpHighlightW = radius * 0.016f
        
        // Draw Left Stump
        drawLine(
            color = Color(0xFFB45309),
            start = lStumpBot,
            end = lStumpTop,
            strokeWidth = stumpW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFFBBF24),
            start = Offset(lStumpBot.x + 0.8.dp.toPx(), lStumpBot.y),
            end = Offset(lStumpTop.x + 0.8.dp.toPx(), lStumpTop.y),
            strokeWidth = stumpHighlightW,
            cap = StrokeCap.Round
        )
        
        // Draw Middle Stump
        drawLine(
            color = Color(0xFFB45309),
            start = mStumpBot,
            end = mStumpTop,
            strokeWidth = stumpW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFFBBF24),
            start = Offset(mStumpBot.x + 0.8.dp.toPx(), mStumpBot.y),
            end = Offset(mStumpTop.x + 0.8.dp.toPx(), mStumpTop.y),
            strokeWidth = stumpHighlightW,
            cap = StrokeCap.Round
        )
        
        // Draw Right Stump
        drawLine(
            color = Color(0xFFB45309),
            start = rStumpBot,
            end = rStumpTop,
            strokeWidth = stumpW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFFBBF24),
            start = Offset(rStumpBot.x + 0.8.dp.toPx(), rStumpBot.y),
            end = Offset(rStumpTop.x + 0.8.dp.toPx(), rStumpTop.y),
            strokeWidth = stumpHighlightW,
            cap = StrokeCap.Round
        )
        
        // 9. Bails resting across the stumps
        val bailW = radius * 0.032f
        drawLine(
            color = Color(0xFF78350F),
            start = Offset(cx - radius * 0.16f, cy - radius * 0.23f),
            end = Offset(cx - radius * 0.01f, cy - radius * 0.26f),
            strokeWidth = bailW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF78350F),
            start = Offset(cx + radius * 0.01f, cy - radius * 0.26f),
            end = Offset(cx + radius * 0.15f, cy - radius * 0.23f),
            strokeWidth = bailW,
            cap = StrokeCap.Round
        )
        
        // 10. Photorealistic glossy reflections and highlights over eyeball/lens
        // Big soft white shoulder highlight
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = radius * 0.14f,
            center = Offset(cx - radius * 0.36f, cy - radius * 0.36f)
        )
        
        // Specular sharp highlight pinpoint
        drawCircle(
            color = Color.White,
            radius = radius * 0.05f,
            center = Offset(cx - radius * 0.42f, cy - radius * 0.42f)
        )
        
        // Lower wet crescent reflection glow
        drawCircle(
            color = Color.White.copy(alpha = 0.12f),
            radius = radius * 0.11f,
            center = Offset(cx + radius * 0.36f, cy + radius * 0.35f)
        )
    }
}

@Composable
fun StumpHeader(state: StumpUiState, onNavBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color(0xFF0F172A))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.currentScreen != AppScreen.SessionsList) {
            IconButton(
                onClick = onNavBack,
                modifier = Modifier.testTag("nav_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = BrightAmber
                )
            }
        } else {
            StumpEyeLogo(
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = "STUMP VISION",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp,
                color = SmoothWhite
            )
            Text(
                text = when (state.currentScreen) {
                    AppScreen.SessionsList -> "AI BOWLING PERSISTENCE"
                    AppScreen.SetupGuide -> "CAMERA TRIPOD CALIBRATION"
                    AppScreen.LiveCapture -> "LIVE AUTO-CLIP RECORDING"
                    AppScreen.DeliveryDetail -> "BALL ANALYSIS DETECT"
                    AppScreen.DrsReview -> "HAWKEYE 3D DRS SYSTEM"
                    AppScreen.Dashboard -> "SESSION REPORT CARD"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BrightAmber,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (state.currentScreen == AppScreen.LiveCapture) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (state.isRecording) LiveRed.copy(alpha = 0.2f) else MutedNavy)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (state.isRecording) LiveRed else LightSlate)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (state.isRecording) "LIVE DETECT" else "PAUSED",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isRecording) SmoothWhite else LightSlate,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun SessionsListScreen(state: StumpUiState, viewModel: StumpViewModel) {
    var expandedNewSession by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, CricketGreen.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "NEW SPELL SETUP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrightAmber,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.bowlerNameInput,
                        onValueChange = { viewModel.setBowlerName(it) },
                        label = { Text("Bowler Name") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedIndicatorColor = BrightAmber,
                            unfocusedIndicatorColor = LightSlate
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bowler_name_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = state.sessionNameInput,
                        onValueChange = { viewModel.setSessionName(it) },
                        label = { Text("Session Description") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedIndicatorColor = BrightAmber,
                            unfocusedIndicatorColor = LightSlate
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "BOWLER TECHNIQUE STYLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LightSlate
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Fast", "Off Spinner", "Leg Spinner").forEach { style ->
                            val selected = state.bowlerType == style
                            Button(
                                onClick = { viewModel.setBowlerType(style) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) CricketGreen else MutedNavy,
                                    contentColor = if (selected) BrightAmber else SmoothWhite
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = style, fontSize = 11.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.startNewSession() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrightAmber,
                            contentColor = DeepNavy
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_session_button")
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "START CALIBRATION AND SPELL",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "PAST TRACKING SESSIONS (${state.sessionsList.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = BrightAmber
            )
        }

        if (state.sessionsList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = LightSlate.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No saved spells yet.\nInitialize your camera configuration above and bowl!",
                            textAlign = TextAlign.Center,
                            color = LightSlate,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(state.sessionsList) { session ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.selectSession(session)
                            viewModel.navigateTo(AppScreen.LiveCapture)
                        },
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    border = BorderStroke(1.dp, BorderCyan.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SportsCricket,
                                contentDescription = null,
                                tint = BrightAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = session.bowlerName.uppercase(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = SmoothWhite
                                )
                                Text(
                                    text = session.sessionName,
                                    fontSize = 12.sp,
                                    color = LightSlate
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.deleteSession(session) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Delete",
                                    tint = LiveRed.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = LightSlate.copy(alpha = 0.15f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("BALLS DELIVERED", fontSize = 10.sp, color = LightSlate)
                                Text(
                                    "${session.totalDeliveries} Balls",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = SmoothWhite
                                )
                            }
                            Column {
                                Text("AVG SPEED", fontSize = 10.sp, color = LightSlate)
                                Text(
                                    "${String.format("%.1f", session.averageSpeed)} km/h",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = BrightAmber
                                )
                            }
                            Column {
                                Text("ECONOMY", fontSize = 10.sp, color = LightSlate)
                                Text(
                                    String.format("%.2f", session.economyRate),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = NeonGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetupGuideScreen(state: StumpUiState, viewModel: StumpViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isFrontCamera by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "TRIPOD WICKET ALIGNMENT",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = BrightAmber,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Mount your phone on a tripod at the bowler crease heights, pointing towards the opposite stumps. Align the red wicket overlay lines with the real physical pitch boundaries.",
            fontSize = 12.sp,
            color = LightSlate
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Aligned preview placeholder or CameraX view
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, BorderCyan.copy(alpha = 0.5f))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Camera preview
            AndroidView(
                factory = { contextView ->
                    PreviewView(contextView).apply {
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = CameraPreview.Builder().build().also {
                                it.setSurfaceProvider(surfaceProvider)
                            }
                            val cameraSelector = if (isFrontCamera) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview
                                )
                            } catch (e: Exception) {
                                // Fallback if camera is unavailable in compiler environments
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Stump Calibration Overlay Lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = size.width / 2f
                val baseHeight = size.height * 0.7f
                val stumpWidth = 24.dp.toPx()
                val stumpHeight = 220.dp.toPx() + (state.calibrationOffset * 10f)

                // Render drawing target wickets matching pitch
                drawRoundRect(
                    color = LiveRed.copy(alpha = state.stumpGridOpacity),
                    topLeft = Offset(center - (stumpWidth * 1.5f), baseHeight - stumpHeight),
                    size = Size(stumpWidth * 3f, stumpHeight),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )

                // Center target line
                drawLine(
                    color = BrightAmber,
                    start = Offset(center, 0f),
                    end = Offset(center, size.height),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                // Calibration Horizontal limits
                drawLine(
                    color = BorderCyan.copy(alpha = 0.5f),
                    start = Offset(0f, baseHeight),
                    end = Offset(size.width, baseHeight),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Small watermark overlay
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "PITCH COMPOSITE AUTO-GRID",
                    color = BrightAmber,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calibration customization inputs
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Calibration Line Opacity", fontSize = 12.sp, color = SmoothWhite)
                    Text("${(state.stumpGridOpacity * 100).toInt()}%", fontSize = 12.sp, color = BrightAmber)
                }
                Slider(
                    value = state.stumpGridOpacity,
                    onValueChange = { viewModel.updateGridOpacity(it) },
                    valueRange = 0.1f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = BrightAmber,
                        activeTrackColor = CricketGreen
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Digital Vertical Offset Height", fontSize = 12.sp, color = SmoothWhite)
                    Text("${state.calibrationOffset.toInt()} px", fontSize = 12.sp, color = BrightAmber)
                }
                Slider(
                    value = state.calibrationOffset,
                    onValueChange = { viewModel.updateCalibration(it) },
                    valueRange = -50f..50f,
                    colors = SliderDefaults.colors(
                        thumbColor = BrightAmber,
                        activeTrackColor = CricketGreen
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.navigateTo(AppScreen.LiveCapture) },
            colors = ButtonDefaults.buttonColors(containerColor = CricketGreen, contentColor = SmoothWhite),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("lock_calibration_button")
        ) {
            Icon(imageVector = Icons.Default.Lock, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("LOCK ALIGNMENT & DEPLOY ENGINE", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LiveCaptureScreen(state: StumpUiState, viewModel: StumpViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Keep dynamic updated states so camera factory/analyzer callback doesn't capture stale snapshots
    val currentRecordingState by rememberUpdatedState(state.isRecording)
    val currentAutoDetectEnabled by rememberUpdatedState(state.isAutoDetectEnabled)
    val currentSimulationActive by rememberUpdatedState(state.isSimulationActive)
    val currentShowClipDetectedOverlay by rememberUpdatedState(state.showClipDetectedOverlay)
    val currentCaptureMode by rememberUpdatedState(state.captureMode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        val totalBalls = state.deliveries.size

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ACTIVE SPELL: ${state.currentSession?.bowlerName?.uppercase() ?: "BOWLER"}",
                    fontSize = 11.sp,
                    color = LightSlate,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.currentSession?.sessionName ?: "Pitch Practice"}",
                    fontSize = 13.sp,
                    color = SmoothWhite,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { viewModel.navigateTo(AppScreen.Dashboard) },
                colors = ButtonDefaults.buttonColors(containerColor = CardBackground, contentColor = BrightAmber),
                border = BorderStroke(1.dp, BrightAmber),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Icon(imageVector = Icons.Default.AutoGraph, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("COACH REPORTS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Live Feed + Ball Fly Animation
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, CricketGreen.copy(alpha = 0.5f))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { contextView ->
                    PreviewView(contextView).apply {
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = CameraPreview.Builder().build().also {
                                it.setSurfaceProvider(surfaceProvider)
                            }
                            
                            // Initialize ImageAnalysis use case to act as consecutive frame motion/light differentiator
                            var lastAverageLuminance = -1f
                            var lastDetectionTime = 0L
                            
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            
                            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                                val planes = imageProxy.planes
                                if (planes.isNotEmpty() && currentRecordingState && currentAutoDetectEnabled && currentCaptureMode == "Sensor") {
                                    val buffer = planes[0].buffer
                                    val data = ByteArray(buffer.remaining())
                                    buffer.get(data)
                                    
                                    // Sample central zone pixels for fast computation
                                    var sum = 0L
                                    var count = 0
                                    val step = 16 // Speed optimization
                                    for (i in data.indices step step) {
                                        sum += data[i].toInt() and 0xFF
                                        count++
                                    }
                                    val currentAverageLuminance = if (count > 0) sum.toFloat() / count else -1f
                                    
                                    val now = System.currentTimeMillis()
                                    if (lastAverageLuminance >= 0f && now - lastDetectionTime > 5000L && !currentSimulationActive && !currentShowClipDetectedOverlay) {
                                        val diff = kotlin.math.abs(currentAverageLuminance - lastAverageLuminance)
                                        // A sudden change in pixel intensity indicates a real ball/subject has entered/passed the calibrated area
                                        if (diff > 5.0f) {
                                            lastDetectionTime = now
                                            viewModel.simulateAutomaticDelivery()
                                        }
                                    }
                                    if (currentAverageLuminance >= 0f) {
                                        lastAverageLuminance = currentAverageLuminance
                                    }
                                }
                                imageProxy.close()
                            }
                            
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                // Stub if no camera is available
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Simulated stadium bowling grid overlays
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = size.width / 2f
                val topPitch = size.height * 0.15f
                val bottomPitch = size.height * 0.85f

                // Draw cricket pitch layout perspective lines
                val path = Path().apply {
                    moveTo(center - size.width * 0.14f, topPitch)
                    lineTo(center + size.width * 0.14f, topPitch)
                    lineTo(center + size.width * 0.38f, bottomPitch)
                    lineTo(center - size.width * 0.38f, bottomPitch)
                    close()
                }
                drawPath(
                    path = path,
                    color = CricketGreen.copy(alpha = 0.25f)
                )
                drawPath(
                    path = path,
                    color = BorderCyan.copy(alpha = 0.4f),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Crease lines
                drawLine(
                    color = SmoothWhite.copy(alpha = 0.6f),
                    start = Offset(center - size.width * 0.18f, topPitch + size.height * 0.08f),
                    end = Offset(center + size.width * 0.18f, topPitch + size.height * 0.08f),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = SmoothWhite.copy(alpha = 0.6f),
                    start = Offset(center - size.width * 0.34f, bottomPitch - size.height * 0.08f),
                    end = Offset(center + size.width * 0.34f, bottomPitch - size.height * 0.08f),
                    strokeWidth = 2.dp.toPx()
                )

                // Draw stumps at batsman end (top end)
                val baseHeight = topPitch + size.height * 0.06f
                val stumpHeight = 44.dp.toPx()
                val spacing = 8.dp.toPx()
                for (offsetMultiplier in -1..1) {
                    val sx = center + (offsetMultiplier * spacing)
                    drawLine(
                        color = BrightAmber,
                        start = Offset(sx, baseHeight),
                        end = Offset(sx, baseHeight - stumpHeight),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                // Bails
                drawLine(
                    color = BrightAmber,
                    start = Offset(center - spacing - 2f, baseHeight - stumpHeight),
                    end = Offset(center + spacing + 2f, baseHeight - stumpHeight),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Draw animated ball flying tracking line if simulation is active
                if (state.isSimulationActive) {
                    val p = state.simProgress

                    // Calculate path from bowler crease (bottom) to batsman stumps (top)
                    // The ball starts from the bottom (closer, larger) and flies toward the top end stumps (further, smaller)
                    val startX = center + size.width * 0.12f
                    val startY = bottomPitch

                    val bouncePointY = topPitch + (bottomPitch - topPitch) * 0.45f // Good length area
                    val bouncePointX = center - size.width * 0.04f

                    // Target finishes at the batsman/stumps (base height is topPitch + size.height * 0.06f)
                    val targetX = center - size.width * 0.01f
                    val targetY = topPitch + size.height * 0.06f - 16.dp.toPx()

                    val currentX: Float
                    val currentY: Float

                    if (p < 0.6f) {
                        // Segment 1: Bowler release to pitch bounce
                        val subP = p / 0.6f
                        currentY = startY + (bouncePointY - startY) * subP
                        currentX = startX + (bouncePointX - startX) * subP
                    } else {
                        // Segment 2: Rise from the pitch to the stumps
                        val subP = (p - 0.6f) / 0.4f
                        currentY = bouncePointY + (targetY - bouncePointY) * subP
                        currentX = bouncePointX + (targetX - bouncePointX) * subP
                    }

                    // Perspective scaling: gets smaller as it travels away
                    val currentRadius = (13.dp.toPx() * (1f - p * 0.65f)).coerceAtLeast(4.dp.toPx())

                    // Draw tracking trail
                    drawCircle(
                        color = BrightAmber.copy(alpha = 0.3f),
                        radius = currentRadius * 2.5f,
                        center = Offset(currentX, currentY)
                    )
                    drawCircle(
                        color = LiveRed,
                        radius = currentRadius,
                        center = Offset(currentX, currentY)
                    )
                }
            }

            // Status bar overlays inside camera feed
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (state.isRecording) NeonGreen else LightSlate)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "COMPUTER VISION: ACTIVE",
                            color = SmoothWhite,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Auto clips detected notification banner overlay
            if (state.showClipDetectedOverlay) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.5.dp, BrightAmber),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 40.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("DELIVERY AUTO-DETECTED", color = BrightAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("Speed: ${String.format("%.1f", state.detectedSpeed)} km/h • Clip saved locally", color = SmoothWhite, fontSize = 11.sp)
                        }
                    }
                }
            }

            // AI Trace Pipeline Analysis Stage HUD
            if (state.isTracingGenerationActive) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        // Flashing tracking crosshair ring
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(80.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { state.tracingProgress },
                                color = BorderCyan,
                                strokeWidth = 1.5.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = BrightAmber,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Text(
                            text = "STUMP-EYE TRACER ENGINE",
                            color = BorderCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "${(state.tracingProgress * 100).toInt()}% COMPLETED",
                            color = SmoothWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Current stage indicator
                        Box(
                            modifier = Modifier
                                .background(CardBackground, RoundedCornerShape(6.dp))
                                .border(1.dp, BorderCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NeonGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.tracingProgressText,
                                    color = SmoothWhite,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Processing recorded bowler clip frames @60 FPS",
                            color = LightSlate,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Dashboard metrics row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("BALLS THROWN", fontSize = 10.sp, color = LightSlate)
                    Text("$totalBalls", fontSize = 18.sp, fontWeight = FontWeight.Black, color = SmoothWhite)
                }
            }
            Card(
                modifier = Modifier.weight(1.3f),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("AVG SPEED", fontSize = 10.sp, color = LightSlate)
                    Text(
                        "${String.format("%.1f", state.currentSession?.averageSpeed ?: 0f)} km/h",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = BrightAmber
                    )
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("DOT BALLS %", fontSize = 10.sp, color = LightSlate)
                    Text(
                        "${(state.currentSession?.dotBallPercentage ?: 0f).toInt()}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Capture Mode Selector (Video Recording vs Auto Sensor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBackground, RoundedCornerShape(8.dp))
                .border(1.dp, BorderCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (state.captureMode == "Video") CricketGreen else Color.Transparent)
                    .clickable { viewModel.setCaptureMode("Video") }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = if (state.captureMode == "Video") SmoothWhite else LightSlate,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RECORD & TRACE",
                        color = if (state.captureMode == "Video") SmoothWhite else LightSlate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (state.captureMode == "Sensor") CricketGreen else Color.Transparent)
                    .clickable { viewModel.setCaptureMode("Sensor") }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = if (state.captureMode == "Sensor") SmoothWhite else LightSlate,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SMART SENSOR",
                        color = if (state.captureMode == "Sensor") SmoothWhite else LightSlate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.captureMode == "Video") {
            // HIGH-FIDELITY BOWLER VIDEO CAPTURE MODE CONTROLS
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, BorderCyan.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!state.isRecordingVideo) {
                        // Not recording state
                        Text(
                            text = "READY TO CAPTURE DELIVERIES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrightAmber,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Point camera at batsman from behind bowler. Tap record, let the bowler bowl, then tap stop to compute 3D tracking.",
                            fontSize = 10.sp,
                            color = LightSlate,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.startVideoRecording() },
                            colors = ButtonDefaults.buttonColors(containerColor = LiveRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(SmoothWhite)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("START VIDEO RECORDING", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        // Recording state active
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(LiveRed.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .border(1.dp, LiveRed.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(LiveRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RECORDING BOWLER ACTIVE",
                                color = SmoothWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            val min = state.videoRecordingSeconds / 60
                            val sec = state.videoRecordingSeconds % 60
                            Text(
                                text = String.format("%02d:%02d", min, sec),
                                color = BrightAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Filming delivery live! Tap stop once the ball passes the target zone to analyze.",
                            fontSize = 10.sp,
                            color = LightSlate,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.stopVideoRecordingAndTrack() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrightAmber, contentColor = DeepNavy),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(DeepNavy)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("STOP & GENERATE TRACKING", fontWeight = FontWeight.Black, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        } else {
            // SENSOR TRIGGER METHOD CODES
            // Stump-Eye Motion Sensor Trigger toggle card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleAutoDetect() },
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (state.isAutoDetectEnabled) NeonGreen.copy(alpha = 0.4f) else LightSlate.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (state.isAutoDetectEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = null,
                            tint = if (state.isAutoDetectEnabled) NeonGreen else LightSlate,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "STUMP-EYE SMART SENSOR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SmoothWhite,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (state.isAutoDetectEnabled) 
                                    "Auto-triggers camera on ball passing wicket" 
                                else 
                                    "Sensor paused • Manual simulate trigger only",
                                fontSize = 10.sp,
                                color = LightSlate
                            )
                        }
                    }
                    Switch(
                        checked = state.isAutoDetectEnabled,
                        onCheckedChange = { viewModel.toggleAutoDetect() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepNavy,
                            checkedTrackColor = NeonGreen,
                            uncheckedThumbColor = LightSlate,
                            uncheckedTrackColor = MutedNavy
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trigger simulator button inside HUD
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.toggleCameraRecording() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isRecording) LiveRed else MutedNavy,
                        contentColor = SmoothWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (state.isRecording) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (state.isRecording) "Pause Feed" else "Resume Feed", fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.simulateAutomaticDelivery() },
                    enabled = !state.isSimulationActive,
                    colors = ButtonDefaults.buttonColors(containerColor = BrightAmber, contentColor = DeepNavy),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("simulate_delivery_button")
                ) {
                    Icon(imageVector = Icons.Default.FlashOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SIMULATE TEST BALL", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Active ball logs rows
        Text(
            text = "DELIVERIES IN THIS SPELL",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = LightSlate,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        if (state.deliveries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.5f),
                contentAlignment = Alignment.Center
            ) {
                Text("Preseed 'BOWL DELIVERY' to trigger machine learning analytics", color = LightSlate, fontSize = 11.sp)
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.deliveries.asReversed()) { delivery ->
                    Box(
                        modifier = Modifier
                            .width(134.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardBackground)
                            .border(1.dp, BorderCyan.copy(alpha = 0.2f))
                            .clickable {
                                viewModel.selectDelivery(delivery)
                                viewModel.navigateTo(AppScreen.DeliveryDetail)
                            }
                            .padding(8.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "BALL ${delivery.deliveryNum}",
                                    color = BrightAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (delivery.isEdgeDetected) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(LiveRed)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("EDGE", color = SmoothWhite, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "${String.format("%.1f", delivery.speedKmph)} km/h",
                                color = SmoothWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                delivery.swingType,
                                color = LightSlate,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Review >",
                                color = BorderCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeliveryDetailScreen(state: StumpUiState, viewModel: StumpViewModel) {
    val delivery = state.selectedDelivery ?: return
    var playerProgress by remember { mutableStateOf(0.45f) } // Slow mo progress

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.HistoryEdu, contentDescription = null, tint = BrightAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BALL #${delivery.deliveryNum} REPORT CARD",
                            color = BrightAmber,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("BALL SPEED", fontSize = 10.sp, color = LightSlate)
                            Text(
                                "${String.format("%.1f", delivery.speedKmph)} km/h",
                                color = SmoothWhite,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "${String.format("%.1f", delivery.speedKmph * 0.621371f)} mph",
                                color = LightSlate,
                                fontSize = 13.sp
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("SWING RADIAL", fontSize = 10.sp, color = LightSlate)
                            Text(
                                delivery.swingType,
                                color = BrightAmber,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Seam: ${delivery.seamMovement}",
                                color = SmoothWhite,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Divider(color = LightSlate.copy(alpha = 0.15f))

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("SPIN RATE", fontSize = 10.sp, color = LightSlate)
                            Text("${delivery.spinRateRpm} RPM", fontWeight = FontWeight.Bold, color = SmoothWhite)
                        }
                        Column {
                            Text("BOUNCE DECK", fontSize = 10.sp, color = LightSlate)
                            Text("${String.format("%.1f", delivery.bounceAngle)}° Rise", fontWeight = FontWeight.Bold, color = SmoothWhite)
                        }
                        Column {
                            Text("L&L LANDING", fontSize = 10.sp, color = LightSlate)
                            Text(delivery.lineLengthClass.substringBefore("("), fontWeight = FontWeight.Bold, color = SmoothWhite)
                        }
                    }
                }
            }
        }

        // Slow-mo video play scraper simulator
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "COMPUTER VISION OVERLAY SCRAMBLER",
                        color = BrightAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulated Slow-Mo Video Scrubber Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing frame by frame action based on slider
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = size.width / 2f
                            val strokeWWidth = 4.dp.toPx()

                            // Render Pitch and animated ball traveling on user drag scrubbing
                            drawRect(
                                color = CricketGreen.copy(alpha = 0.3f),
                                topLeft = Offset(center - size.width * 0.2f, size.height * 0.1f),
                                size = Size(size.width * 0.4f, size.height * 0.8f)
                            )

                            // Drawn bowler release or wickets
                            drawLine(
                                color = SmoothWhite.copy(alpha = 0.5f),
                                start = Offset(center - size.width * 0.2f, size.height * 0.7f),
                                end = Offset(center + size.width * 0.2f, size.height * 0.7f),
                                strokeWidth = 2.dp.toPx()
                            )

                            // Live coordinates of ball landing coordinate based on state
                            val bx = center + (delivery.pitchLocationX - 0.5f) * size.width * 0.35f
                            val by = size.height * 0.1f + (1f - playerProgress) * size.height * 0.7f

                            drawCircle(
                                color = LiveRed,
                                radius = 10.dp.toPx(),
                                center = Offset(bx, by)
                            )

                            // Show speed vector line
                            drawLine(
                                color = BrightAmber,
                                start = Offset(bx, by),
                                end = Offset(bx + (if (delivery.swingType == "In-Swing") -20f else 20f), by - 14f),
                                strokeWidth = 2f
                            )
                        }

                        // Video timing HUD
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.8f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "PRO-SLO • FPS: 240 • FRAME ${(playerProgress * 100).toInt()}",
                                color = BrightAmber,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.DirectionsRun, contentDescription = null, tint = LightSlate)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scrub Delivery Timeline", fontSize = 12.sp, color = SmoothWhite)
                    }

                    Slider(
                        value = playerProgress,
                        onValueChange = { playerProgress = it },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = BrightAmber,
                            activeTrackColor = CricketGreen
                        )
                    )
                }
            }
        }

        // Pitch Diagram layout
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "PITCH BIRD'S-EYE LANDING SPOT",
                        color = BrightAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw Green Pitch Rectangle
                            drawRect(
                                color = CricketGreen,
                                topLeft = Offset(w * 0.1f, h * 0.2f),
                                size = Size(w * 0.8f, h * 0.6f)
                            )

                            // Crease boundaries
                            drawLine(color = SmoothWhite, start = Offset(w * 0.18f, h * 0.2f), end = Offset(w * 0.18f, h * 0.8f), strokeWidth = 1.5.dp.toPx())
                            drawLine(color = SmoothWhite, start = Offset(w * 0.82f, h * 0.2f), end = Offset(w * 0.82f, h * 0.8f), strokeWidth = 1.5.dp.toPx())

                            // Draw opposite stumps representation
                            drawRect(color = BrightAmber, topLeft = Offset(w * 0.14f, h * 0.45f), size = Size(4.dp.toPx(), h * 0.1f))

                            // Draw mapped landing coordinate
                            val lx = w * 0.1f + (delivery.pitchLocationY * w * 0.8f) // Y represents landing progress along pitch
                            val ly = h * 0.2f + (delivery.pitchLocationX * h * 0.6f) // X represents lateral landing offset

                            drawCircle(
                                color = BrightAmber,
                                radius = 7.dp.toPx(),
                                center = Offset(lx, ly)
                            )
                            drawCircle(
                                color = LiveRed,
                                radius = 3.dp.toPx(),
                                center = Offset(lx, ly)
                            )
                        }
                    }
                }
            }
        }

        // EXPORT TO GALLERY ACTION CARD
        item {
            OutlinedButton(
                onClick = { viewModel.exportTrackedDeliveryToGallery(delivery) },
                border = BorderStroke(1.dp, BorderCyan),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BorderCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_video_gallery_button")
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("EXPORT & SAVE TRACKED MP4 TO GALLERY", fontWeight = FontWeight.Bold)
            }
        }

        // DRS Action Launcher
        item {
            Button(
                onClick = { viewModel.navigateTo(AppScreen.DrsReview) },
                colors = ButtonDefaults.buttonColors(containerColor = BrightAmber, contentColor = DeepNavy),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("launch_drs_button")
            ) {
                Icon(imageVector = Icons.Default.Analytics, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("LAUNCH HAWKEYE 3D DRS MODULE", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DrsReviewScreen(state: StumpUiState, viewModel: StumpViewModel) {
    val delivery = state.selectedDelivery ?: return
    var playTraj by remember { mutableStateOf(true) }
    val trajProgress = remember { Animatable(0f) }

    // Audio soundwave coordinates parse for snicko-meter visualization
    val waves = remember(delivery.audioWaveform) {
        if (delivery.audioWaveform.isBlank()) {
            Array(100) { Random.nextDouble(-0.1, 0.1).toFloat() }
        } else {
            delivery.audioWaveform.split(",").map { it.toFloatOrNull() ?: 0f }.toTypedArray()
        }
    }

    LaunchedEffect(playTraj) {
        if (playTraj) {
            trajProgress.snapTo(0f)
            trajProgress.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(1500, easing = LinearEasing)
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "3D TRAJECTORY PREDICTION",
                    color = BrightAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                IconButton(onClick = { playTraj = !playTraj }) {
                    Icon(
                        imageVector = if (playTraj) Icons.Default.Refresh else Icons.Default.PlayArrow,
                        contentDescription = "Replay",
                        tint = BrightAmber
                    )
                }
            }
        }

        // DRS Hawkeye Trajectory 3D perspective Canvas
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, BorderCyan.copy(alpha = 0.5f))
                    .background(Color(0xFF020617)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val center = w / 2f

                    // Draw 3D-angled pitch lines
                    val pTopLeft = Offset(center - w * 0.1f, h * 0.2f)
                    val pTopRight = Offset(center + w * 0.1f, h * 0.2f)
                    val pBottomRight = Offset(center + w * 0.45f, h * 0.85f)
                    val pBottomLeft = Offset(center - w * 0.45f, h * 0.85f)

                    val pitchPath = Path().apply {
                        moveTo(pTopLeft.x, pTopLeft.y)
                        lineTo(pTopRight.x, pTopRight.y)
                        lineTo(pBottomRight.x, pBottomRight.y)
                        lineTo(pBottomLeft.x, pBottomLeft.y)
                        close()
                    }
                    drawPath(pitchPath, color = CricketGreen.copy(alpha = 0.4f))
                    drawPath(pitchPath, color = BorderCyan.copy(alpha = 0.3f), style = Stroke(2f))

                    // Draw batsman end stumps (middle-center perspective)
                    val stumpBaseY = h * 0.24f
                    val stHeight = 60.dp.toPx()
                    val stSpacing = 10.dp.toPx()

                    for (i in -1..1) {
                        val sx = center + (i * stSpacing)
                        // Stump
                        drawLine(
                            color = SmoothWhite.copy(alpha = 0.8f),
                            start = Offset(sx, stumpBaseY),
                            end = Offset(sx, stumpBaseY - stHeight),
                            strokeWidth = 3f
                        )
                    }
                    // Bails
                    drawLine(
                        color = BrightAmber,
                        start = Offset(center - stSpacing - 1f, stumpBaseY - stHeight),
                        end = Offset(center + stSpacing + 1f, stumpBaseY - stHeight),
                        strokeWidth = 3f
                    )

                    // Compute ball trajectory along timeline progress
                    val p = trajProgress.value

                    // Ball starts from bottom-right (bowler release) to batsman crease
                    val ballsStartX = center + w * 0.3f
                    val ballStartY = h * 0.85f

                    // Landing spot on deck
                    val landingX = center + (delivery.pitchLocationX - 0.5f) * w * 0.25f
                    val landingY = h * 0.42f

                    // Final collision coordinate or projection height
                    val finalX = center + (delivery.pitchLocationX - 0.52f) * w * 0.15f
                    val finalY = stumpBaseY - stHeight * 0.35f // Strikes stump height midpoint

                    val bx: Float
                    val by: Float

                    if (p < 0.65f) {
                        // Phase 1: In the air to pitching spot
                        val sp = p / 0.65f
                        bx = ballsStartX + (landingX - ballsStartX) * sp
                        // Ball arc path
                        by = ballStartY + (landingY - ballStartY) * sp - (40f * sin(sp * Math.PI.toFloat()))
                    } else {
                        // Phase 2: Rise off deck to batsmen collision strike
                        val sp = (p - 0.65f) / 0.35f
                        bx = landingX + (finalX - landingX) * sp
                        by = landingY + (finalY - landingY) * sp - (15f * sin(sp * Math.PI.toFloat()))

                        // Draw projected hit indicator line (Hawkeye style)
                        drawLine(
                            color = LiveRed,
                            start = Offset(landingX, landingY),
                            end = Offset(finalX, finalY),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                        )
                    }

                    // Render running track trail
                    drawCircle(
                        color = LiveRed,
                        radius = (6.dp.toPx() + (1f - p) * 8.dp.toPx()),
                        center = Offset(bx, by)
                    )

                    // Draw landing contact spot on pitch
                    if (p > 0.65f) {
                        drawCircle(
                            color = BrightAmber,
                            radius = 6.dp.toPx(),
                            center = Offset(landingX, landingY)
                        )
                    }
                }

                // HUD labels
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "HAWKEYE TRACK PROJECTION ENGINE",
                        color = BrightAmber,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // DRS Verdict Status Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (delivery.drsVerdict.contains("OUT")) Color(0xFF1C1318) else Color(0xFF131C18)
                ),
                border = BorderStroke(1.5.dp, if (delivery.drsVerdict.contains("OUT")) LiveRed else NeonGreen)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DECISION REVIEW VERDICT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = LightSlate,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (delivery.drsVerdict.contains("OUT")) LiveRed else NeonGreen)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (delivery.drsVerdict.contains("OUT")) "OUT" else "NOT OUT",
                                color = SmoothWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = delivery.drsVerdict,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = SmoothWhite
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Projected Stump Contact: YES (Middle Stump)",
                            fontSize = 12.sp,
                            color = SmoothWhite
                        )
                        Text(
                            text = "Confidence: ${String.format("%.1f", delivery.drsConfidence)}%",
                            fontSize = 12.sp,
                            color = BrightAmber,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Snick-o-meter Waveform spike
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "SNICK-O-METER AUDIO GRAPH",
                            color = BrightAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (delivery.isEdgeDetected) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(LiveRed)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("BAT EDGE CONTACT", color = SmoothWhite, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("NO CONTACT EDGE", color = LightSlate, fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Snicko Scrolling Soundwave Canvas representation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color.Black, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val midY = size.height / 2f
                            val numSamples = waves.size
                            val barWidth = size.width / numSamples

                            // Draw horizontal zero line
                            drawLine(
                                color = LightSlate.copy(alpha = 0.3f),
                                start = Offset(0f, midY),
                                end = Offset(size.width, midY),
                                strokeWidth = 1f
                            )

                            // Render vertical sound columns
                            for (i in 0 until numSamples) {
                                val value = waves[i]
                                val barHeight = value * (size.height / 2f) * 0.95f
                                val color = if (delivery.isEdgeDetected && i in 42..48) LiveRed else BorderCyan

                                drawLine(
                                    color = color,
                                    start = Offset(i * barWidth, midY - barHeight),
                                    end = Offset(i * barWidth, midY + barHeight),
                                    strokeWidth = 2f
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Audio correlation correlates camera alignment timeline with microphone sensor spikes to prove bat contact edges.",
                        color = LightSlate,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SessionDashboardScreen(state: StumpUiState, viewModel: StumpViewModel) {
    val session = state.currentSession ?: return

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Review Coach Panel powered by Gemini API
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E38)),
                border = BorderStroke(1.5.dp, BorderCyan)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Coach logo",
                            tint = BorderCyan,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "STUMPVISION COCHING CHIP",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BorderCyan,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "Elite AI Analysis reports for Bowlers",
                                fontSize = 11.sp,
                                color = LightSlate
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.isAnalyzing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = BorderCyan,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Analyzing spell metrics via server GenAI...", color = SmoothWhite, fontSize = 12.sp)
                        }
                    } else {
                        Text(
                            text = state.coachFeedback.ifBlank { "No analytical reports built yet. Let AI Coach examine your bowling variables." },
                            color = SmoothWhite,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.fetchCoachingFeedback(session) },
                        colors = ButtonDefaults.buttonColors(containerColor = BorderCyan, contentColor = DeepNavy),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analyze_spell_button")
                    ) {
                        Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CALCULATE COACH TIPS", fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        // Traditional Bowling stats details card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "METRIC SPELL TOTALS",
                        color = BrightAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("BOWLER NAME", fontSize = 10.sp, color = LightSlate)
                            Text(session.bowlerName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SmoothWhite)
                        }
                        Column {
                            Text("BALL DELIVERIES", fontSize = 10.sp, color = LightSlate)
                            Text("${session.totalDeliveries}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SmoothWhite)
                        }
                        Column {
                            Text("METHOD", fontSize = 10.sp, color = LightSlate)
                            Text(state.bowlerType, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrightAmber)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = LightSlate.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("ECONOMY", fontSize = 10.sp, color = LightSlate)
                            Text(String.format("%.2f", session.economyRate), fontWeight = FontWeight.Black, fontSize = 20.sp, color = NeonGreen)
                        }
                        Column {
                            Text("DOT BALLS %", fontSize = 10.sp, color = LightSlate)
                            Text("${session.dotBallPercentage.toInt()}%", fontWeight = FontWeight.Black, fontSize = 20.sp, color = BorderCyan)
                        }
                        Column {
                            Text("BOUNDARY %", fontSize = 10.sp, color = LightSlate)
                            Text("${session.boundaryPercentage.toInt()}%", fontWeight = FontWeight.Black, fontSize = 20.sp, color = LiveRed)
                        }
                    }
                }
            }
        }

        // Pitch Map heatmap representation
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "PITCH MAP HEATMAP LANDINGS",
                        color = BrightAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw Green Pitch Rectangle
                            drawRect(
                                color = CricketGreen,
                                topLeft = Offset(w * 0.1f, h * 0.2f),
                                size = Size(w * 0.8f, h * 0.6f)
                            )

                            // Crease boundaries
                            drawLine(color = SmoothWhite, start = Offset(w * 0.18f, h * 0.1f), end = Offset(w * 0.18f, h * 0.9f), strokeWidth = 2f)
                            drawLine(color = SmoothWhite, start = Offset(w * 0.82f, h * 0.1f), end = Offset(w * 0.82f, h * 0.9f), strokeWidth = 2f)

                            // Loop and draw all landing heat dots
                            state.deliveries.forEach { delivery ->
                                val lx = w * 0.1f + (delivery.pitchLocationY * w * 0.8f)
                                val ly = h * 0.2f + (delivery.pitchLocationX * h * 0.6f)

                                // Blur gradient heat halo
                                drawCircle(
                                    color = BrightAmber.copy(alpha = 0.35f),
                                    radius = 12.dp.toPx(),
                                    center = Offset(lx, ly)
                                )
                                drawCircle(
                                    color = LiveRed.copy(alpha = 0.8f),
                                    radius = 5.dp.toPx(),
                                    center = Offset(lx, ly)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Heatmap points highlight landing consistency. Tighter group lines indicate top length consistency.",
                        color = LightSlate,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Bowling Speed over time Fatigue Graph
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "SPEED FATIGUE TRACKER (KM/H)",
                        color = BrightAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.deliveries.isEmpty()) {
                        Text("No ball speeds registered yet. Throw some deliveries first.", color = LightSlate, fontSize = 11.sp)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                val maxVal = 160f
                                val minVal = 60f

                                val points = state.deliveries.mapIndexed { idx, d ->
                                    val x = idx * (w / (state.deliveries.size.coerceAtLeast(2) - 1))
                                    val speedRatio = (d.speedKmph - minVal) / (maxVal - minVal)
                                    val y = h - (speedRatio.coerceIn(0f, 1f) * h)
                                    Offset(x, y)
                                }

                                // Draw vertical axis guidelines
                                for (i in 1..3) {
                                    val y = h * (i / 4f)
                                    drawLine(
                                        color = LightSlate.copy(alpha = 0.15f),
                                        start = Offset(0f, y),
                                        end = Offset(w, y),
                                        strokeWidth = 1f
                                    )
                                }

                                // Connect coordinates
                                val path = Path()
                                points.forEachIndexed { index, offset ->
                                    if (index == 0) {
                                        path.moveTo(offset.x, offset.y)
                                    } else {
                                        path.lineTo(offset.x, offset.y)
                                    }
                                    // Highlight each ball coordinate node
                                    drawCircle(color = BrightAmber, radius = 4.dp.toPx(), center = offset)
                                }

                                drawPath(
                                    path = path,
                                    color = BorderCyan,
                                    style = Stroke(width = 3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
