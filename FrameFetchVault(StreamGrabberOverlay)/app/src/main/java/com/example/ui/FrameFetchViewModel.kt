package com.example.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BorderState
import com.example.data.DetectedVideoStream
import com.example.data.QualityVariant
import com.example.data.QuickShowcaseSite
import com.example.data.VaultRepository
import com.example.data.VaultStorageStats
import com.example.data.VaultVideoEntity
import com.example.overlay.BorderOverlayService
import com.example.overlay.BorderOverlayStateBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray

enum class AppTab {
    WORKSPACE,
    OVERLAY_DECK,
    PRIVATE_VAULT
}

data class FrameFetchUiState(
    val activeTab: AppTab = AppTab.WORKSPACE,
    val currentUrl: String = "https://framefetch.local/studio",
    val urlInputText: String = "https://framefetch.local/studio",
    val isPageLoading: Boolean = false,
    val borderState: BorderState = BorderState.IDLE,
    val detectedStreams: List<DetectedVideoStream> = emptyList(),
    val selectedStreamForInspection: DetectedVideoStream? = null,
    val isStreamInspectorVisible: Boolean = false,
    val vaultVideos: List<VaultVideoEntity> = emptyList(),
    val vaultSearchQuery: String = "",
    val vaultFilter: String = "ALL", // "ALL", "COMPLETED", "DOWNLOADING"
    val storageStats: VaultStorageStats = VaultStorageStats(),
    val activePlayerVideo: VaultVideoEntity? = null,
    val hasOverlayPermission: Boolean = false,
    val isOverlayServiceRunning: Boolean = false,
    val borderStrokeDp: Float = 4f,
    val showCornerBrackets: Boolean = true,
    val externalPasteUrl: String = "",
    val isProbingExternalUrl: Boolean = false,
    val statusBannerMessage: String? = null
)

