package com.example.ui

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.VaultVideoEntity
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SignalEmerald
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrivateVaultScreen(
    uiState: FrameFetchUiState,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onPlayVaultVideo: (VaultVideoEntity) -> Unit,
    onClosePlayer: () -> Unit,
    onDeleteVaultVideo: (VaultVideoEntity) -> Unit,
    onExportVaultVideo: (VaultVideoEntity) -> Unit,
    onGoToBrowserClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Handle system back button when built-in vault video player is open
    if (uiState.activePlayerVideo != null) {
        BackHandler {
            onClosePlayer()
        }
    }

    val filteredVideos = remember(uiState.vaultVideos, uiState.vaultSearchQuery, uiState.vaultFilter) {
        uiState.vaultVideos.filter { video ->
            val matchesQuery = uiState.vaultSearchQuery.isBlank() ||
                video.title.contains(uiState.vaultSearchQuery, ignoreCase = true) ||
                video.sourceDomain.contains(uiState.vaultSearchQuery, ignoreCase = true) ||
                video.format.contains(uiState.vaultSearchQuery, ignoreCase = true)
            val matchesStatus = when (uiState.vaultFilter) {
                "COMPLETED" -> video.status == "COMPLETED"
                "DOWNLOADING" -> video.status == "DOWNLOADING"
                else -> true
            }
            matchesQuery && matchesStatus
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .testTag("private_vault_list")
    ) {
        // 1. Separate Storage Partition Telemetry Card
        item {
            val stats = uiState.storageStats
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = CarbonSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ElevatedBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElectricCyan.copy(alpha = 0.16f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = ElectricCyan
                                )
                            }
                            Column {
                                Text(
                                    text = "Separate Mobile Storage Vault",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Isolated Partition • Hidden from Phone Gallery",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SignalEmerald
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SignalEmerald.copy(alpha = 0.15f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = SignalEmerald,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = ".nomedia ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SignalEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Storage Metrics Row
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "VAULT USED",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = formatBytesLabel(stats.vaultUsedBytes),
                                style = MaterialTheme.typography.titleLarge,
                                color = ElectricCyan
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "ISOLATED FILES",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "${stats.vaultFileCount} videos",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "MOBILE FREE SPACE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = formatBytesLabel(stats.deviceFreeBytes),
                                style = MaterialTheme.typography.titleLarge,
                                color = SignalEmerald
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val usedRatio = ((stats.deviceTotalBytes - stats.deviceFreeBytes).toFloat() /
                        stats.deviceTotalBytes.coerceAtLeast(1L).toFloat()).coerceIn(0.02f, 1f)
                    LinearProgressIndicator(
                        progress = { usedRatio },
                        color = ElectricCyan,
                        trackColor = GlassSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Vault Partition: ${stats.vaultDirectoryPath}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 2. Built-In Private Vault Video Player (shown when a video is active)
        val playingVideo = uiState.activePlayerVideo
        if (playingVideo != null) {
            item {
                BuiltInVaultVideoPlayerCard(
                    video = playingVideo,
                    onClose = onClosePlayer,
                    onExport = { onExportVaultVideo(playingVideo) },
                    onDelete = { onDeleteVaultVideo(playingVideo) }
                )
            }
        }

        // 3. Search & Filter Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.vaultSearchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = "Search isolated vault videos by title or format…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Vault",
                            tint = ElectricCyan
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = ElevatedBorder,
                        focusedContainerColor = CarbonSurface,
                        unfocusedContainerColor = CarbonSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vault_search_input")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "ALL" to "All Vault (${uiState.vaultVideos.size})",
                        "COMPLETED" to "Ready to Play (${uiState.vaultVideos.count { it.status == "COMPLETED" }})",
                        "DOWNLOADING" to "Active Transfers (${uiState.vaultVideos.count { it.status == "DOWNLOADING" }})"
                    ).forEach { (key, label) ->
                        val selected = uiState.vaultFilter == key
                        FilterChip(
                            selected = selected,
                            onClick = { onFilterChange(key) },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.18f),
                                selectedLabelColor = ElectricCyan
                            ),
                            modifier = Modifier.testTag("vault_filter_$key")
                        )
                    }
                }
            }
        }

        // 4. Empty State or Vault Video List
        if (filteredVideos.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CarbonSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ElevatedBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_vault_hero_illustration),
                            contentDescription = "Empty Private Vault",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Videos in Separate Storage Yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Open any video site inside the Framed Browser or tap the glowing Download pill on the border to extract and save videos directly into this isolated vault.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onGoToBrowserClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = Color(0xFF001F26)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("empty_vault_open_browser_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Open Framed Browser & Extract Video",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            items(filteredVideos, key = { it.id }) { video ->
                VaultVideoItemCard(
                    video = video,
                    isCurrentlyPlaying = uiState.activePlayerVideo?.id == video.id,
                    onPlayClick = { onPlayVaultVideo(video) },
                    onExportClick = { onExportVaultVideo(video) },
                    onDeleteClick = { onDeleteVaultVideo(video) }
                )
            }
        }
    }
}

