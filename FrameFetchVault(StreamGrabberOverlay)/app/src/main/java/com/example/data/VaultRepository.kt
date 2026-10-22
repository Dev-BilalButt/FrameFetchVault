package com.example.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

data class QuickShowcaseSite(
    val title: String,
    val subtitle: String,
    val url: String,
    val badge: String
)

class VaultRepository(
    private val context: Context,
    private val dao: VaultVideoDao
) {
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    val allVaultVideos: Flow<List<VaultVideoEntity>> = dao.observeAllVideos()

    val showcaseSites: List<QuickShowcaseSite> = listOf(
        QuickShowcaseSite(
            title = "Open Video Studio Hub",
            subtitle = "Multi-resolution 1080p / 720p / 480p HTML5 streams",
            url = "https://framefetch.local/studio",
            badge = "INSTANT HD"
        ),
        QuickShowcaseSite(
            title = "W3C HTML5 Video Spec",
            subtitle = "Official HTML5 video player & MP4/OGG test page",
            url = "https://www.w3schools.com/html/html5_video.asp",
            badge = "HTML5 DOM"
        ),
        QuickShowcaseSite(
            title = "Wikimedia Commons Videos",
            subtitle = "Open-licensed documentary & science video files",
            url = "https://commons.wikimedia.org/wiki/Commons:Video",
            badge = "WEBM / MP4"
        ),
        QuickShowcaseSite(
            title = "Archive Open Cinema",
            subtitle = "Public domain feature films & clips",
            url = "https://archive.org/details/movies",
            badge = "ARCHIVE"
        )
    )

    /**
     * Returns the dedicated, isolated storage directory on the device's mobile storage
     * with a `.nomedia` guard so downloaded videos stay separate from the public gallery.
     */
    fun getIsolatedVaultDirectory(): File {
        val baseDir = context.getExternalFilesDir("vault_videos")
            ?: File(context.filesDir, "vault_videos")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        val noMediaFile = File(baseDir, ".nomedia")
        if (!noMediaFile.exists()) {
            runCatching { noMediaFile.createNewFile() }
        }
        return baseDir
    }

    suspend fun computeStorageStats(): VaultStorageStats = withContext(Dispatchers.IO) {
        val vaultDir = getIsolatedVaultDirectory()
        val videoFiles = vaultDir.listFiles()?.filter { it.isFile && it.name != ".nomedia" }.orEmpty()
        val usedBytes = videoFiles.sumOf { it.length() }
        val noMediaExists = File(vaultDir, ".nomedia").exists()

        val statFs = runCatching { StatFs(vaultDir.absolutePath) }.getOrNull()
        val freeBytes = statFs?.availableBytes ?: 0L
        val totalBytes = (statFs?.totalBytes ?: 1L).coerceAtLeast(1L)

        VaultStorageStats(
            vaultDirectoryPath = vaultDir.absolutePath,
            vaultUsedBytes = usedBytes,
            vaultFileCount = videoFiles.size,
            deviceFreeBytes = freeBytes,
            deviceTotalBytes = totalBytes,
            isIsolatedNoMediaActive = noMediaExists
        )
    }

    /**
     * Built-in HTML5 Video Hub HTML with real playable open-source video streams
     * across 1080p, 720p, and 480p so the user can test DOM sniffing immediately inside the border.
     */
    fun getBuiltInVideoHubHtml(): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>FrameFetch Open Video Hub</title>
                <style>
                    body {
                        margin: 0;
                        padding: 16px;
                        background: #0D131C;
                        color: #F2F6FC;
                        font-family: -apple-system, BlinkMacSystemFont, sans-serif;
                    }
                    .header {
                        border-left: 3px solid #00E5FF;
                        padding-left: 12px;
                        margin-bottom: 18px;
                    }
                    .header h2 { margin: 0 0 4px 0; font-size: 18px; color: #00E5FF; }
                    .header p { margin: 0; font-size: 12px; color: #9BA9BC; }
                    .card {
                        background: #141D2B;
                        border: 1px solid #243248;
                        border-radius: 14px;
                        padding: 14px;
                        margin-bottom: 16px;
                    }
                    .badge {
                        display: inline-block;
                        background: rgba(0, 230, 118, 0.16);
                        color: #00E676;
                        font-size: 11px;
                        font-weight: 700;
                        padding: 3px 8px;
                        border-radius: 6px;
                        margin-bottom: 8px;
                    }
                    h3 { margin: 4px 0 6px 0; font-size: 15px; }
                    p.desc { margin: 0 0 10px 0; font-size: 12px; color: #9BA9BC; }
                    video {
                        width: 100%;
                        border-radius: 10px;
                        background: #000;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h2>Live Stream Sniffer Test Deck</h2>
                    <p>Play any video below or browse any URL above — the active border automatically extracts all stream resolutions.</p>
                </div>

                <div class="card">
                    <span class="badge">MULTI-BITRATE • 1080p / 720p / 360p MP4</span>
                    <h3>Big Buck Bunny — Open Movie Mastering Clip</h3>
                    <p class="desc">Blender Foundation open-source animation with multiple resolution sources embedded in DOM.</p>
                    <video controls preload="metadata" data-title="Big Buck Bunny Open Movie" width="1920" height="1080">
                        <source src="https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" type="video/mp4" data-res="1080p Full HD" data-width="1920" data-height="1080" />
                        <source src="https://www.w3schools.com/html/mov_bbb.mp4" type="video/mp4" data-res="480p Fast Clip" data-width="854" data-height="480" />
                        <source src="https://www.w3schools.com/html/mov_bbb.ogg" type="video/ogg" data-res="360p OGG Vorbis" data-width="640" data-height="360" />
                    </video>
                </div>

                <div class="card">
                    <span class="badge">HD STREAM • 720p MP4</span>
                    <h3>For Bigger Blazes — High Frame Rate Demo</h3>
                    <p class="desc">Fast-downloading 15-second HD showcase clip (~2.4 MB) ideal for quick vault testing.</p>
                    <video controls preload="metadata" data-title="For Bigger Blazes HD Showcase" width="1280" height="720">
                        <source src="https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4" type="video/mp4" data-res="720p HD" data-width="1280" data-height="720" />
                    </video>
                </div>

                <div class="card">
                    <span class="badge">SCI-FI OPEN FILM • 1080p / 720p MP4</span>
                    <h3>Elephants Dream — Open Project Stream</h3>
                    <p class="desc">Open-source CGI short with HD and compact mobile stream profiles.</p>
                    <video controls preload="metadata" data-title="Elephants Dream Open Film" width="1280" height="720">
                        <source src="https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4" type="video/mp4" data-res="720p HD" data-width="1280" data-height="720" />
                        <source src="https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4" type="video/mp4" data-res="480p SD" data-width="854" data-height="480" />
                    </video>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Probes a direct video URL or webpage URL to extract downloadable video streams,
     * resolutions, MIME formats, and exact Content-Length via OkHttp.
     */
    suspend fun probeStreamOrPageUrl(
        rawUrl: String,
        fallbackTitle: String? = null,
        pageContextUrl: String? = null
    ): List<DetectedVideoStream> = withContext(Dispatchers.IO) {
        val cleanUrl = rawUrl.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            return@withContext emptyList()
        }

        val results = mutableListOf<DetectedVideoStream>()
        val domain = extractDomain(pageContextUrl ?: cleanUrl)

        // First check if the URL itself is a direct video resource or HTML page via HEAD/Range request
        val headInfo = probeUrlHeaders(cleanUrl)
        val isDirectVideo = isVideoMimeOrExtension(cleanUrl, headInfo.contentType)

        if (isDirectVideo) {
            val format = inferFormatLabel(cleanUrl, headInfo.contentType)
            val inferredRes = inferResolutionFromUrl(cleanUrl)
            val title = fallbackTitle?.takeIf { it.isNotBlank() }
                ?: inferTitleFromUrl(cleanUrl)

            val primaryVariant = QualityVariant(
                id = UUID.randomUUID().toString(),
                label = "${inferredRes.first} ($format)",
                resolution = "${inferredRes.second}x${inferredRes.third}",
                format = format,
                mimeType = headInfo.contentType.ifBlank { "video/mp4" },
                streamUrl = cleanUrl,
                sizeBytes = headInfo.contentLength,
                bitrateLabel = if (inferredRes.third >= 1080) "High Bitrate" else "Standard Stream"
            )

            results.add(
                DetectedVideoStream(
                    id = cleanUrl.hashCode().toString(),
                    title = title,
                    pageUrl = pageContextUrl ?: cleanUrl,
                    sourceDomain = domain,
                    primaryStreamUrl = cleanUrl,
                    format = format,
                    mimeType = headInfo.contentType.ifBlank { "video/mp4" },
                    resolutionLabel = inferredRes.first,
                    width = inferredRes.second,
                    height = inferredRes.third,
                    durationSeconds = 0,
                    contentLengthBytes = headInfo.contentLength,
                    qualityVariants = listOf(primaryVariant)
                )
            )
            return@withContext results
        }

        // Otherwise, fetch the webpage HTML and scan for <video>, <source>, og:video, or .mp4/.webm links
        val htmlBody = fetchHtmlSnippet(cleanUrl)
        if (htmlBody.isNotBlank()) {
            val pageTitle = extractHtmlTitle(htmlBody) ?: fallbackTitle ?: domain
            val discoveredUrls = extractVideoUrlsFromHtml(htmlBody, cleanUrl)
            if (discoveredUrls.isNotEmpty()) {
                val variants = discoveredUrls.mapIndexed { index, candidateUrl ->
                    val probe = probeUrlHeaders(candidateUrl)
                    val fmt = inferFormatLabel(candidateUrl, probe.contentType)
                    val res = inferResolutionFromUrl(candidateUrl, index)
                    QualityVariant(
                        id = "${candidateUrl.hashCode()}_$index",
                        label = "${res.first} • $fmt",
                        resolution = "${res.second}x${res.third}",
                        format = fmt,
                        mimeType = probe.contentType.ifBlank { "video/mp4" },
                        streamUrl = candidateUrl,
                        sizeBytes = probe.contentLength,
                        bitrateLabel = if (index == 0) "Primary Stream" else "Alternate Stream"
                    )
                }
                val primary = variants.first()
                results.add(
                    DetectedVideoStream(
                        id = cleanUrl.hashCode().toString(),
                        title = pageTitle,
                        pageUrl = cleanUrl,
                        sourceDomain = domain,
                        primaryStreamUrl = primary.streamUrl,
                        format = primary.format,
                        mimeType = primary.mimeType,
                        resolutionLabel = primary.label.substringBefore(" •"),
                        width = primary.resolution.substringBefore("x").toIntOrNull() ?: 1280,
                        height = primary.resolution.substringAfter("x").toIntOrNull() ?: 720,
                        durationSeconds = 0,
                        contentLengthBytes = primary.sizeBytes,
                        qualityVariants = variants
                    )
                )
            }
        }

        results
    }

    /**
     * Enriches raw streams detected by the WebView's JavaScript DOM sniffer with real HTTP
     * Content-Length and MIME headers.
     */
    suspend fun enrichDomDetectedStream(
        title: String,
        pageUrl: String,
        rawSources: List<Triple<String, String, String>> // (url, type, resHint)
    ): DetectedVideoStream? = withContext(Dispatchers.IO) {
        val validSources = rawSources
            .filter { it.first.startsWith("http://") || it.first.startsWith("https://") }
            .distinctBy { it.first }
        if (validSources.isEmpty()) return@withContext null

        val domain = extractDomain(pageUrl)
        val variants = validSources.mapIndexed { idx, (streamUrl, mimeHint, resHint) ->
            val headers = probeUrlHeaders(streamUrl)
            val resolvedMime = headers.contentType.takeIf { it.startsWith("video/") || it.contains("mpegurl", true) }
                ?: mimeHint.ifBlank { "video/mp4" }
            val format = inferFormatLabel(streamUrl, resolvedMime)
            val resTriple = if (resHint.isNotBlank()) {
                parseResHint(resHint)
            } else {
                inferResolutionFromUrl(streamUrl, idx)
            }

            QualityVariant(
                id = "${streamUrl.hashCode()}_$idx",
                label = "${resTriple.first} ($format)",
                resolution = "${resTriple.second}x${resTriple.third}",
                format = format,
                mimeType = resolvedMime,
                streamUrl = streamUrl,
                sizeBytes = headers.contentLength,
                bitrateLabel = when {
                    resTriple.third >= 1080 -> "Full HD Stream"
                    resTriple.third >= 720 -> "HD Stream"
                    else -> "Compact Mobile"
                }
            )
        }

        val bestVariant = variants.first()
        DetectedVideoStream(
            id = "${pageUrl}_${bestVariant.streamUrl}".hashCode().toString(),
            title = title.ifBlank { inferTitleFromUrl(bestVariant.streamUrl) },
            pageUrl = pageUrl,
            sourceDomain = domain,
            primaryStreamUrl = bestVariant.streamUrl,
            format = bestVariant.format,
            mimeType = bestVariant.mimeType,
            resolutionLabel = bestVariant.label.substringBefore(" ("),
            width = bestVariant.resolution.substringBefore("x").toIntOrNull() ?: 1280,
            height = bestVariant.resolution.substringAfter("x").toIntOrNull() ?: 720,
            durationSeconds = 0,
            contentLengthBytes = bestVariant.sizeBytes,
            qualityVariants = variants
        )
    }

    /**
     * Downloads the selected video stream variant directly into the isolated private vault partition
     * while emitting live progress updates to Room.
     */
    suspend fun downloadStreamToVault(
        stream: DetectedVideoStream,
        selectedVariant: QualityVariant,
        customTitle: String
    ): Long = withContext(Dispatchers.IO) {
        val vaultDir = getIsolatedVaultDirectory()
        val cleanTitle = customTitle.trim().ifBlank { stream.title.ifBlank { "Vault_Video" } }
        val safeFileName = cleanTitle
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .take(48)
            .trim('_')
            .ifBlank { "video" }
        val ext = selectedVariant.format.lowercase().let {
            if (it in listOf("mp4", "webm", "ogg", "mov", "mkv")) it else "mp4"
        }
        val targetFile = File(vaultDir, "${safeFileName}_${System.currentTimeMillis()}.$ext")

        val initialEntity = VaultVideoEntity(
            title = cleanTitle,
            sourceUrl = stream.pageUrl,
            sourceDomain = stream.sourceDomain,
            streamUrl = selectedVariant.streamUrl,
            localFilePath = targetFile.absolutePath,
            format = selectedVariant.format,
            mimeType = selectedVariant.mimeType,
            resolution = selectedVariant.resolution,
            durationSeconds = stream.durationSeconds,
            totalBytes = selectedVariant.sizeBytes.coerceAtLeast(0L),
            downloadedBytes = 0L,
            status = "DOWNLOADING"
        )
        val rowId = dao.insertVideo(initialEntity)

        try {
            val request = Request.Builder()
                .url(selectedVariant.streamUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) Chrome/124.0.0.0 Mobile Safari/537.36")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    dao.updateProgress(rowId, 0L, initialEntity.totalBytes, "FAILED")
                    return@withContext rowId
                }

                val body = response.body ?: run {
                    dao.updateProgress(rowId, 0L, initialEntity.totalBytes, "FAILED")
                    return@withContext rowId
                }

                val totalBytes = body.contentLength().takeIf { it > 0 } ?: selectedVariant.sizeBytes
                var downloaded = 0L
                var lastEmitTime = 0L

                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            val now = System.currentTimeMillis()
                            if (now - lastEmitTime >= 200) {
                                lastEmitTime = now
                                dao.updateProgress(
                                    id = rowId,
                                    downloadedBytes = downloaded,
                                    totalBytes = totalBytes.coerceAtLeast(downloaded),
                                    status = "DOWNLOADING"
                                )
                            }
                        }
                        output.flush()
                    }
                }

                // Inspect actual downloaded file metadata with MediaMetadataRetriever
                var actualResolution = selectedVariant.resolution
                var actualDurationSec = stream.durationSeconds
                runCatching {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(targetFile.absolutePath)
                    val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                    val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                    val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    if (w != null && h != null && w > 0 && h > 0) {
                        actualResolution = "${w}x${h}"
                    }
                    if (durMs != null && durMs > 0) {
                        actualDurationSec = (durMs / 1000L).toInt()
                    }
                    retriever.release()
                }

                val completedEntity = dao.getVideoById(rowId)?.copy(
                    totalBytes = downloaded,
                    downloadedBytes = downloaded,
                    resolution = actualResolution,
                    durationSeconds = actualDurationSec,
                    status = "COMPLETED"
                )
                if (completedEntity != null) {
                    dao.updateVideo(completedEntity)
                } else {
                    dao.updateProgress(rowId, downloaded, downloaded, "COMPLETED")
                }
            }
        } catch (e: Exception) {
            dao.updateProgress(rowId, 0L, initialEntity.totalBytes, "FAILED")
        }

        rowId
    }

    suspend fun deleteVaultVideo(video: VaultVideoEntity) = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(video.localFilePath)
            if (file.exists()) {
                file.delete()
            }
        }
        dao.deleteById(video.id)
    }

    /**
     * Exports a copy of a video from the private vault to the device's public Downloads folder.
     */
    suspend fun exportVideoToPublicDownloads(video: VaultVideoEntity): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val sourceFile = File(video.localFilePath)
            require(sourceFile.exists()) { "Vault file not found on disk" }
            val exportFileName = sourceFile.name

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, exportFileName)
                    put(MediaStore.Downloads.MIME_TYPE, video.mimeType.ifBlank { "video/mp4" })
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/FrameFetchVault")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, values)
                    ?: error("Could not create MediaStore entry")

                resolver.openOutputStream(itemUri)?.use { out ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(out)
                    }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
                "Downloads/FrameFetchVault/$exportFileName"
            } else {
                val downloadsDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "FrameFetchVault"
                )
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, exportFileName)
                sourceFile.copyTo(destFile, overwrite = true)
                destFile.absolutePath
            }
        }
    }

    private data class ProbedHeaders(
        val contentType: String,
        val contentLength: Long
    )

    private fun probeUrlHeaders(url: String): ProbedHeaders {
        return runCatching {
            val headReq = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) Chrome/124.0.0.0 Mobile Safari/537.36")
                .build()
            httpClient.newCall(headReq).execute().use { resp ->
                val type = resp.header("Content-Type")?.substringBefore(";")?.trim().orEmpty()
                val len = resp.header("Content-Length")?.toLongOrNull() ?: -1L
                if (len > 0 || type.isNotBlank()) {
                    return@runCatching ProbedHeaders(type, len.coerceAtLeast(0L))
                }
            }
            // Fallback to Range 0-0 GET if HEAD is rejected
            val rangeReq = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-0")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) Chrome/124.0.0.0 Mobile Safari/537.36")
                .build()
            httpClient.newCall(rangeReq).execute().use { resp ->
                val type = resp.header("Content-Type")?.substringBefore(";")?.trim().orEmpty()
                val contentRange = resp.header("Content-Range").orEmpty() // e.g. bytes 0-0/1580000
                val totalFromRange = contentRange.substringAfter("/", "").toLongOrNull()
                val len = totalFromRange ?: resp.header("Content-Length")?.toLongOrNull() ?: 0L
                ProbedHeaders(type, len.coerceAtLeast(0L))
            }
        }.getOrDefault(ProbedHeaders("", 0L))
    }

    private fun fetchHtmlSnippet(url: String): String {
        return runCatching {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) Chrome/124.0.0.0 Mobile Safari/537.36")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                val contentType = resp.header("Content-Type").orEmpty()
                if (contentType.contains("text/html", true) || contentType.isBlank()) {
                    resp.body?.string()?.take(250_000).orEmpty()
                } else {
                    ""
                }
            }
        }.getOrDefault("")
    }

    private fun extractHtmlTitle(html: String): String? {
        val match = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)
        return match?.groupValues?.getOrNull(1)?.replace(Regex("\\s+"), " ")?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun extractVideoUrlsFromHtml(html: String, baseUrl: String): List<String> {
        val found = linkedSetOf<String>()
        // 1. og:video or twitter:player:stream
        val metaRegex = Regex(
            """<meta[^>]+(?:property|name)=["'](?:og:video|og:video:url|og:video:secure_url|twitter:player:stream)["'][^>]+content=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE
        )
        metaRegex.findAll(html).forEach { m ->
            resolveRelativeUrl(baseUrl, m.groupValues[1])?.let { found.add(it) }
        }

        // 2. <video src="..."> and <source src="...">
        val srcRegex = Regex(
            """<(?:video|source)[^>]+src=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE
        )
        srcRegex.findAll(html).forEach { m ->
            resolveRelativeUrl(baseUrl, m.groupValues[1])?.let { found.add(it) }
        }

        // 3. Direct .mp4 / .webm URLs inside JSON or page script
        val directRegex = Regex(
            """https?://[^\s"'<>\\]+\.(?:mp4|webm|m4v|mov)(?:\?[^\s"'<>\\]*)?""",
            RegexOption.IGNORE_CASE
        )
        directRegex.findAll(html).forEach { m ->
            found.add(m.value)
        }

        return found.take(6)
    }

    private fun resolveRelativeUrl(baseUrl: String, candidate: String): String? {
        val trimmed = candidate.trim()
        if (trimmed.isBlank() || trimmed.startsWith("blob:") || trimmed.startsWith("data:")) return null
        return runCatching {
            URI(baseUrl).resolve(trimmed).toString()
        }.getOrNull()
    }

    private fun isVideoMimeOrExtension(url: String, contentType: String): Boolean {
        val lowerType = contentType.lowercase()
        if (lowerType.startsWith("video/") || lowerType.contains("mpegurl") || lowerType.contains("dash+xml")) {
            return true
        }
        val cleanPath = url.substringBefore("?").lowercase()
        return cleanPath.endsWith(".mp4") ||
            cleanPath.endsWith(".webm") ||
            cleanPath.endsWith(".ogg") ||
            cleanPath.endsWith(".ogv") ||
            cleanPath.endsWith(".mov") ||
            cleanPath.endsWith(".m4v") ||
            cleanPath.endsWith(".mkv") ||
            cleanPath.endsWith(".m3u8")
    }

    private fun inferFormatLabel(url: String, contentType: String): String {
        val cleanPath = url.substringBefore("?").lowercase()
        return when {
            cleanPath.endsWith(".webm") || contentType.contains("webm", true) -> "WEBM"
            cleanPath.endsWith(".ogg") || cleanPath.endsWith(".ogv") || contentType.contains("ogg", true) -> "OGG"
            cleanPath.endsWith(".mov") || contentType.contains("quicktime", true) -> "MOV"
            cleanPath.endsWith(".mkv") || contentType.contains("matroska", true) -> "MKV"
            cleanPath.endsWith(".m3u8") || contentType.contains("mpegurl", true) -> "HLS"
            else -> "MP4"
        }
    }

    private fun parseResHint(hint: String): Triple<String, Int, Int> {
        val lower = hint.lowercase()
        return when {
            lower.contains("1080") -> Triple("1080p Full HD", 1920, 1080)
            lower.contains("720") -> Triple("720p HD", 1280, 720)
            lower.contains("480") -> Triple("480p SD", 854, 480)
            lower.contains("360") -> Triple("360p Compact", 640, 360)
            else -> Triple(hint, 1280, 720)
        }
    }

    private fun inferResolutionFromUrl(url: String, variantIndex: Int = 0): Triple<String, Int, Int> {
        val lower = url.lowercase()
        return when {
            lower.contains("1080") || lower.contains("fhd") -> Triple("1080p Full HD", 1920, 1080)
            lower.contains("720") || lower.contains("hd") -> Triple("720p HD", 1280, 720)
            lower.contains("480") || lower.contains("sd") -> Triple("480p SD", 854, 480)
            lower.contains("360") || lower.contains("ogg") -> Triple("360p Compact", 640, 360)
            variantIndex == 0 -> Triple("720p HD", 1280, 720)
            variantIndex == 1 -> Triple("480p SD", 854, 480)
            else -> Triple("360p Compact", 640, 360)
        }
    }

    private fun inferTitleFromUrl(url: String): String {
        val lastSeg = url.substringBefore("?").substringAfterLast("/").substringBeforeLast(".")
        return if (lastSeg.isNotBlank()) {
            Uri.decode(lastSeg).replace(Regex("[_-]+"), " ").trim()
        } else {
            "Extracted Web Video"
        }
    }

    private fun extractDomain(url: String): String {
        return runCatching {
            URI(url).host?.removePrefix("www.") ?: "web.stream"
        }.getOrDefault("web.stream")
    }
}
