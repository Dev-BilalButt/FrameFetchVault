package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.BorderState
import com.example.data.DetectedVideoStream
import com.example.data.QuickShowcaseSite
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SignalEmerald
import com.example.ui.theme.TextSecondary

private class WebViewDomSnifferBridge(
    private val onDomPayload: (String, String) -> Unit
) {
    @JavascriptInterface
    fun onVideosFound(jsonArrayString: String, pageUrl: String) {
        if (jsonArrayString.isNotBlank() && jsonArrayString != "[]") {
            onDomPayload(jsonArrayString, pageUrl)
        }
    }
}

private val DOM_VIDEO_SNIFFER_SCRIPT = """
(function() {
    function scanPageForVideos() {
        try {
            var videos = document.querySelectorAll('video');
            var payload = [];
            for (var i = 0; i < videos.length; i++) {
                var v = videos[i];
                var title = v.getAttribute('data-title') || document.title || ('Web Video Stream #' + (i + 1));
                var sources = [];
                if (v.currentSrc && v.currentSrc.indexOf('http') === 0) {
                    var w = v.videoWidth || v.getAttribute('width') || 1280;
                    var h = v.videoHeight || v.getAttribute('height') || 720;
                    sources.push({
                        src: v.currentSrc,
                        type: 'video/mp4',
                        res: (h >= 1080 ? '1080p Full HD' : (h >= 720 ? '720p HD' : '480p SD'))
                    });
                }
                if (v.src && v.src.indexOf('http') === 0) {
                    sources.push({
                        src: v.src,
                        type: v.getAttribute('type') || 'video/mp4',
                        res: v.getAttribute('data-res') || ''
                    });
                }
                var childSources = v.querySelectorAll('source');
                for (var j = 0; j < childSources.length; j++) {
                    var cs = childSources[j];
                    var cUrl = cs.src || cs.getAttribute('src') || '';
                    if (cUrl.indexOf('http') === 0) {
                        sources.push({
                            src: cUrl,
                            type: cs.getAttribute('type') || 'video/mp4',
                            res: cs.getAttribute('data-res') || ''
                        });
                    }
                }
                if (sources.length > 0) {
                    payload.push({ title: title, sources: sources });
                }
            }
            if (payload.length > 0 && window.FrameFetchBridge) {
                window.FrameFetchBridge.onVideosFound(JSON.stringify(payload), window.location.href);
            }
        } catch (err) {}
    }
    scanPageForVideos();
    document.addEventListener('play', scanPageForVideos, true);
    document.addEventListener('loadedmetadata', scanPageForVideos, true);
    if (!window.__frameFetchObserverAttached) {
        window.__frameFetchObserverAttached = true;
        var observer = new MutationObserver(function() { scanPageForVideos(); });
        observer.observe(document.documentElement || document.body, { childList: true, subtree: true });
    }
})();
""".trimIndent()

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FramedBrowserScreen(
    uiState: FrameFetchUiState,
    showcaseSites: List<QuickShowcaseSite>,
    builtInHubHtml: String,
    onUrlInputChange: (String) -> Unit,
    onNavigateToUrl: (String) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onDomVideosSniffed: (String, String) -> Unit,
    onNetworkVideoUrlIntercepted: (String, String) -> Unit,
    onInspectStreamsClick: (DetectedVideoStream?) -> Unit,
    onOpenOverlayTabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var lastLoadedTarget by remember { mutableStateOf("") }

    val currentOnDomVideosSniffed by rememberUpdatedState(onDomVideosSniffed)
    val currentOnNetworkVideoIntercepted by rememberUpdatedState(onNetworkVideoUrlIntercepted)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // 1. Tactical Top Status HUD inside Border
        Surface(
            color = CarbonSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Live Border Status Pill
                    val (statusText, statusColor) = when (uiState.borderState) {
                        BorderState.VIDEO_DETECTED -> "VIDEO STREAM LOCKED (${uiState.detectedStreams.size})" to SignalEmerald
                        BorderState.DOWNLOADING -> "DOWNLOADING TO VAULT" to ElectricCyan
                        BorderState.SCANNING -> "SCANNING FRAME FOR VIDEO…" to AmberPulse
                        BorderState.IDLE -> "BORDER ACTIVE • SNIFFER READY" to ElectricCyan
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor
                        )
                    }

                    // System Overlay Quick Badge + Minimize to Home Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (uiState.isOverlayServiceRunning) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = ElectricCyan.copy(alpha = 0.18f),
                                modifier = Modifier
                                    .border(1.dp, ElectricCyan, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                                            addCategory(android.content.Intent.CATEGORY_HOME)
                                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        runCatching { context.startActivity(homeIntent) }
                                    }
                                    .testTag("minimize_to_home_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "Minimize to Home",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (uiState.isOverlayServiceRunning) {
                                SignalEmerald.copy(alpha = 0.16f)
                            } else {
                                AmberPulse.copy(alpha = 0.18f)
                            },
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (uiState.isOverlayServiceRunning) SignalEmerald else AmberPulse,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onOpenOverlayTabClick() }
                                .testTag("overlay_status_chip")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = "Overlay Mode",
                                    tint = if (uiState.isOverlayServiceRunning) SignalEmerald else AmberPulse,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (uiState.isOverlayServiceRunning) "Home Border ON" else "Enable Home Border",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.isOverlayServiceRunning) SignalEmerald else AmberPulse,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Framed Omnibar URL & Search Input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.urlInputText,
                        onValueChange = onUrlInputChange,
                        placeholder = {
                            Text(
                                text = "Enter website URL, video link, or search…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "URL",
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        focusManager.clearFocus()
                                        webViewRef?.evaluateJavascript(DOM_VIDEO_SNIFFER_SCRIPT, null)
                                        onNavigateToUrl(uiState.urlInputText)
                                    },
                                    modifier = Modifier.testTag("go_url_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Navigate",
                                        tint = ElectricCyan
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                focusManager.clearFocus()
                                onNavigateToUrl(uiState.urlInputText)
                            }
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = ElevatedBorder,
                            focusedContainerColor = GlassSurface,
                            unfocusedContainerColor = GlassSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("url_input_field")
                    )

                    IconButton(
                        onClick = {
                            webViewRef?.evaluateJavascript(DOM_VIDEO_SNIFFER_SCRIPT, null)
                            onNavigateToUrl(uiState.currentUrl)
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GlassSurface)
                            .border(1.dp, ElevatedBorder, RoundedCornerShape(12.dp))
                            .testTag("rescan_page_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Rescan Page for Videos",
                            tint = ElectricCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Quick-Launch Video Sources Bar
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(showcaseSites) { site ->
                        val isSelected = uiState.currentUrl.equals(site.url, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) ElectricCyan.copy(alpha = 0.16f) else GlassSurface,
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) ElectricCyan else ElevatedBorder,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    focusManager.clearFocus()
                                    onNavigateToUrl(site.url)
                                }
                                .testTag("showcase_chip_${site.badge}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = site.badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) ElectricCyan else SignalEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = site.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isPageLoading) {
            LinearProgressIndicator(
                color = ElectricCyan,
                trackColor = CarbonSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }

        // 4. Instant Extraction Banner inside the Border when Videos are Detected
        if (uiState.detectedStreams.isNotEmpty()) {
            val lead = uiState.detectedStreams.first()
            Surface(
                color = Color(0xFF0E231C),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SignalEmerald.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoFile,
                                contentDescription = null,
                                tint = SignalEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lead.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${uiState.detectedStreams.size} stream(s) extracted • ${lead.resolutionLabel} • ${lead.format} (${lead.qualityVariants.size} quality options)",
                                style = MaterialTheme.typography.labelSmall,
                                color = SignalEmerald,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onInspectStreamsClick(lead) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SignalEmerald,
                            contentColor = Color(0xFF041B0E)
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("banner_download_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download Video",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Download",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            HorizontalDivider(color = SignalEmerald.copy(alpha = 0.35f), thickness = 1.dp)
        }

        // 5. Embedded Sniffing WebView inside the Active Border
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.loadsImagesAutomatically = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true

                        addJavascriptInterface(
                            WebViewDomSnifferBridge { jsonPayload, pageUrl ->
                                currentOnDomVideosSniffed(jsonPayload, pageUrl)
                            },
                            "FrameFetchBridge"
                        )

                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                if (!url.isNullOrBlank()) {
                                    onPageStarted(url)
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                view?.evaluateJavascript(DOM_VIDEO_SNIFFER_SCRIPT, null)
                                if (!url.isNullOrBlank()) {
                                    onPageFinished(url)
                                }
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val reqUrl = request?.url?.toString().orEmpty()
                                val clean = reqUrl.substringBefore("?").lowercase()
                                if (clean.endsWith(".mp4") ||
                                    clean.endsWith(".webm") ||
                                    clean.endsWith(".ogg") ||
                                    clean.endsWith(".m4v") ||
                                    clean.endsWith(".mov") ||
                                    clean.endsWith(".m3u8")
                                ) {
                                    currentOnNetworkVideoIntercepted(reqUrl, uiState.currentUrl)
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                        }
                        webViewRef = this
                    }
                },
                update = { webView ->
                    if (lastLoadedTarget != uiState.currentUrl) {
                        lastLoadedTarget = uiState.currentUrl
                        if (uiState.currentUrl == "https://framefetch.local/studio") {
                            webView.loadDataWithBaseURL(
                                "https://framefetch.local/studio",
                                builtInHubHtml,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        } else {
                            webView.loadUrl(uiState.currentUrl)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("framed_sniffing_webview")
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.stopLoading()
        }
    }
}
