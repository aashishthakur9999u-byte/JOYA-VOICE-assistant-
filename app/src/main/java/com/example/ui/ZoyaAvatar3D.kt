package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.live.ZoyaState
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Cyberpunk 2050 Palette
private val NeonCyan = Color(0xFF00F0FF)
private val NeonMagenta = Color(0xFFFF007F)
private val NeonPurple = Color(0xFF9D00FF)
private val CyberGold = Color(0xFFFFB800)
private val MatrixGreen = Color(0xFF00FF66)
private val DeepSpace = Color(0xFF050814)

@Composable
fun ZoyaAvatar3D(
    zoyaState: ZoyaState,
    isOfflineMode: Boolean,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()

    // 3D Gyro / Drag Rotations
    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }

    // Infinite Transitions for 2050 Hologram Physics
    val infiniteTransition = rememberInfiniteTransition(label = "hologram_physics")

    // Organic 3D Floating Oscillations
    val floatY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    val ambientPitch by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambientPitch"
    )

    // Holographic Quantum Orbit Rings Rotation
    val ringRotationOuter by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing)
        ),
        label = "ring_outer"
    )

    val ringRotationInner by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing)
        ),
        label = "ring_inner"
    )

    // Fast Quantum Warp Ring when Thinking
    val thinkingWarp by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "thinking_warp"
    )

    // Holographic Scanline Y Sweep
    val scanlineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "scanline"
    )

    // Audio-Reactive Pulse Scaling
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = when (zoyaState) {
            ZoyaState.SPEAKING -> 1.0f
            ZoyaState.LISTENING -> 1.02f
            ZoyaState.THINKING -> 0.98f
            else -> 1.0f
        },
        targetValue = when (zoyaState) {
            ZoyaState.SPEAKING -> 1.12f
            ZoyaState.LISTENING -> 1.06f
            ZoyaState.THINKING -> 1.04f
            else -> 1.02f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (zoyaState) {
                    ZoyaState.SPEAKING -> 350
                    ZoyaState.LISTENING -> 800
                    ZoyaState.THINKING -> 500
                    else -> 1600
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Aura Glow Color based on state and mode
    val auraColor = when {
        isOfflineMode -> CyberGold
        zoyaState == ZoyaState.SPEAKING -> NeonMagenta
        zoyaState == ZoyaState.LISTENING -> NeonCyan
        zoyaState == ZoyaState.THINKING -> NeonPurple
        else -> NeonCyan.copy(alpha = 0.8f)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 2050 Cyber HUD Status Header
        CyberHudStatusHeader(
            zoyaState = zoyaState,
            isOfflineMode = isOfflineMode
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3D / 4D Hologram Viewport with interactive touch tilt
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset { IntOffset(0, floatY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val newRotY = (rotY.value + dragAmount.x * 0.4f).coerceIn(-35f, 35f)
                                val newRotX = (rotX.value - dragAmount.y * 0.4f).coerceIn(-30f, 30f)
                                rotY.snapTo(newRotY)
                                rotX.snapTo(newRotX)
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                rotY.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 300f))
                            }
                            scope.launch {
                                rotX.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 300f))
                            }
                        }
                    )
                }
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            // Layer 1: Hologram Background Glow
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = this.center
                val radius = size.minDimension / 2f

                // Radial ambient cyber glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            auraColor.copy(alpha = 0.45f * pulseScale),
                            NeonPurple.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius * 1.35f
                    ),
                    radius = radius * 1.35f
                )
            }

            // Layer 2: 3D Outer Quantum Rings (tilted in 3D space)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationX = 65f + (rotX.value * 0.3f)
                        rotationZ = ringRotationOuter
                        cameraDistance = 14f * density
                    }
            ) {
                val strokeW = 2.5f.dp.toPx()
                val radius = (size.minDimension / 2f) * 0.96f

                // Dashed outer ring
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.7f),
                    radius = radius,
                    style = Stroke(
                        width = strokeW,
                        cap = StrokeCap.Round
                    )
                )

                // 4 Orbiting Quantum Nodes
                for (i in 0 until 4) {
                    val angle = Math.toRadians((i * 90.0))
                    val nx = center.x + radius * cos(angle).toFloat()
                    val ny = center.y + radius * sin(angle).toFloat()
                    drawCircle(
                        color = if (i % 2 == 0) NeonCyan else NeonMagenta,
                        radius = 4.5f.dp.toPx(),
                        center = Offset(nx, ny)
                    )
                }
            }

            // Layer 3: 3D Middle Counter-Rotating Ring
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = 45f + (rotY.value * 0.3f)
                        rotationZ = ringRotationInner
                        cameraDistance = 14f * density
                    }
            ) {
                val strokeW = 1.8f.dp.toPx()
                val radius = (size.minDimension / 2f) * 0.82f

                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(NeonMagenta, NeonPurple, NeonCyan, NeonMagenta)
                    ),
                    radius = radius,
                    style = Stroke(
                        width = strokeW,
                        cap = StrokeCap.Round
                    )
                )
            }

            // Layer 4: High-Speed Thinking Quantum Ring (Activates when thinking)
            if (zoyaState == ZoyaState.THINKING) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationX = 35f
                            rotationY = 35f
                            rotationZ = thinkingWarp
                        }
                ) {
                    drawCircle(
                        color = NeonPurple,
                        radius = (size.minDimension / 2f) * 0.88f,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }

            // Layer 5: The 2050 Anime Holographic AI Avatar Core (with 3D perspective graphicsLayer)
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        rotationX = rotX.value + ambientPitch
                        rotationY = rotY.value
                        cameraDistance = 14f * density
                        shadowElevation = 24.dp.toPx()
                    }
                    .clip(CircleShape)
                    .border(
                        width = 3.dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                NeonCyan,
                                NeonMagenta,
                                NeonPurple,
                                auraColor,
                                NeonCyan
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // High Quality 2050 Anime Girl Avatar Asset
                Image(
                    painter = painterResource(id = R.drawable.img_zoya_anime_avatar),
                    contentDescription = "Zoya 2050 Anime Holographic AI Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()

                            // Holographic Scanline Overlay passing vertically
                            val lineY = size.height * scanlineY
                            drawLine(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        NeonCyan.copy(alpha = 0.5f),
                                        Color.Transparent
                                    ),
                                    startY = lineY - 15f,
                                    endY = lineY + 15f
                                ),
                                start = Offset(0f, lineY),
                                end = Offset(size.width, lineY),
                                strokeWidth = 3f.dp.toPx()
                            )

                            // Subtle chromatic tint when speaking or thinking
                            if (zoyaState == ZoyaState.SPEAKING) {
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            NeonMagenta.copy(alpha = 0.18f)
                                        )
                                    )
                                )
                            }
                        }
                )

                // Voice / Lip-Sync Radial Holographic Wave when speaking
                if (zoyaState == ZoyaState.SPEAKING) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = NeonMagenta.copy(alpha = 0.35f),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            // Layer 6: Hologram Corner Cyber Reticles
            CyberReticlesOverlay(sizeDp = 240)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Cyber Equalizer Visualizer & Tap Prompt
        CyberEqualizerVisualizer(
            zoyaState = zoyaState,
            isOfflineMode = isOfflineMode
        )
    }
}

