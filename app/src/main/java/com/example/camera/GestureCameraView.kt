package com.example.camera

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun GestureCameraScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PermissionChecker.PERMISSION_GRANTED
        )
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PermissionChecker.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
        hasAudioPermission = permissions[Manifest.permission.RECORD_AUDIO] == true
    }

    if (!hasCameraPermission) {
        PermissionScreen(
            onGrantClicked = {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                    )
                )
            }
        )
    } else {
        CameraViewfinder(
            hasAudioPermission = hasAudioPermission,
            modifier = modifier
        )
    }
}

@Composable
private fun PermissionScreen(
    onGrantClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF131B2E)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFF1E293B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Permission",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "Pure Camera",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )

                Text(
                    text = "A clean, UI-free camera experience designed around simple gestures. Double-tap to capture photos auto-enhanced with AI to iPhone 17 Pro Max quality, hold to record video clips, and pinch to zoom.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF94A3B8),
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onGrantClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("grant_camera_permission_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0284C7),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Enable Camera Access",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraViewfinder(
    hasAudioPermission: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val cameraManager = remember { CameraManager(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Flash animation state
    val flashAlpha = remember { Animatable(0f) }

    // Zoom badge state
    var currentZoomRatio by remember { mutableFloatStateOf(1f) }
    var minZoomRatio by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(8f) }
    var showZoomBadge by remember { mutableStateOf(false) }

    // Tap-to-focus ring state
    var focusPosition by remember { mutableStateOf<Offset?>(null) }
    val focusScale = remember { Animatable(1.4f) }
    val focusAlpha = remember { Animatable(0f) }

    // Video recording states
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableLongStateOf(0L) }

    // Floating notifications (toast pills)
    var notificationMessage by remember { mutableStateOf<String?>(null) }
    var isVideoNotification by remember { mutableStateOf(false) }

    // Gesture Guide HUD
    var showGestureGuide by remember { mutableStateOf(true) }

    // Auto-dismiss gesture guide after 4.5 seconds
    LaunchedEffect(Unit) {
        delay(4500)
        showGestureGuide = false
    }

    // Dismiss notification pill after 2.5 seconds
    LaunchedEffect(notificationMessage) {
        if (notificationMessage != null) {
            delay(2500)
            notificationMessage = null
        }
    }

    // Dismiss zoom badge after 1.5 seconds of inactivity
    LaunchedEffect(currentZoomRatio) {
        if (showZoomBadge) {
            delay(1500)
            showZoomBadge = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraManager.release()
        }
    }

    fun showFlash() {
        scope.launch {
            flashAlpha.snapTo(0.85f)
            flashAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
        }
    }

    fun triggerFocus(tapOffset: Offset, viewWidth: Int, viewHeight: Int) {
        if (viewWidth <= 0 || viewHeight <= 0) return
        focusPosition = tapOffset

        scope.launch {
            focusScale.snapTo(1.4f)
            focusAlpha.snapTo(0.9f)
            focusScale.animateTo(1.0f, tween(180, easing = FastOutSlowInEasing))
            delay(1200)
            focusAlpha.animateTo(0f, tween(250, easing = LinearEasing))
            focusPosition = null
        }

        val factory = SurfaceOrientedMeteringPointFactory(
            viewWidth.toFloat(),
            viewHeight.toFloat()
        )
        val point = factory.createPoint(tapOffset.x, tapOffset.y)
        cameraManager.focusOnPoint(point)
    }

    fun capturePhoto() {
        showFlash()
        notificationMessage = "✨ Enhancing to iPhone 17 Pro Max..."
        isVideoNotification = false

        cameraManager.takePhoto(
            onEnhanceStatus = { status ->
                notificationMessage = status
                isVideoNotification = false
            },
            onImageSaved = { uri, isAiMaster ->
                notificationMessage = if (isAiMaster) {
                    "✨ iPhone 17 Pro Max AI Master • Saved"
                } else {
                    "✨ iPhone 17 Pro Max Enhanced • Saved"
                }
                isVideoNotification = false
            },
            onError = { exc ->
                notificationMessage = "Capture failed: ${exc.message}"
                isVideoNotification = false
            }
        )
    }

    fun startVideoClip() {
        if (isRecording) return
        cameraManager.startRecording(
            hasAudioPermission = hasAudioPermission
        ) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    isRecording = true
                    recordingDurationSeconds = 0L
                }
                is VideoRecordEvent.Status -> {
                    recordingDurationSeconds = event.recordingStats.recordedDurationNanos / 1_000_000_000L
                }
                is VideoRecordEvent.Finalize -> {
                    isRecording = false
                    val durationText = formatDuration(recordingDurationSeconds)
                    if (!event.hasError()) {
                        notificationMessage = "Video saved ($durationText)"
                        isVideoNotification = true
                    } else {
                        notificationMessage = "Video stopped"
                        isVideoNotification = true
                    }
                }
            }
        }
    }

    fun stopVideoClip() {
        if (isRecording) {
            cameraManager.stopRecording()
        }
    }

    fun flipLens() {
        val pView = previewView ?: return
        cameraManager.flipCamera(lifecycleOwner, pView) { cam ->
            cam.cameraInfo.zoomState.value?.let { zState ->
                currentZoomRatio = zState.zoomRatio
                minZoomRatio = zState.minZoomRatio
                maxZoomRatio = zState.maxZoomRatio
            }
        }
    }

    // Infinite transition for recording pulse
    val infiniteTransition = rememberInfiniteTransition(label = "recordingPulse")
    val recordingPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_viewfinder_container")
    ) {
        // CameraX PreviewView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                    previewView = this

                    cameraManager.startCamera(
                        lifecycleOwner = lifecycleOwner,
                        previewView = this,
                        onCameraBound = { boundCamera ->
                            boundCamera.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                                zoomState?.let {
                                    currentZoomRatio = it.zoomRatio
                                    minZoomRatio = it.minZoomRatio
                                    maxZoomRatio = it.maxZoomRatio
                                }
                            }
                        }
                    )
                }
            }
        )

        // Recording screen edge indicator (red border vignette)
        if (isRecording) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 4.dp,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFFF1744).copy(alpha = recordingPulseAlpha * 0.7f)
                            )
                        ),
                        shape = androidx.compose.ui.graphics.RectangleShape
                    )
            )
        }

        // Tap-to-focus indicator ring
        focusPosition?.let { pos ->
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (pos.x - 36.dp.toPx()).roundToInt(),
                            (pos.y - 36.dp.toPx()).roundToInt()
                        )
                    }
                    .size(72.dp)
                    .scale(focusScale.value)
                    .border(
                        width = 1.5.dp,
                        color = Color(0xFFFBBF24).copy(alpha = focusAlpha.value),
                        shape = CircleShape
                    )
            )
        }

        // Shutter Flash Overlay
        if (flashAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha.value))
            )
        }

        // Gesture Detection Overlay (Pinch, Double-Tap, Long-Press, Tap, Swipe)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isRecording, currentZoomRatio, minZoomRatio, maxZoomRatio) {
                    var lastTapTime = 0L
                    var lastTapPos = Offset.Zero
                    val doubleTapTimeout = 320L
                    val doubleTapSlop = 90f

                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointers = event.changes

                            if (pointers.isEmpty()) continue

                            if (pointers.size == 1) {
                                val change = pointers[0]

                                if (change.pressed && change.previousPressed.not()) {
                                    // Pointer DOWN
                                    val downTime = SystemClock.uptimeMillis()
                                    val downPos = change.position

                                    val isDoubleTapCandidate =
                                        (downTime - lastTapTime < doubleTapTimeout) &&
                                                (downPos - lastTapPos).getDistance() < doubleTapSlop

                                    if (isDoubleTapCandidate) {
                                        // DOUBLE-TAP: Capture Photo!
                                        lastTapTime = 0L
                                        capturePhoto()
                                        change.consume()
                                    } else {
                                        // Wait to determine if it's tap, long-press (hold to record), or drag
                                        var isLongPressTriggered = false
                                        var pointerCurrent = change

                                        while (pointerCurrent.pressed) {
                                            val nextEvent = awaitPointerEvent()
                                            val currentPointers = nextEvent.changes

                                            if (currentPointers.size > 1) {
                                                // Transitioned to multi-touch pinch!
                                                break
                                            }

                                            if (currentPointers.isNotEmpty()) {
                                                pointerCurrent = currentPointers[0]
                                                val elapsed = SystemClock.uptimeMillis() - downTime
                                                val dist = (pointerCurrent.position - downPos).getDistance()

                                                if (!isLongPressTriggered && elapsed >= 400L && dist < 40f) {
                                                    // Long-press threshold reached -> Start recording video!
                                                    isLongPressTriggered = true
                                                    startVideoClip()
                                                }
                                                pointerCurrent.consume()
                                            }
                                        }

                                        // Pointer UP / RELEASED
                                        val totalElapsed = SystemClock.uptimeMillis() - downTime
                                        val totalDist = (pointerCurrent.position - downPos).getDistance()
                                        val deltaY = pointerCurrent.position.y - downPos.y

                                        if (isLongPressTriggered) {
                                            // Finger lifted after long-press video recording -> stop recording!
                                            stopVideoClip()
                                        } else if (totalDist > 220f && kotlin.math.abs(deltaY) > 180f && totalElapsed < 450L) {
                                            // Vertical swipe gesture -> Flip camera!
                                            flipLens()
                                        } else if (totalElapsed < 300L && totalDist < 30f) {
                                            // Single tap
                                            if (isRecording) {
                                                // If already recording, tap stops recording
                                                stopVideoClip()
                                            } else {
                                                lastTapTime = downTime
                                                lastTapPos = downPos
                                                // Trigger focus and metering
                                                triggerFocus(downPos, size.width, size.height)
                                            }
                                        }
                                    }
                                }
                            } else if (pointers.size >= 2) {
                                // Pinch-to-zoom gesture
                                var p0 = pointers[0]
                                var p1 = pointers[1]
                                var previousDistance = (p0.position - p1.position).getDistance()
                                var trackingZoom = currentZoomRatio

                                showZoomBadge = true

                                while (event.changes.size >= 2 && event.changes.any { it.pressed }) {
                                    val nextEvent = awaitPointerEvent()
                                    val currentPointers = nextEvent.changes
                                    if (currentPointers.size < 2) break

                                    p0 = currentPointers[0]
                                    p1 = currentPointers[1]
                                    val currentDistance = (p0.position - p1.position).getDistance()

                                    if (previousDistance > 0f && currentDistance > 0f) {
                                        val factor = currentDistance / previousDistance
                                        trackingZoom = (trackingZoom * factor).coerceIn(minZoomRatio, maxZoomRatio)
                                        cameraManager.setZoomRatio(trackingZoom)
                                        currentZoomRatio = trackingZoom
                                        showZoomBadge = true
                                        previousDistance = currentDistance
                                    }
                                    p0.consume()
                                    p1.consume()
                                }
                            }
                        }
                    }
                }
        )

        // Top UI: Recording HUD or Gesture Hint (unobtrusive, floating)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Video Recording Status Indicator
            AnimatedVisibility(
                visible = isRecording,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF1744).copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("recording_indicator")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFFFF1744).copy(alpha = recordingPulseAlpha), CircleShape)
                        )
                        Text(
                            text = "REC ${formatDuration(recordingDurationSeconds)}",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }
            }

            // Gesture Guide Banner (fades out automatically)
            AnimatedVisibility(
                visible = showGestureGuide && !isRecording,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            showGestureGuide = false
                        }
                        .testTag("gesture_guide_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Gestures",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Double-tap: iPhone 17 Pro AI Photo • Hold: Record • Pinch: Zoom • Swipe: Flip",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }

        // Floating Zoom Level Badge (appears when zooming, disappears after inactivity)
        AnimatedVisibility(
            visible = showZoomBadge && !isRecording,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Zoom Ratio",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = String.format(Locale.US, "%.1f×", currentZoomRatio),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }

        // Floating Action Feedback Pill ("Photo saved to Gallery" / "Video saved")
        AnimatedVisibility(
            visible = notificationMessage != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        ) {
            notificationMessage?.let { msg ->
                Surface(
                    color = Color(0xFF1E293B).copy(alpha = 0.94f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (msg.contains("✨")) Color(0xFFF59E0B).copy(alpha = 0.6f) else Color(0xFF38BDF8).copy(alpha = 0.4f)
                    ),
                    shadowElevation = 8.dp,
                    modifier = Modifier.testTag("save_feedback_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val iconVector = when {
                            isVideoNotification -> Icons.Default.Videocam
                            msg.contains("✨") || msg.contains("iPhone") -> Icons.Default.AutoAwesome
                            else -> Icons.Default.Check
                        }
                        val iconTint = when {
                            isVideoNotification -> Color(0xFFFF1744)
                            msg.contains("✨") || msg.contains("iPhone") -> Color(0xFFFBBF24)
                            else -> Color(0xFF34D399)
                        }

                        Icon(
                            imageVector = iconVector,
                            contentDescription = "Status",
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }

        // Subtle, low-opacity Info & Flip buttons at top corners for convenience
        // (styled with 25% opacity so the view remains pure camera, but accessible if needed)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { showGestureGuide = !showGestureGuide },
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                    .testTag("toggle_guide_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Show Gesture Guide",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { flipLens() },
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.25f), CircleShape)
                    .testTag("flip_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = "Flip Camera",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