@Composable
private fun BuiltInVaultVideoPlayerCard(
    video: VaultVideoEntity,
    onClose: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf((video.durationSeconds * 1000).coerceAtLeast(1000)) }

    LaunchedEffect(video.id, isPlaying) {
        while (true) {
            val vv = videoViewRef
            if (vv != null) {
                runCatching {
                    if (vv.isPlaying) {
                        currentPosMs = vv.currentPosition
                        val dur = vv.duration
                        if (dur > 0) durationMs = dur
                    }
                    isPlaying = vv.isPlaying
                }
            }
            delay(250L)
        }
    }

    DisposableEffect(video.id) {
        onDispose {
            runCatching { videoViewRef?.stopPlayback() }
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CarbonSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, SignalEmerald, RoundedCornerShape(18.dp))
            .testTag("built_in_vault_player")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Player Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassSurface)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "BUILT-IN VAULT PLAYER • ${video.resolution} ${video.format}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SignalEmerald
                    )
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("close_vault_player_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Vault Player",
                        tint = TextSecondary
                    )
                }
            }

            // Video Surface
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.Black)
            ) {
                val localFile = remember(video.localFilePath) { File(video.localFilePath) }
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            val controller = MediaController(ctx)
                            controller.setAnchorView(this)
                            setMediaController(controller)
                            setOnPreparedListener { mp ->
                                durationMs = mp.duration.coerceAtLeast(1000)
                                mp.isLooping = true
                                start()
                                isPlaying = true
                            }
                            if (localFile.exists()) {
                                setVideoPath(localFile.absolutePath)
                            } else {
                                setVideoURI(Uri.parse(video.streamUrl))
                            }
                            videoViewRef = this
                        }
                    },
                    update = { vv ->
                        videoViewRef = vv
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Custom Tactical Transport Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Slider(
                    value = currentPosMs.toFloat().coerceIn(0f, durationMs.toFloat()),
                    onValueChange = { targetMs ->
                        currentPosMs = targetMs.toInt()
                        runCatching { videoViewRef?.seekTo(targetMs.toInt()) }
                    },
                    valueRange = 0f..durationMs.toFloat().coerceAtLeast(1000f),
                    colors = SliderDefaults.colors(
                        thumbColor = SignalEmerald,
                        activeTrackColor = SignalEmerald
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${formatDurationMs(currentPosMs)} / ${formatDurationMs(durationMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val next = (currentPosMs - 10_000).coerceAtLeast(0)
                                currentPosMs = next
                                runCatching { videoViewRef?.seekTo(next) }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind 10 seconds",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = {
                                val vv = videoViewRef ?: return@IconButton
                                if (vv.isPlaying) {
                                    vv.pause()
                                    isPlaying = false
                                } else {
                                    vv.start()
                                    isPlaying = true
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SignalEmerald)
                                .testTag("player_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color(0xFF031B0E)
                            )
                        }

                        IconButton(
                            onClick = {
                                val next = (currentPosMs + 10_000).coerceAtMost(durationMs)
                                currentPosMs = next
                                runCatching { videoViewRef?.seekTo(next) }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Forward 10 seconds",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onExport) {
                            Icon(
                                imageVector = Icons.Default.IosShare,
                                contentDescription = "Export Copy to Downloads",
                                tint = ElectricCyan
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete from Vault",
                                tint = CrimsonAlert
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultVideoItemCard(
    video: VaultVideoEntity,
    isCurrentlyPlaying: Boolean,
    onPlayClick: () -> Unit,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isDownloading = video.status == "DOWNLOADING"
    val isFailed = video.status == "FAILED"
    val progressFraction = if (video.totalBytes > 0) {
        (video.downloadedBytes.toFloat() / video.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0.5f
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CarbonSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCurrentlyPlaying) 1.5.dp else 1.dp,
                color = when {
                    isCurrentlyPlaying -> SignalEmerald
                    isDownloading -> ElectricCyan
                    isFailed -> CrimsonAlert
                    else -> ElevatedBorder
                },
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isDownloading) { onPlayClick() }
            .testTag("vault_video_card_${video.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isDownloading -> ElectricCyan.copy(alpha = 0.16f)
                                    isFailed -> CrimsonAlert.copy(alpha = 0.16f)
                                    else -> SignalEmerald.copy(alpha = 0.16f)
                                }
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Video",
                            tint = when {
                                isDownloading -> ElectricCyan
                                isFailed -> CrimsonAlert
                                else -> SignalEmerald
                            },
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ElectricCyan.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = video.resolution,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ElectricCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SignalEmerald.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = video.format,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SignalEmerald,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "• ${formatBytesLabel(video.totalBytes.coerceAtLeast(video.downloadedBytes))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            if (isDownloading) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Saving to Isolated Vault…",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                    Text(
                        text = "${formatBytesLabel(video.downloadedBytes)} / ${formatBytesLabel(video.totalBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progressFraction },
                    color = ElectricCyan,
                    trackColor = GlassSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = ElevatedBorder)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${video.sourceDomain} • ${formatDateShort(video.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onExportClick,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("export_vault_video_${video.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.IosShare,
                                contentDescription = "Export Copy",
                                tint = ElectricCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Export Copy",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }

                        Button(
                            onClick = onPlayClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SignalEmerald,
                                contentColor = Color(0xFF031B0E)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("play_vault_video_${video.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play in Vault",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isCurrentlyPlaying) "Playing" else "Play in Vault",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("delete_vault_video_${video.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Video",
                                tint = CrimsonAlert,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDurationMs(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", min, sec)
}

private fun formatDateShort(epochMs: Long): String {
    return runCatching {
        SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(epochMs))
    }.getOrDefault("")
}