@Composable
private fun CyberHudStatusHeader(
    zoyaState: ZoyaState,
    isOfflineMode: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .background(
                DeepSpace.copy(alpha = 0.85f),
                RoundedCornerShape(20.dp)
            )
            .border(
                1.dp,
                if (isOfflineMode) CyberGold.copy(alpha = 0.5f) else NeonCyan.copy(alpha = 0.4f),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Status Pulsing Dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    if (isOfflineMode) CyberGold else if (zoyaState != ZoyaState.IDLE) MatrixGreen else NeonCyan,
                    CircleShape
                )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = if (isOfflineMode) {
                "OFFLINE NEURAL CORE 2050"
            } else {
                "ONLINE QUANTUM MATRIX • GEMINI 2.5"
            },
            color = if (isOfflineMode) CyberGold else NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun CyberReticlesOverlay(sizeDp: Int) {
    Canvas(modifier = Modifier.size(sizeDp.dp)) {
        val stroke = 1.5f.dp.toPx()
        val tickLen = 14f.dp.toPx()
        val color = NeonCyan.copy(alpha = 0.4f)

        // Top-Left Reticle
        drawLine(color, Offset(0f, 0f), Offset(tickLen, 0f), stroke)
        drawLine(color, Offset(0f, 0f), Offset(0f, tickLen), stroke)

        // Top-Right Reticle
        drawLine(color, Offset(size.width, 0f), Offset(size.width - tickLen, 0f), stroke)
        drawLine(color, Offset(size.width, 0f), Offset(size.width, tickLen), stroke)

        // Bottom-Left Reticle
        drawLine(color, Offset(0f, size.height), Offset(tickLen, size.height), stroke)
        drawLine(color, Offset(0f, size.height), Offset(0f, size.height - tickLen), stroke)

        // Bottom-Right Reticle
        drawLine(color, Offset(size.width, size.height), Offset(size.width - tickLen, size.height), stroke)
        drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - tickLen), stroke)
    }
}

