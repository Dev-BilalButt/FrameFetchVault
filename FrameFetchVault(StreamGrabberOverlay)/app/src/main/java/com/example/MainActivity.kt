package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.VaultDatabase
import com.example.data.VaultRepository
import com.example.overlay.BorderOverlayService
import com.example.ui.AppTab
import com.example.ui.FrameFetchViewModel
import com.example.ui.FramedBrowserScreen
import com.example.ui.OverlayControlScreen
import com.example.ui.PerimeterBorderContainer
import com.example.ui.PrivateVaultScreen
import com.example.ui.StreamInspectorSheet
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SignalEmerald
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var viewModelInstance: FrameFetchViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = VaultDatabase.getInstance(applicationContext)
        val repository = VaultRepository(applicationContext, database.vaultVideoDao())
        val factory = FrameFetchViewModel.Factory(applicationContext, repository)

        setContent {
            MyApplicationTheme {
                val vm: FrameFetchViewModel = viewModel(factory = factory)
                viewModelInstance = vm

                LaunchedEffect(Unit) {
                    handleIncomingIntent(intent, vm)
                }

                FrameFetchAppRoot(viewModel = vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModelInstance?.let { vm ->
            handleIncomingIntent(intent, vm)
        }
    }

    private fun handleIncomingIntent(intent: Intent?, vm: FrameFetchViewModel) {
        if (intent == null) return
        val capturedFromOverlay = intent.getStringExtra(BorderOverlayService.EXTRA_CAPTURED_URL)
        val openInspector = intent.getBooleanExtra(BorderOverlayService.EXTRA_OPEN_STREAM_INSPECTOR, false)

        if (!capturedFromOverlay.isNullOrBlank()) {
            vm.handleCapturedExternalUrl(capturedFromOverlay, openInspectorImmediately = openInspector)
            intent.removeExtra(BorderOverlayService.EXTRA_CAPTURED_URL)
            return
        }

        if (openInspector) {
            vm.openStreamInspector()
            intent.removeExtra(BorderOverlayService.EXTRA_OPEN_STREAM_INSPECTOR)
        }

        when (intent.action) {
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                val urlMatch = Regex("https?://[^\\s\"'<>]+", RegexOption.IGNORE_CASE).find(sharedText)?.value
                if (!urlMatch.isNullOrBlank()) {
                    vm.handleCapturedExternalUrl(urlMatch, openInspectorImmediately = true)
                }
            }
            Intent.ACTION_VIEW -> {
                val viewUrl = intent.dataString
                if (!viewUrl.isNullOrBlank()) {
                    vm.handleCapturedExternalUrl(viewUrl, openInspectorImmediately = true)
                }
            }
        }
    }
}