class FrameFetchViewModel(
    private val appContext: Context,
    private val repository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FrameFetchUiState())
    val uiState: StateFlow<FrameFetchUiState> = _uiState.asStateFlow()

    val showcaseSites: List<QuickShowcaseSite> = repository.showcaseSites

    init {
        refreshPermissionState()
        refreshStorageStats()

        // Observe Room vault videos reactively
        viewModelScope.launch {
            repository.allVaultVideos.collectLatest { videos ->
                val anyDownloading = videos.any { it.status == "DOWNLOADING" }
                _uiState.update { current ->
                    val nextBorder = when {
                        anyDownloading -> BorderState.DOWNLOADING
                        current.detectedStreams.isNotEmpty() -> BorderState.VIDEO_DETECTED
                        current.isPageLoading -> BorderState.SCANNING
                        else -> BorderState.IDLE
                    }
                    BorderOverlayStateBus.updateBorderStatus(
                        borderState = nextBorder,
                        detectedCount = current.detectedStreams.size,
                        latestTitle = current.detectedStreams.firstOrNull()?.title.orEmpty()
                    )
                    current.copy(
                        vaultVideos = videos,
                        borderState = nextBorder
                    )
                }
                refreshStorageStats()
            }
        }

        // Observe System Border Overlay runtime bus
        viewModelScope.launch {
            BorderOverlayStateBus.state.collectLatest { overlayState ->
                _uiState.update {
                    it.copy(
                        isOverlayServiceRunning = overlayState.isOverlayRunning,
                        borderStrokeDp = overlayState.borderStrokeDp,
                        showCornerBrackets = overlayState.showCornerBrackets
                    )
                }
                val pendingUrl = BorderOverlayStateBus.consumePendingExternalUrl()
                if (!pendingUrl.isNullOrBlank()) {
                    handleCapturedExternalUrl(pendingUrl, openInspectorImmediately = true)
                }
            }
        }

        // Seed initial detection for the built-in studio hub so the border immediately demonstrates stream lock
        seedBuiltInStudioStreams()
    }

    fun getBuiltInVideoHubHtml(): String = repository.getBuiltInVideoHubHtml()

    private fun seedBuiltInStudioStreams() {
        viewModelScope.launch {
            val initialStreams = listOf(
                DetectedVideoStream(
                    id = "studio_bbb",
                    title = "Big Buck Bunny — Open Movie Mastering Clip",
                    pageUrl = "https://framefetch.local/studio",
                    sourceDomain = "framefetch.local",
                    primaryStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    format = "MP4",
                    mimeType = "video/mp4",
                    resolutionLabel = "1080p Full HD",
                    width = 1920,
                    height = 1080,
                    durationSeconds = 596,
                    contentLengthBytes = 158_008_374L,
                    qualityVariants = listOf(
                        QualityVariant(
                            id = "bbb_480_fast",
                            label = "480p Fast Clip (MP4)",
                            resolution = "854x480",
                            format = "MP4",
                            mimeType = "video/mp4",
                            streamUrl = "https://www.w3schools.com/html/mov_bbb.mp4",
                            sizeBytes = 788_493L,
                            bitrateLabel = "Fast Download • 10s Clip"
                        ),
                        QualityVariant(
                            id = "bbb_1080_full",
                            label = "1080p Full HD (MP4)",
                            resolution = "1920x1080",
                            format = "MP4",
                            mimeType = "video/mp4",
                            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                            sizeBytes = 158_008_374L,
                            bitrateLabel = "Master Bitrate • Full Film"
                        ),
                        QualityVariant(
                            id = "bbb_360_ogg",
                            label = "360p OGG Vorbis (OGG)",
                            resolution = "640x360",
                            format = "OGG",
                            mimeType = "video/ogg",
                            streamUrl = "https://www.w3schools.com/html/mov_bbb.ogg",
                            sizeBytes = 615_565L,
                            bitrateLabel = "Compact Stream"
                        )
                    )
                ),
                DetectedVideoStream(
                    id = "studio_blazes",
                    title = "For Bigger Blazes — High Frame Rate Demo",
                    pageUrl = "https://framefetch.local/studio",
                    sourceDomain = "framefetch.local",
                    primaryStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    format = "MP4",
                    mimeType = "video/mp4",
                    resolutionLabel = "720p HD",
                    width = 1280,
                    height = 720,
                    durationSeconds = 15,
                    contentLengthBytes = 2_498_185L,
                    qualityVariants = listOf(
                        QualityVariant(
                            id = "blazes_720",
                            label = "720p HD (MP4)",
                            resolution = "1280x720",
                            format = "MP4",
                            mimeType = "video/mp4",
                            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                            sizeBytes = 2_498_185L,
                            bitrateLabel = "HD Showcase • 15s"
                        )
                    )
                )
            )
            _uiState.update {
                it.copy(
                    detectedStreams = initialStreams,
                    selectedStreamForInspection = initialStreams.first(),
                    borderState = BorderState.VIDEO_DETECTED
                )
            }
            BorderOverlayStateBus.updateBorderStatus(
                borderState = BorderState.VIDEO_DETECTED,
                detectedCount = initialStreams.size,
                latestTitle = initialStreams.first().title
            )
        }
    }

    fun refreshPermissionState() {
        val canDraw = Settings.canDrawOverlays(appContext)
        _uiState.update { it.copy(hasOverlayPermission = canDraw) }
        val busState = BorderOverlayStateBus.state.value
        if (canDraw && !busState.isOverlayRunning && !busState.userExplicitlyDisabled) {
            startPersistentOverlayService()
        }
    }

    private fun startPersistentOverlayService() {
        val serviceIntent = Intent(appContext, BorderOverlayService::class.java)
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                appContext.startForegroundService(serviceIntent)
            } else {
                appContext.startService(serviceIntent)
            }
            BorderOverlayStateBus.setOverlayRunning(running = true, explicitlyDisabled = false)
            _uiState.update { it.copy(isOverlayServiceRunning = true) }
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            val stats = repository.computeStorageStats()
            _uiState.update { it.copy(storageStats = stats) }
        }
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(activeTab = tab) }
        if (tab == AppTab.PRIVATE_VAULT) {
            refreshStorageStats()
        }
    }

    fun onUrlInputChange(text: String) {
        _uiState.update { it.copy(urlInputText = text) }
    }

    fun navigateToUrlOrSearch(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return

        val resolvedUrl = when {
            trimmed.equals("https://framefetch.local/studio", ignoreCase = true) -> "https://framefetch.local/studio"
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> "https://duckduckgo.com/?q=${ java.net.URLEncoder.encode("$trimmed video", "UTF-8") }"
        }

        _uiState.update {
            it.copy(
                activeTab = AppTab.WORKSPACE,
                currentUrl = resolvedUrl,
                urlInputText = resolvedUrl,
                isPageLoading = true,
                borderState = BorderState.SCANNING,
                detectedStreams = if (resolvedUrl == "https://framefetch.local/studio") it.detectedStreams else emptyList()
            )
        }

        if (resolvedUrl == "https://framefetch.local/studio") {
            seedBuiltInStudioStreams()
        } else {
            // Also probe the URL directly in the background in case it's a direct .mp4 link or static video page
            viewModelScope.launch {
                val probed = repository.probeStreamOrPageUrl(resolvedUrl)
                if (probed.isNotEmpty()) {
                    mergeDetectedStreams(probed)
                }
            }
        }
    }

    fun onPageStarted(url: String) {
        _uiState.update {
            it.copy(
                currentUrl = url,
                urlInputText = url,
                isPageLoading = true,
                borderState = if (it.detectedStreams.isEmpty()) BorderState.SCANNING else BorderState.VIDEO_DETECTED
            )
        }
    }

    fun onPageFinished(url: String) {
        _uiState.update { current ->
            val nextState = when {
                current.vaultVideos.any { it.status == "DOWNLOADING" } -> BorderState.DOWNLOADING
                current.detectedStreams.isNotEmpty() -> BorderState.VIDEO_DETECTED
                else -> BorderState.IDLE
            }
            current.copy(
                isPageLoading = false,
                borderState = nextState
            )
        }
    }

    /**
     * Called by the WebView JavascriptInterface whenever <video> or <source> elements are found in the DOM.
     */
    fun onDomVideosSniffed(jsonPayload: String, pageUrl: String) {
        viewModelScope.launch {
            runCatching {
                val arr = JSONArray(jsonPayload)
                val newStreams = mutableListOf<DetectedVideoStream>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val title = obj.optString("title", "Extracted Video #${i + 1}")
                    val sourcesArr = obj.optJSONArray("sources") ?: continue
                    val rawSources = mutableListOf<Triple<String, String, String>>()
                    for (j in 0 until sourcesArr.length()) {
                        val sObj = sourcesArr.getJSONObject(j)
                        val srcUrl = sObj.optString("src")
                        val mime = sObj.optString("type", "video/mp4")
                        val res = sObj.optString("res", "")
                        if (srcUrl.isNotBlank()) {
                            rawSources.add(Triple(srcUrl, mime, res))
                        }
                    }
                    val enriched = repository.enrichDomDetectedStream(
                        title = title,
                        pageUrl = pageUrl,
                        rawSources = rawSources
                    )
                    if (enriched != null) {
                        newStreams.add(enriched)
                    }
                }
                if (newStreams.isNotEmpty()) {
                    mergeDetectedStreams(newStreams)
                }
            }
        }
    }

    /**
     * Called when WebView's shouldInterceptRequest catches a network request for a video file.
     */
    fun onNetworkVideoUrlIntercepted(videoUrl: String, pageUrl: String) {
        viewModelScope.launch {
            val probed = repository.probeStreamOrPageUrl(
                rawUrl = videoUrl,
                pageContextUrl = pageUrl
            )
            if (probed.isNotEmpty()) {
                mergeDetectedStreams(probed)
            }
        }
    }

    private fun mergeDetectedStreams(incoming: List<DetectedVideoStream>) {
        _uiState.update { current ->
            val combined = (incoming + current.detectedStreams)
                .distinctBy { it.primaryStreamUrl }
            val selected = current.selectedStreamForInspection ?: combined.firstOrNull()
            BorderOverlayStateBus.updateBorderStatus(
                borderState = BorderState.VIDEO_DETECTED,
                detectedCount = combined.size,
                latestTitle = combined.firstOrNull()?.title.orEmpty()
            )
            current.copy(
                detectedStreams = combined,
                selectedStreamForInspection = selected,
                borderState = BorderState.VIDEO_DETECTED
            )
        }
    }

    fun openStreamInspector(stream: DetectedVideoStream? = null) {
        _uiState.update { current ->
            val target = stream ?: current.selectedStreamForInspection ?: current.detectedStreams.firstOrNull()
            current.copy(
                selectedStreamForInspection = target,
                isStreamInspectorVisible = target != null
            )
        }
    }

    fun dismissStreamInspector() {
        _uiState.update { it.copy(isStreamInspectorVisible = false) }
    }

    fun selectStreamInInspector(stream: DetectedVideoStream) {
        _uiState.update { it.copy(selectedStreamForInspection = stream) }
    }

    fun startVaultDownload(
        stream: DetectedVideoStream,
        variant: QualityVariant,
        customTitle: String
    ) {
        _uiState.update {
            it.copy(
                isStreamInspectorVisible = false,
                borderState = BorderState.DOWNLOADING,
                statusBannerMessage = "Downloading '${customTitle.ifBlank { stream.title }}' (${variant.label}) to Private Vault…"
            )
        }
        viewModelScope.launch {
            repository.downloadStreamToVault(
                stream = stream,
                selectedVariant = variant,
                customTitle = customTitle
            )
            refreshStorageStats()
            _uiState.update {
                it.copy(
                    statusBannerMessage = "Saved '${customTitle.ifBlank { stream.title }}' to Isolated Private Vault"
                )
            }
        }
    }

    fun onExternalPasteUrlChange(url: String) {
        _uiState.update { it.copy(externalPasteUrl = url) }
    }

    fun handleCapturedExternalUrl(url: String, openInspectorImmediately: Boolean = true) {
        val clean = url.trim()
        if (clean.isBlank()) return
        _uiState.update {
            it.copy(
                externalPasteUrl = clean,
                isProbingExternalUrl = true,
                borderState = BorderState.SCANNING,
                statusBannerMessage = "Border Sniffer: Probing video streams from link…"
            )
        }
        viewModelScope.launch {
            val discovered = repository.probeStreamOrPageUrl(clean)
            if (discovered.isNotEmpty()) {
                mergeDetectedStreams(discovered)
                _uiState.update {
                    it.copy(
                        isProbingExternalUrl = false,
                        selectedStreamForInspection = discovered.first(),
                        isStreamInspectorVisible = openInspectorImmediately,
                        statusBannerMessage = "Extracted ${discovered.first().qualityVariants.size} stream option(s) ready for Vault download"
                    )
                }
            } else {
                // Load the URL inside the Framed Browser so its dynamic JS DOM can execute and be sniffed
                _uiState.update {
                    it.copy(
                        isProbingExternalUrl = false,
                        activeTab = AppTab.WORKSPACE,
                        currentUrl = clean,
                        urlInputText = clean,
                        statusBannerMessage = "Opened link inside Framed Browser for live DOM video extraction"
                    )
                }
            }
        }
    }

    fun toggleSystemOverlayService(enable: Boolean) {
        val canDraw = Settings.canDrawOverlays(appContext)
        _uiState.update { it.copy(hasOverlayPermission = canDraw) }
        if (!canDraw) {
            _uiState.update {
                it.copy(statusBannerMessage = "Please grant 'Display over other apps' permission first")
            }
            return
        }
        if (enable) {
            startPersistentOverlayService()
            _uiState.update {
                it.copy(
                    isOverlayServiceRunning = true,
                    statusBannerMessage = "System Border Overlay active — stays on when you go to Home!"
                )
            }
        } else {
            val stopIntent = Intent(appContext, BorderOverlayService::class.java).apply {
                action = BorderOverlayService.ACTION_STOP_OVERLAY
            }
            runCatching { appContext.startService(stopIntent) }
            BorderOverlayStateBus.setOverlayRunning(running = false, explicitlyDisabled = true)
            _uiState.update {
                it.copy(
                    isOverlayServiceRunning = false,
                    statusBannerMessage = "System Border Overlay stopped"
                )
            }
        }
    }

    fun updateBorderStyle(strokeDp: Float, showBrackets: Boolean) {
        BorderOverlayStateBus.updateBorderCustomization(strokeDp, showBrackets)
        _uiState.update {
            it.copy(
                borderStrokeDp = strokeDp,
                showCornerBrackets = showBrackets
            )
        }
    }

    fun updateVaultSearchQuery(query: String) {
        _uiState.update { it.copy(vaultSearchQuery = query) }
    }

    fun updateVaultFilter(filter: String) {
        _uiState.update { it.copy(vaultFilter = filter) }
    }

    fun openVaultVideoPlayer(video: VaultVideoEntity) {
        _uiState.update { it.copy(activePlayerVideo = video) }
    }

    fun closeVaultVideoPlayer() {
        _uiState.update { it.copy(activePlayerVideo = null) }
    }

    fun deleteVaultVideo(video: VaultVideoEntity) {
        viewModelScope.launch {
            if (_uiState.value.activePlayerVideo?.id == video.id) {
                _uiState.update { it.copy(activePlayerVideo = null) }
            }
            repository.deleteVaultVideo(video)
            refreshStorageStats()
            _uiState.update {
                it.copy(statusBannerMessage = "Removed '${video.title}' from Private Vault")
            }
        }
    }

    fun exportVaultVideo(video: VaultVideoEntity) {
        viewModelScope.launch {
            val res = repository.exportVideoToPublicDownloads(video)
            val msg = res.fold(
                onSuccess = { path -> "Exported copy to device storage: $path" },
                onFailure = { err -> "Export failed: ${err.message}" }
            )
            _uiState.update { it.copy(statusBannerMessage = msg) }
        }
    }

    fun clearStatusBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    class Factory(
        private val appContext: Context,
        private val repository: VaultRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FrameFetchViewModel(appContext, repository) as T
        }
    }
}
