package com.example.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SignalEmerald
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun OverlayControlScreen(
    uiState: FrameFetchUiState,
    onRequestOverlayPermission: () -> Unit,
    onToggleOverlayService: (Boolean) -> Unit,
    onUpdateBorderStyle: (Float, Boolean) -> Unit,
    onExternalPasteUrlChange: (String) -> Unit,
    onExtractExternalUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // 1. Tactical Hero Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = CarbonSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ElevatedBorder, RoundedCornerShape(18.dp))
        ) {
            Column {
                Image(
                    painter = painterResource(id = R.drawable.ic_vault_hero_illustration),
                    contentDescription = "System Border Overlay & Vault Illustration",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                )
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "System-Wide Border Overlay Engine",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Draws a pass-through glowing frame around your entire phone screen over any app you open, paired with a floating 'Sniff / Download' pill that captures shared or copied video links straight into your isolated Vault.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // 2. "Display Over Other Apps" Permission & Service Launcher Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CarbonSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (uiState.hasOverlayPermission) SignalEmerald.copy(alpha = 0.6f) else AmberPulse,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.hasOverlayPermission) {
                                        SignalEmerald.copy(alpha = 0.16f)
                                    } else {
                                        AmberPulse.copy(alpha = 0.16f)
                                    }
                                )
                        ) {
                            Icon(
                                imageVector = if (uiState.hasOverlayPermission) {
                                    Icons.Default.CheckCircle
                                } else {
                                    Icons.Default.WarningAmber
                                },
                                contentDescription = null,
                                tint = if (uiState.hasOverlayPermission) SignalEmerald else AmberPulse
                            )
                        }
                        Column {
                            Text(
                                text = "Overlay Permission (Display Over Apps)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.hasOverlayPermission) {
                                    "Granted • Ready to project border over your phone"
                                } else {
                                    "Required to keep the border & download pill visible over other apps"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (uiState.hasOverlayPermission) SignalEmerald else AmberPulse
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!uiState.hasOverlayPermission) {
                    Button(
                        onClick = onRequestOverlayPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberPulse,
                            contentColor = Color(0xFF1F1400)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("grant_overlay_permission_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Allow 'Display Over Other Apps' Permission",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Toggle System Border Service
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GlassSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (uiState.isOverlayServiceRunning) {
                                        "System Border Overlay is ACTIVE"
                                    } else {
                                        "Project Border Over Normal Mobile Screen"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (uiState.isOverlayServiceRunning) SignalEmerald else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Touch pass-through enabled so you can use all apps normally inside the border",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = uiState.isOverlayServiceRunning,
                                onCheckedChange = { enabled -> onToggleOverlayService(enabled) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF042111),
                                    checkedTrackColor = SignalEmerald
                                ),
                                modifier = Modifier.testTag("toggle_system_overlay_switch")
                            )
                        }
                    }
                }
            }
        }

        // 3. External App & Clipboard Video Link Sniffer
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CarbonSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ElevatedBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = null,
                        tint = SignalEmerald
                    )
                    Text(
                        text = "External App Link & Clipboard Video Extractor",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Copied or shared a video link from another app inside the border? Paste or capture it below to probe resolutions and download to your Private Vault.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.externalPasteUrl,
                    onValueChange = onExternalPasteUrlChange,
                    placeholder = {
                        Text(
                            text = "https://example.com/video.mp4 or webpage link…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = ElectricCyan
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = ElevatedBorder,
                        focusedContainerColor = GlassSurface,
                        unfocusedContainerColor = GlassSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("external_url_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipUrl = readClipboardText(context)
                            if (clipUrl.isNotBlank()) {
                                onExternalPasteUrlChange(clipUrl)
                                onExtractExternalUrl(clipUrl)
                            } else {
                                // Populate a sample HD stream URL if clipboard is empty so user can test 1-tap extraction
                                val demoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
                                onExternalPasteUrlChange(demoUrl)
                                onExtractExternalUrl(demoUrl)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("paste_clipboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Paste & Sniff",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                    }

                    Button(
                        onClick = { onExtractExternalUrl(uiState.externalPasteUrl) },
                        enabled = uiState.externalPasteUrl.isNotBlank() && !uiState.isProbingExternalUrl,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SignalEmerald,
                            contentColor = Color(0xFF041E10)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("probe_external_url_button")
                    ) {
                        if (uiState.isProbingExternalUrl) {
                            CircularProgressIndicator(
                                color = Color(0xFF041E10),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = "Extract Video",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Perimeter Border Customization Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CarbonSurface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ElevatedBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = ElectricCyan
                    )
                    Text(
                        text = "Border Frame Geometry & Viewfinder HUD",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Perimeter Border Thickness",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${uiState.borderStrokeDp.roundToInt()} dp",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                }

                Slider(
                    value = uiState.borderStrokeDp,
                    onValueChange = { newStroke ->
                        onUpdateBorderStyle(newStroke, uiState.showCornerBrackets)
                    },
                    valueRange = 2f..10f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricCyan,
                        activeTrackColor = ElectricCyan
                    ),
                    modifier = Modifier.testTag("border_thickness_slider")
                )

                HorizontalDivider(color = ElevatedBorder, modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Corner Viewfinder Brackets",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Highlights screen corners when a video stream locks inside the border",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = uiState.showCornerBrackets,
                        onCheckedChange = { show ->
                            onUpdateBorderStyle(uiState.borderStrokeDp, show)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF001F26),
                            checkedTrackColor = ElectricCyan
                        ),
                        modifier = Modifier.testTag("corner_brackets_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun readClipboardText(context: Context): String {
    return runCatching {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim().orEmpty()
    }.getOrDefault("")
}
