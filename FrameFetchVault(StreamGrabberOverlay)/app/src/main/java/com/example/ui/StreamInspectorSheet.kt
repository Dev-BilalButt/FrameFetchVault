package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DetectedVideoStream
import com.example.data.QualityVariant
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.SignalEmerald
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamInspectorSheet(
    detectedStreams: List<DetectedVideoStream>,
    selectedStream: DetectedVideoStream?,
    onSelectStream: (DetectedVideoStream) -> Unit,
    onStartDownload: (DetectedVideoStream, QualityVariant, String) -> Unit,
    onDismiss: () -> Unit
) {
    val activeStream = selectedStream ?: detectedStreams.firstOrNull() ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var customTitle by remember(activeStream.id) { mutableStateOf(activeStream.title) }
    var selectedVariant by remember(activeStream.id) {
        mutableStateOf(activeStream.qualityVariants.firstOrNull())
    }

    LaunchedEffect(activeStream.id) {
        customTitle = activeStream.title
        selectedVariant = activeStream.qualityVariants.firstOrNull()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CarbonSurface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.testTag("stream_inspector_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SignalEmerald.copy(alpha = 0.16f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.HighQuality,
                            contentDescription = null,
                            tint = SignalEmerald
                        )
                    }
                    Column {
                        Text(
                            text = "Stream Resolution & Format Inspector",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Source: ${activeStream.sourceDomain} • Isolated Vault Ready",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Inspector",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // If multiple distinct videos were detected on the page, allow switching between them
            if (detectedStreams.size > 1) {
                Text(
                    text = "DETECTED VIDEOS ON PAGE (${detectedStreams.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(detectedStreams) { streamItem ->
                        val isChosen = streamItem.id == activeStream.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isChosen) ElectricCyan.copy(alpha = 0.18f) else GlassSurface,
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (isChosen) ElectricCyan else ElevatedBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectStream(streamItem) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = if (isChosen) ElectricCyan else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = streamItem.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Editable Vault File Name
            OutlinedTextField(
                value = customTitle,
                onValueChange = { customTitle = it },
                label = { Text("Vault Video Title") },
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
                    .testTag("inspector_title_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "SELECT RESOLUTION & CONTAINER FORMAT",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quality Variant Cards
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(activeStream.qualityVariants) { variant ->
                    val isSelected = selectedVariant?.id == variant.id
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) SignalEmerald.copy(alpha = 0.14f) else GlassSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) SignalEmerald else ElevatedBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedVariant = variant }
                            .testTag("quality_variant_${variant.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.RadioButtonUnchecked
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) SignalEmerald else TextSecondary
                                )

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = variant.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = ElectricCyan.copy(alpha = 0.16f)
                                        ) {
                                            Text(
                                                text = variant.resolution,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = ElectricCyan,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${variant.mimeType} • ${variant.bitrateLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Probed File Size Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CarbonSurface
                            ) {
                                Text(
                                    text = formatBytesLabel(variant.sizeBytes),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSelected) SignalEmerald else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Separate Storage Target Info Bar
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = GlassSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Destination: Isolated Private App Vault (.nomedia)",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                        Text(
                            text = "Stored in separate app-dedicated mobile storage hidden from main gallery",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = ElevatedBorder)

            Spacer(modifier = Modifier.height(14.dp))

            val chosen = selectedVariant ?: activeStream.qualityVariants.firstOrNull()
            Button(
                onClick = {
                    if (chosen != null) {
                        onStartDownload(activeStream, chosen, customTitle)
                    }
                },
                enabled = chosen != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SignalEmerald,
                    contentColor = Color(0xFF031B0E)
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_vault_download_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (chosen != null) {
                        "Download ${chosen.label} to Separate Vault (${formatBytesLabel(chosen.sizeBytes)})"
                    } else {
                        "Download to Separate Vault"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun formatBytesLabel(bytes: Long): String {
    if (bytes <= 0L) return "Live Stream"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        else -> String.format(Locale.US, "%.0f KB", kb)
    }
}