@Composable
fun FrameFetchAppRoot(
    viewModel: FrameFetchViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var dismissedPermissionPrompt by androidx.compose.runtime.saveable.rememberSaveable {
        androidx.compose.runtime.mutableStateOf(false)
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshPermissionState()
        if (Settings.canDrawOverlays(context)) {
            viewModel.toggleSystemOverlayService(true)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionState()
                viewModel.refreshStorageStats()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Prompt user on launch if "Display over other apps" is not granted yet so the border stays when minimizing to Home
    if (!uiState.hasOverlayPermission && !dismissedPermissionPrompt) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { dismissedPermissionPrompt = true },
            containerColor = CarbonSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = TextSecondary,
            title = {
                Text(
                    text = "Keep Border Active on Home Screen",
                    style = MaterialTheme.typography.titleLarge,
                    color = ElectricCyan
                )
            },
            text = {
                Text(
                    text = "To keep the glowing extraction border and floating download button active over your normal mobile Home screen and other apps when you minimize this app, please allow 'Display over other apps'.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                 androidx.compose.material3.Button(
                    onClick = {
                        dismissedPermissionPrompt = true
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        runCatching { overlayPermissionLauncher.launch(intent) }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = SignalEmerald,
                        contentColor = Color(0xFF031B0E)
                    )
                ) {
                    Text(
                        text = "Allow Overlay Permission",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { dismissedPermissionPrompt = true }) {
                    Text("Use In-App Only", color = TextSecondary)
                }
            }
        )
    }

    // Auto-clear transient status banner after 4.5 seconds
    LaunchedEffect(uiState.statusBannerMessage) {
        if (uiState.statusBannerMessage != null) {
            delay(4500L)
            viewModel.clearStatusBanner()
        }
    }

    // Handle Back navigation on secondary tabs to return to Framed Browser Workspace
    if (uiState.activeTab != AppTab.WORKSPACE && uiState.activePlayerVideo == null) {
        BackHandler {
            viewModel.selectTab(AppTab.WORKSPACE)
        }
    }

    val activeDownloadCount = uiState.vaultVideos.count { it.status == "DOWNLOADING" }

    // Outer Active Viewfinder Border surrounding the entire screen
    PerimeterBorderContainer(
        borderState = uiState.borderState,
        strokeDp = uiState.borderStrokeDp,
        showCornerBrackets = uiState.showCornerBrackets,
        detectedStreams = uiState.detectedStreams,
        activeDownloadCount = activeDownloadCount,
        onInspectStreamsClick = { viewModel.openStreamInspector() },
        onOpenVaultClick = { viewModel.selectTab(AppTab.PRIVATE_VAULT) },
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Scaffold(
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar(
                    containerColor = CarbonSurface,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    // Tab 1: Framed Browser & Sniffer
                    NavigationBarItem(
                        selected = uiState.activeTab == AppTab.WORKSPACE,
                        onClick = { viewModel.selectTab(AppTab.WORKSPACE) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (uiState.detectedStreams.isNotEmpty()) {
                                        Badge(
                                            containerColor = SignalEmerald,
                                            contentColor = Color(0xFF031B0E)
                                        ) {
                                            Text("${uiState.detectedStreams.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (uiState.activeTab == AppTab.WORKSPACE) {
                                        Icons.Filled.Language
                                    } else {
                                        Icons.Outlined.Language
                                    },
                                    contentDescription = stringResource(R.string.nav_workspace)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.nav_workspace),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricCyan,
                            selectedTextColor = ElectricCyan,
                            indicatorColor = ElectricCyan.copy(alpha = 0.16f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_workspace")
                    )

                    // Tab 2: System Border Overlay Deck
                    NavigationBarItem(
                        selected = uiState.activeTab == AppTab.OVERLAY_DECK,
                        onClick = { viewModel.selectTab(AppTab.OVERLAY_DECK) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (uiState.isOverlayServiceRunning) {
                                        Badge(
                                            containerColor = SignalEmerald,
                                            contentColor = Color(0xFF031B0E)
                                        ) {
                                            Text("ON")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (uiState.activeTab == AppTab.OVERLAY_DECK) {
                                        Icons.Filled.Layers
                                    } else {
                                        Icons.Outlined.Layers
                                    },
                                    contentDescription = stringResource(R.string.nav_overlay)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.nav_overlay),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricCyan,
                            selectedTextColor = ElectricCyan,
                            indicatorColor = ElectricCyan.copy(alpha = 0.16f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_overlay")
                    )

                    // Tab 3: Isolated Private Storage Vault
                    NavigationBarItem(
                        selected = uiState.activeTab == AppTab.PRIVATE_VAULT,
                        onClick = { viewModel.selectTab(AppTab.PRIVATE_VAULT) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (uiState.vaultVideos.isNotEmpty()) {
                                        Badge(
                                            containerColor = if (activeDownloadCount > 0) ElectricCyan else SignalEmerald,
                                            contentColor = Color(0xFF031B0E)
                                        ) {
                                            Text("${uiState.vaultVideos.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (uiState.activeTab == AppTab.PRIVATE_VAULT) {
                                        Icons.Filled.FolderSpecial
                                    } else {
                                        Icons.Outlined.FolderSpecial
                                    },
                                    contentDescription = stringResource(R.string.nav_vault)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.nav_vault),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricCyan,
                            selectedTextColor = ElectricCyan,
                            indicatorColor = ElectricCyan.copy(alpha = 0.16f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_tab_vault")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (uiState.activeTab) {
                    AppTab.WORKSPACE -> {
                        FramedBrowserScreen(
                            uiState = uiState,
                            showcaseSites = viewModel.showcaseSites,
                            builtInHubHtml = viewModel.getBuiltInVideoHubHtml(),
                            onUrlInputChange = viewModel::onUrlInputChange,
                            onNavigateToUrl = viewModel::navigateToUrlOrSearch,
                            onPageStarted = viewModel::onPageStarted,
                            onPageFinished = viewModel::onPageFinished,
                            onDomVideosSniffed = viewModel::onDomVideosSniffed,
                            onNetworkVideoUrlIntercepted = viewModel::onNetworkVideoUrlIntercepted,
                            onInspectStreamsClick = { stream -> viewModel.openStreamInspector(stream) },
                            onOpenOverlayTabClick = { viewModel.selectTab(AppTab.OVERLAY_DECK) }
                        )
                    }

                    AppTab.OVERLAY_DECK -> {
                        OverlayControlScreen(
                            uiState = uiState,
                            onRequestOverlayPermission = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                runCatching { overlayPermissionLauncher.launch(intent) }
                            },
                            onToggleOverlayService = viewModel::toggleSystemOverlayService,
                            onUpdateBorderStyle = viewModel::updateBorderStyle,
                            onExternalPasteUrlChange = viewModel::onExternalPasteUrlChange,
                            onExtractExternalUrl = { url ->
                                viewModel.handleCapturedExternalUrl(url, openInspectorImmediately = true)
                            }
                        )
                    }

                    AppTab.PRIVATE_VAULT -> {
                        PrivateVaultScreen(
                            uiState = uiState,
                            onSearchQueryChange = viewModel::updateVaultSearchQuery,
                            onFilterChange = viewModel::updateVaultFilter,
                            onPlayVaultVideo = viewModel::openVaultVideoPlayer,
                            onClosePlayer = viewModel::closeVaultVideoPlayer,
                            onDeleteVaultVideo = viewModel::deleteVaultVideo,
                            onExportVaultVideo = viewModel::exportVaultVideo,
                            onGoToBrowserClick = { viewModel.selectTab(AppTab.WORKSPACE) }
                        )
                    }
                }

                // Floating Status Notification Banner
                AnimatedVisibility(
                    visible = uiState.statusBannerMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = GlassSurface.copy(alpha = 0.96f),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ElectricCyan, RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = uiState.statusBannerMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    viewModel.clearStatusBanner()
                                    viewModel.selectTab(AppTab.PRIVATE_VAULT)
                                }
                            ) {
                                Text(
                                    text = "View Vault",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SignalEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Pre-Download Stream Resolution & Format Inspector Sheet
                if (uiState.isStreamInspectorVisible) {
                    StreamInspectorSheet(
                        detectedStreams = uiState.detectedStreams,
                        selectedStream = uiState.selectedStreamForInspection,
                        onSelectStream = viewModel::selectStreamInInspector,
                        onStartDownload = { stream, variant, customTitle ->
                            viewModel.startVaultDownload(stream, variant, customTitle)
                        },
                        onDismiss = viewModel::dismissStreamInspector
                    )
                }
            }
        }
    }
}
