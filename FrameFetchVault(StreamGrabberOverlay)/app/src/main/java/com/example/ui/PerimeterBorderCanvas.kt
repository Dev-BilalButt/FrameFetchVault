package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BorderState
import com.example.data.DetectedVideoStream
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SignalEmerald

/**
 * Wraps the entire app viewport inside an active glowing viewfinder border
 * that visually reacts to video stream detection and provides an instant
 * floating download pill along the border edge.
 */
@Composable
fun PerimeterBorderContainer(
    borderState: BorderState,
    strokeDp: Float,
    showCornerBrackets: Boolean,
    detectedStreams: List<DetectedVideoStream>,
    activeDownloadCount: Int,
    onInspectStreamsClick: () -> Unit,
    onOpenVaultClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "border_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val primaryBorderColor by animateColorAsState(
        targetValue = when (borderState) {
            BorderState.VIDEO_DETECTED -> SignalEmerald
            BorderState.DOWNLOADING -> ElectricCyan
            BorderState.SCANNING -> AmberPulse
            BorderState.IDLE -> ElectricCyan.copy(alpha = 0.65f)
        },
        animationSpec = tween(350),
        label = "border_color"
    )

    val secondaryAccentColor by animateColorAsState(
        targetValue = when (borderState) {
            BorderState.VIDEO_DETECTED -> ElectricCyan
            BorderState.DOWNLOADING -> SignalEmerald
            BorderState.SCANNING -> ElectricCyan
            BorderState.IDLE -> SignalEmerald.copy(alpha = 0.7f)
        },
        animationSpec = tween(350),
        label = "accent_color"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Inner workspace content framed inside the border padding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            content()
        }

        // Full-bleed Perimeter Border Canvas (Pass-through touches)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("perimeter_border_canvas")
        ) {
            val strokePx = strokeDp.dp.toPx()
            val inset = strokePx / 2f + 2.dp.toPx()
            val rectSize = Size(size.width - inset * 2, size.height - inset * 2)
            val cornerRad = 22.dp.toPx()

            // Outer ambient glow
            drawRoundRect(
                color = primaryBorderColor.copy(alpha = glowAlpha * 0.35f),
                topLeft = Offset(inset, inset),
                size = rectSize,
                cornerRadius = CornerRadius(cornerRad, cornerRad),
                style = Stroke(width = strokePx * 2.6f)
            )

            // Main crisp perimeter frame
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        primaryBorderColor,
                        secondaryAccentColor,
                        primaryBorderColor
                    )
                ),
                topLeft = Offset(inset, inset),
                size = rectSize,
                cornerRadius = CornerRadius(cornerRad, cornerRad),
                style = Stroke(width = strokePx)
            )

            // Tactical Corner Viewfinder Brackets
            if (showCornerBrackets) {
                val bracketLen = 26.dp.toPx()
                val bInset = inset + 4.dp.toPx()
                val bracketStroke = (strokeDp + 2f).dp.toPx()

                // Top-Left
                drawLine(
                    color = secondaryAccentColor,
                    start = Offset(bInset, bInset + bracketLen),
                    end = Offset(bInset, bInset),
                    strokeWidth = bracketStroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = secondaryAccentColor,
                    start = Offset(bInset, bInset),
                    end = Offset(bInset + bracketLen, bInset),
                    strokeWidth = bracketStroke,
                    cap = StrokeCap.Round
                )

                // Top-Right
                drawLine(
                    color = secondaryAccentColor,
                    start = Offset(size.width - bInset - bracketLen, bInset),
                    end = Offset(size.width - bInset, bInset),
                    strokeWidth = bracketStroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = secondaryAccentColor,
                    start = Offset(size.width - bInset, bInset),
                    end = Offset(size.width - bInset, bInset + bracketLen),
                    strokeWidth = bracketStroke,
                    cap = StrokeCap.Round
                )

                // Bottom-Left
                drawLine(
                    color = secondaryAccentColor,
                    start = Offset(bInset, size.height - bInset - bracketLen),
                    end = Offset(bInset, size.height - bInset),
                    strokeWidth = bracketStroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = secondaryAccentColor,
                    start = Offset(bInset, size.height - bInset),
                    end = Offset(bInset + bracketLen, size.height - bInset),
                    strokeWidth = bracketStroke,
                    cap = StrokeCap.Round
                )

                // Bottom-Right
                drawLine(
                    color = secondaryAccentColor,
                    start = Offset(size.width - bInset - bracketLen, size.height - bInset),
                    end = Offset(size.width - bInset, size.height - bInset),
                    strokeWidth = bracketStroke,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = secondaryAccentColor,
                    start = Offset(size.width - bInset, size.height - bInset - bracketLen),
                    end = Offset(size.width - bInset, size.height - bInset),
                    strokeWidth = bracketStroke,
                    cap = StrokeCap.Round
                )
            }
        }

        // Floating Border Extraction Action Pill (docked above bottom navigation inside the border)
        AnimatedVisibility(
            visible = detectedStreams.isNotEmpty() || activeDownloadCount > 0,
            enter = slideInVertically(animationSpec = spring()) { it } + fadeIn(),
            exit = slideOutVertically(animationSpec = spring()) { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 92.dp)
        ) {
            val topStream = detectedStreams.firstOrNull()
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = ObsidianBg.copy(alpha = 0.95f),
                tonalElevation = 10.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(listOf(SignalEmerald, ElectricCyan)),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .clickable {
                        if (detectedStreams.isNotEmpty()) {
                            onInspectStreamsClick()
                        } else {
                            onOpenVaultClick()
                        }
                    }
                    .testTag("floating_extract_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(SignalEmerald.copy(alpha = 0.18f))
                    ) {
                        Icon(
                            imageVector = if (detectedStreams.isNotEmpty()) Icons.Default.Download else Icons.Default.Radar,
                            contentDescription = "Extract Detected Video",
                            tint = SignalEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.width(156.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (detectedStreams.isNotEmpty()) {
                                    "${detectedStreams.size} VIDEO STREAM${if (detectedStreams.size > 1) "S" else ""}"
                                } else {
                                    "$activeDownloadCount ACTIVE IN VAULT"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = SignalEmerald,
                                fontWeight = FontWeight.Bold
                            )
                            if (topStream != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.HighQuality,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = topStream?.let { "${it.resolutionLabel} • ${it.format} • Tap to Download" }
                                ?: "Tap to view private vault progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
