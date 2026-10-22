package com.example.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.BorderState
import com.example.data.DetectedVideoStream
import com.example.data.QualityVariant
import com.example.data.VaultDatabase
import com.example.data.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Persistent Foreground Overlay Service that keeps the glowing perimeter border and
 * floating video extractor HUD active over the Home screen and all other apps even
 * when FrameFetch Vault is minimized.
 */
class BorderOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var windowManager: WindowManager? = null
    private var borderFrameView: PerimeterOverlayView? = null
    private var floatingControlContainer: LinearLayout? = null
    private var expandedDrawerContainer: LinearLayout? = null
    private var statusBadgeText: TextView? = null
    private var actionButtonText: TextView? = null
    private var drawerSubTitleText: TextView? = null
    private var drawerVariantsContainer: LinearLayout? = null

    private lateinit var repository: VaultRepository
    private var latestProbedStream: DetectedVideoStream? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val db = VaultDatabase.getInstance(applicationContext)
        repository = VaultRepository(applicationContext, db.vaultVideoDao())

        if (!Settings.canDrawOverlays(this)) {
            BorderOverlayStateBus.setOverlayRunning(false)
            stopSelf()
            return
        }

        promoteToForegroundService()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        attachSystemBorderOverlay()
        attachFloatingExtractorPill()
        BorderOverlayStateBus.setOverlayRunning(running = true, explicitlyDisabled = false)

        // Seed default stream for quick overlay download testing
        seedOverlayDefaultStream()

        serviceScope.launch {
            BorderOverlayStateBus.state.collectLatest { state ->
                borderFrameView?.updateVisualState(
                    borderState = state.borderState,
                    strokeDp = state.borderStrokeDp,
                    showBrackets = state.showCornerBrackets
                )
                updatePillText(state)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_OVERLAY) {
            BorderOverlayStateBus.setOverlayRunning(running = false, explicitlyDisabled = true)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            BorderOverlayStateBus.setOverlayRunning(false)
            stopSelf()
            return START_NOT_STICKY
        }
        promoteToForegroundService()
        if (borderFrameView == null) {
            attachSystemBorderOverlay()
        }
        if (floatingControlContainer == null) {
            attachFloatingExtractorPill()
        }
        BorderOverlayStateBus.setOverlayRunning(running = true, explicitlyDisabled = false)
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Keep the border overlay alive even if the user minimizes or swipes the activity away
        if (Settings.canDrawOverlays(this) && !BorderOverlayStateBus.state.value.userExplicitlyDisabled) {
            val restartIntent = Intent(applicationContext, BorderOverlayService::class.java)
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    applicationContext.startForegroundService(restartIntent)
                } else {
                    applicationContext.startService(restartIntent)
                }
            }
        }
    }

    private fun promoteToForegroundService() {
        val channelId = "framefetch_border_overlay_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Active Screen Border Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the FrameFetch video extraction border active over your Home screen and apps"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            101,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, BorderOverlayService::class.java).apply {
            action = ACTION_STOP_OVERLAY
        }
        val pendingStop = PendingIntent.getService(
            this,
            102,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("FrameFetch Border Active Over Screen")
            .setContentText("Tap floating border pill over any app to extract & download videos to Vault")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingOpen)
            .addAction(0, "Open Vault", pendingOpen)
            .addAction(0, "Stop Border", pendingStop)
            .build()

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun seedOverlayDefaultStream() {
        latestProbedStream = DetectedVideoStream(
            id = "overlay_ready_hd",
            title = "For Bigger Blazes — HD Stream",
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
                    id = "ov_720p",
                    label = "720p HD (MP4 • 2.4 MB)",
                    resolution = "1280x720",
                    format = "MP4",
                    mimeType = "video/mp4",
                    streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    sizeBytes = 2_498_185L,
                    bitrateLabel = "HD Stream"
                ),
                QualityVariant(
                    id = "ov_480p",
                    label = "480p Fast Clip (MP4 • 770 KB)",
                    resolution = "854x480",
                    format = "MP4",
                    mimeType = "video/mp4",
                    streamUrl = "https://www.w3schools.com/html/mov_bbb.mp4",
                    sizeBytes = 788_493L,
                    bitrateLabel = "SD Fast Stream"
                )
            )
        )
    }

    private fun attachSystemBorderOverlay() {
        val wm = windowManager ?: return
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // Pass-through border frame: does not intercept touches, so the user uses their normal mobile phone inside the border
        val frameParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val view = PerimeterOverlayView(this)
        runCatching {
            wm.addView(view, frameParams)
            borderFrameView = view
        }
    }

    private fun attachFloatingExtractorPill() {
        val wm = windowManager ?: return
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val pillParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dpToPx(12f)
            y = dpToPx(96f)
        }

        val rootStack = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
        }

        // Top compact pill bar
        val pillBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(12f), dpToPx(8f), dpToPx(10f), dpToPx(8f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(24f).toFloat()
                setColor(Color.parseColor("#F00A0E14"))
                setStroke(dpToPx(1.5f), Color.parseColor("#00E5FF"))
            }
            elevation = dpToPx(10f).toFloat()
        }

        val statusTv = TextView(this).apply {
            text = "● BORDER ACTIVE"
            setTextColor(Color.parseColor("#00E5FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, dpToPx(8f), 0)
        }

        val extractBtn = TextView(this).apply {
            text = "Sniff / Download"
            setTextColor(Color.parseColor("#041E10"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dpToPx(10f), dpToPx(5f), dpToPx(10f), dpToPx(5f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(16f).toFloat()
                setColor(Color.parseColor("#00E676"))
            }
            setOnClickListener {
                toggleExpandedOverlayExtractor()
            }
        }

        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#9BA9BC"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dpToPx(8f), dpToPx(2f), dpToPx(4f), dpToPx(2f))
            setOnClickListener {
                BorderOverlayStateBus.setOverlayRunning(running = false, explicitlyDisabled = true)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        pillBar.addView(statusTv)
        pillBar.addView(extractBtn)
        pillBar.addView(closeBtn)

        // Expandable in-overlay Quick Video Extractor Drawer (works right over the Home screen & other apps!)
        val drawer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dpToPx(14f), dpToPx(12f), dpToPx(14f), dpToPx(12f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(18f).toFloat()
                setColor(Color.parseColor("#F5101722"))
                setStroke(dpToPx(1.5f), Color.parseColor("#00E676"))
            }
            val lp = LinearLayout.LayoutParams(dpToPx(270f), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpToPx(8f)
            }
            layoutParams = lp
        }

        val drawerHeader = TextView(this).apply {
            text = "BORDER STREAM EXTRACTOR"
            setTextColor(Color.parseColor("#00E5FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
        }

        val drawerSubtitle = TextView(this).apply {
            text = "Ready over current screen • Tap a quality below to download directly to Separate Vault:"
            setTextColor(Color.parseColor("#D5E1F0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            setPadding(0, dpToPx(4f), 0, dpToPx(8f))
        }
        drawerSubTitleText = drawerSubtitle

        val variantsBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        drawerVariantsContainer = variantsBox

        val bottomActionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(8f), 0, 0)
        }

        val sniffClipboardBtn = TextView(this).apply {
            text = "Sniff Copied Link"
            setTextColor(Color.parseColor("#00E5FF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dpToPx(10f), dpToPx(6f), dpToPx(10f), dpToPx(6f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(10f).toFloat()
                setColor(Color.parseColor("#1A2636"))
                setStroke(dpToPx(1f), Color.parseColor("#00E5FF"))
            }
            setOnClickListener {
                sniffClipboardInOverlay()
            }
        }

        val openAppBtn = TextView(this).apply {
            text = "Open Browser / Vault"
            setTextColor(Color.parseColor("#F2F6FC"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dpToPx(10f), dpToPx(6f), dpToPx(10f), dpToPx(6f))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(10f).toFloat()
                setColor(Color.parseColor("#223044"))
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = dpToPx(8f)
            }
            layoutParams = lp
            setOnClickListener {
                drawer.visibility = View.GONE
                val launchIntent = Intent(this@BorderOverlayService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(launchIntent)
            }
        }

        bottomActionsRow.addView(sniffClipboardBtn)
        bottomActionsRow.addView(openAppBtn)

        drawer.addView(drawerHeader)
        drawer.addView(drawerSubtitle)
        drawer.addView(variantsBox)
        drawer.addView(bottomActionsRow)

        rootStack.addView(pillBar)
        rootStack.addView(drawer)

        // Allow dragging the floating pill anywhere along the screen while using other apps
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        pillBar.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = pillParams.x
                    initialY = pillParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (initialTouchX - event.rawX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) {
                        isDragging = true
                        pillParams.x = (initialX + dx).coerceAtLeast(0)
                        pillParams.y = (initialY + dy).coerceAtLeast(dpToPx(32f))
                        runCatching { wm.updateViewLayout(rootStack, pillParams) }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        toggleExpandedOverlayExtractor()
                    }
                    true
                }
                else -> false
            }
        }

        runCatching {
            wm.addView(rootStack, pillParams)
            floatingControlContainer = rootStack
            expandedDrawerContainer = drawer
            statusBadgeText = statusTv
            actionButtonText = extractBtn
            rebuildDrawerVariantButtons()
        }
    }

    private fun toggleExpandedOverlayExtractor() {
        val drawer = expandedDrawerContainer ?: return
        if (drawer.visibility == View.VISIBLE) {
            drawer.visibility = View.GONE
        } else {
            // Check clipboard automatically when expanding over other apps
            val clipUrl = readHttpUrlFromClipboard()
            if (clipUrl != null) {
                sniffUrlInOverlay(clipUrl)
            } else {
                rebuildDrawerVariantButtons()
            }
            drawer.visibility = View.VISIBLE
        }
    }

    private fun sniffClipboardInOverlay() {
        val clipUrl = readHttpUrlFromClipboard()
        if (clipUrl != null) {
            sniffUrlInOverlay(clipUrl)
        } else {
            Toast.makeText(
                this,
                "Copy any video link from an app or browser, or tap a quality option below to download!",
                Toast.LENGTH_SHORT
            ).show()
            rebuildDrawerVariantButtons()
        }
    }

    private fun sniffUrlInOverlay(url: String) {
        drawerSubTitleText?.text = "Probing video stream: $url"
        BorderOverlayStateBus.updateBorderStatus(BorderState.SCANNING)
        serviceScope.launch {
            val found = repository.probeStreamOrPageUrl(url)
            if (found.isNotEmpty()) {
                latestProbedStream = found.first()
                BorderOverlayStateBus.updateBorderStatus(
                    borderState = BorderState.VIDEO_DETECTED,
                    detectedCount = found.size,
                    latestTitle = found.first().title
                )
                rebuildDrawerVariantButtons()
            } else {
                drawerSubTitleText?.text = "Link requires full DOM sniffer — opening inside Framed Browser…"
                BorderOverlayStateBus.requestExtractExternalUrl(url)
                val launchIntent = Intent(this@BorderOverlayService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(EXTRA_CAPTURED_URL, url)
                    putExtra(EXTRA_OPEN_STREAM_INSPECTOR, true)
                }
                startActivity(launchIntent)
            }
        }
    }

    private fun rebuildDrawerVariantButtons() {
        val container = drawerVariantsContainer ?: return
        container.removeAllViews()
        val stream = latestProbedStream ?: return
        drawerSubTitleText?.text = "Detected: ${stream.title}\nTap resolution to save to Separate Vault:"

        stream.qualityVariants.take(3).forEach { variant ->
            val btn = TextView(this).apply {
                text = "⬇ Download ${variant.label} to Vault"
                setTextColor(Color.parseColor("#031B0E"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dpToPx(12f), dpToPx(8f), dpToPx(12f), dpToPx(8f))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(10f).toFloat()
                    setColor(Color.parseColor("#00E676"))
                }
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dpToPx(6f)
                }
                layoutParams = lp
                setOnClickListener {
                    expandedDrawerContainer?.visibility = View.GONE
                    BorderOverlayStateBus.updateBorderStatus(BorderState.DOWNLOADING)
                    Toast.makeText(
                        this@BorderOverlayService,
                        "Downloading '${stream.title}' (${variant.label}) to Separate Vault…",
                        Toast.LENGTH_SHORT
                    ).show()
                    serviceScope.launch {
                        repository.downloadStreamToVault(
                            stream = stream,
                            selectedVariant = variant,
                            customTitle = stream.title
                        )
                        BorderOverlayStateBus.updateBorderStatus(BorderState.VIDEO_DETECTED)
                        Toast.makeText(
                            this@BorderOverlayService,
                            "Saved to Isolated Private Vault!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            container.addView(btn)
        }
    }

    private fun readHttpUrlFromClipboard(): String? {
        return runCatching {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val text = cm?.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim().orEmpty()
            val match = Regex("https?://[^\\s\"'<>]+", RegexOption.IGNORE_CASE).find(text)
            match?.value
        }.getOrNull()
    }

    private fun updatePillText(state: OverlayRuntimeState) {
        val statusTv = statusBadgeText ?: return
        val actionTv = actionButtonText ?: return

        val (statusLabel, colorHex, actionLabel) = when (state.borderState) {
            BorderState.VIDEO_DETECTED -> Triple(
                "● ${state.detectedCount.coerceAtLeast(1)} VIDEO READY",
                "#00E676",
                "Download (${state.detectedCount.coerceAtLeast(1)})"
            )
            BorderState.DOWNLOADING -> Triple(
                "● SAVING TO VAULT",
                "#00E5FF",
                "Downloading…"
            )
            BorderState.SCANNING -> Triple(
                "● SCANNING FRAME",
                "#FFB300",
                "Inspect"
            )
            BorderState.IDLE -> Triple(
                "● BORDER ACTIVE",
                "#00E5FF",
                "Extract / Vault"
            )
        }

        statusTv.text = statusLabel
        statusTv.setTextColor(Color.parseColor(colorHex))
        actionTv.text = actionLabel
    }

    override fun onDestroy() {
        BorderOverlayStateBus.setOverlayRunning(false)
        serviceScope.cancel()
        val wm = windowManager
        borderFrameView?.let { view ->
            runCatching { wm?.removeView(view) }
        }
        floatingControlContainer?.let { view ->
            runCatching { wm?.removeView(view) }
        }
        borderFrameView = null
        floatingControlContainer = null
        expandedDrawerContainer = null
        super.onDestroy()
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }

    /**
     * Custom View that draws the active viewfinder border around the entire phone screen.
     */
    private class PerimeterOverlayView(context: Context) : View(context) {
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#00E5FF")
        }
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#3800E5FF")
        }
        private val bracketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#00E676")
        }
        private val rect = RectF()
        private var showCornerBrackets = true

        init {
            updateVisualState(BorderState.IDLE, 4f, true)
        }

        fun updateVisualState(borderState: BorderState, strokeDp: Float, showBrackets: Boolean) {
            val density = resources.displayMetrics.density
            val pxStroke = strokeDp * density
            borderPaint.strokeWidth = pxStroke
            glowPaint.strokeWidth = pxStroke * 2.6f
            bracketPaint.strokeWidth = (strokeDp + 2f) * density
            showCornerBrackets = showBrackets

            when (borderState) {
                BorderState.VIDEO_DETECTED -> {
                    borderPaint.color = Color.parseColor("#00E676")
                    glowPaint.color = Color.parseColor("#4500E676")
                    bracketPaint.color = Color.parseColor("#00E5FF")
                }
                BorderState.DOWNLOADING -> {
                    borderPaint.color = Color.parseColor("#00E5FF")
                    glowPaint.color = Color.parseColor("#5500E5FF")
                    bracketPaint.color = Color.parseColor("#00E676")
                }
                BorderState.SCANNING -> {
                    borderPaint.color = Color.parseColor("#FFB300")
                    glowPaint.color = Color.parseColor("#40FFB300")
                    bracketPaint.color = Color.parseColor("#00E5FF")
                }
                BorderState.IDLE -> {
                    borderPaint.color = Color.parseColor("#00E5FF")
                    glowPaint.color = Color.parseColor("#3300E5FF")
                    bracketPaint.color = Color.parseColor("#00E676")
                }
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val inset = borderPaint.strokeWidth / 2f + 4f
            rect.set(inset, inset, width - inset, height - inset)
            val cornerRadius = 32f * resources.displayMetrics.density

            // Outer soft glow + crisp perimeter border
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glowPaint)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

            if (showCornerBrackets) {
                val len = 28f * resources.displayMetrics.density
                val bInset = inset + 6f
                // Top-left bracket
                canvas.drawLine(bInset, bInset + len, bInset, bInset, bracketPaint)
                canvas.drawLine(bInset, bInset, bInset + len, bInset, bracketPaint)
                // Top-right bracket
                canvas.drawLine(width - bInset - len, bInset, width - bInset, bInset, bracketPaint)
                canvas.drawLine(width - bInset, bInset, width - bInset, bInset + len, bracketPaint)
                // Bottom-left bracket
                canvas.drawLine(bInset, height - bInset - len, bInset, height - bInset, bracketPaint)
                canvas.drawLine(bInset, height - bInset, bInset + len, height - bInset, bracketPaint)
                // Bottom-right bracket
                canvas.drawLine(width - bInset - len, height - bInset, width - bInset, height - bInset, bracketPaint)
                canvas.drawLine(width - bInset, height - bInset - len, width - bInset, height - bInset, bracketPaint)
            }
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 4401
        const val ACTION_STOP_OVERLAY = "com.example.overlay.ACTION_STOP_OVERLAY"
        const val EXTRA_CAPTURED_URL = "extra_captured_url"
        const val EXTRA_OPEN_STREAM_INSPECTOR = "extra_open_stream_inspector"
    }
}
