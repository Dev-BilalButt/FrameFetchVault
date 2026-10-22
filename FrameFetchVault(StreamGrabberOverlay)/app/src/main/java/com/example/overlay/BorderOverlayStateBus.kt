package com.example.overlay

import com.example.data.BorderState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OverlayRuntimeState(
    val isOverlayRunning: Boolean = false,
    val userExplicitlyDisabled: Boolean = false,
    val borderState: BorderState = BorderState.IDLE,
    val detectedCount: Int = 0,
    val latestDetectedTitle: String = "",
    val borderStrokeDp: Float = 4f,
    val showCornerBrackets: Boolean = true,
    val pendingExternalUrlToExtract: String? = null
)

/**
 * Shared reactive state bus between the System-Wide WindowManager Overlay Service
 * and the main Framed Activity / ViewModel.
 */
object BorderOverlayStateBus {
    private val _state = MutableStateFlow(OverlayRuntimeState())
    val state: StateFlow<OverlayRuntimeState> = _state.asStateFlow()

    fun setOverlayRunning(running: Boolean, explicitlyDisabled: Boolean = _state.value.userExplicitlyDisabled) {
        _state.update {
            it.copy(
                isOverlayRunning = running,
                userExplicitlyDisabled = explicitlyDisabled
            )
        }
    }

    fun updateBorderStatus(
        borderState: BorderState,
        detectedCount: Int = _state.value.detectedCount,
        latestTitle: String = _state.value.latestDetectedTitle
    ) {
        _state.update {
            it.copy(
                borderState = borderState,
                detectedCount = detectedCount,
                latestTitle = latestTitle
            )
        }
    }

    private fun OverlayRuntimeState.copy(
        borderState: BorderState,
        detectedCount: Int,
        latestTitle: String
    ): OverlayRuntimeState {
        return copy(
            borderState = borderState,
            detectedCount = detectedCount,
            latestDetectedTitle = latestTitle
        )
    }

    fun updateBorderCustomization(strokeDp: Float, showBrackets: Boolean) {
        _state.update {
            it.copy(
                borderStrokeDp = strokeDp.coerceIn(2f, 10f),
                showCornerBrackets = showBrackets
            )
        }
    }

    fun requestExtractExternalUrl(url: String) {
        if (url.isNotBlank()) {
            _state.update { it.copy(pendingExternalUrlToExtract = url.trim()) }
        }
    }

    fun consumePendingExternalUrl(): String? {
        val current = _state.value.pendingExternalUrlToExtract
        if (current != null) {
            _state.update { it.copy(pendingExternalUrlToExtract = null) }
        }
        return current
    }
}