@Composable
private fun CyberEqualizerVisualizer(
    zoyaState: ZoyaState,
    isOfflineMode: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer_bars")

    // Dynamic bar heights
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = if (zoyaState != ZoyaState.IDLE) 26f else 8f,
        animationSpec = infiniteRepeatable(tween(200, easing = LinearEasing), RepeatMode.Reverse),
        label = "b1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 10f, targetValue = if (zoyaState != ZoyaState.IDLE) 36f else 10f,
        animationSpec = infiniteRepeatable(tween(180, easing = LinearEasing), RepeatMode.Reverse),
        label = "b2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = if (zoyaState != ZoyaState.IDLE) 30f else 6f,
        animationSpec = infiniteRepeatable(tween(240, easing = LinearEasing), RepeatMode.Reverse),
        label = "b3"
    )
    val bar4 by infiniteTransition.animateFloat(
        initialValue = 12f, targetValue = if (zoyaState != ZoyaState.IDLE) 40f else 12f,
        animationSpec = infiniteRepeatable(tween(160, easing = LinearEasing), RepeatMode.Reverse),
        label = "b4"
    )
    val bar5 by infiniteTransition.animateFloat(
        initialValue = 8f, targetValue = if (zoyaState != ZoyaState.IDLE) 28f else 7f,
        animationSpec = infiniteRepeatable(tween(220, easing = LinearEasing), RepeatMode.Reverse),
        label = "b5"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Equalizer Bars Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(42.dp)
        ) {
            val barColor = when {
                isOfflineMode -> CyberGold
                zoyaState == ZoyaState.SPEAKING -> NeonMagenta
                zoyaState == ZoyaState.LISTENING -> NeonCyan
                zoyaState == ZoyaState.THINKING -> NeonPurple
                else -> NeonCyan.copy(alpha = 0.5f)
            }

            listOf(bar1, bar2, bar3, bar4, bar5, bar3, bar2, bar1).forEach { heightVal ->
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(heightVal.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(barColor, barColor.copy(alpha = 0.3f))
                            ),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // State Action Text
        Text(
            text = when (zoyaState) {
                ZoyaState.LISTENING -> if (isOfflineMode) "🎙️ Offline Listening... Speak Command" else "🎙️ Quantum Matrix Listening..."
                ZoyaState.SPEAKING -> if (isOfflineMode) "🔊 Zoya 2050 Speaking (Offline)..." else "🔊 Zoya Responding (Neural)..."
                ZoyaState.THINKING -> "⚡ Neural Matrix Processing..."
                ZoyaState.IDLE -> "Tap Avatar to Activate / Hold to Rotate 3D"
            },
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}
