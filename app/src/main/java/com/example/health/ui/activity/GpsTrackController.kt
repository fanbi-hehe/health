package com.example.health.ui.activity

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.health.worker.GpsTrackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * GPS 运动记录控制器：UI 与 [GpsTrackService] 之间的桥梁。
 */
object GpsTrackController {

    data class TrackState(
        val recording: Boolean = false,
        val type: String = "running",
        val startTime: Long = 0L,
        val durationSeconds: Int = 0,
        val distanceMeters: Double = 0.0,
        val caloriesKcal: Int = 0,
        val pointCount: Int = 0
    )

    private val _state = MutableStateFlow(TrackState())
    val state: StateFlow<TrackState> = _state.asStateFlow()

    /** 仅供 GpsTrackService 内部回写状态。 */
    internal fun updateState(newState: TrackState) {
        _state.value = newState
    }

    fun start(context: Context, type: String) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, GpsTrackService::class.java)
                .setAction(GpsTrackService.ACTION_START)
                .putExtra(GpsTrackService.EXTRA_TYPE, type)
        )
    }

    fun stop(context: Context) {
        context.startService(
            Intent(context, GpsTrackService::class.java)
                .setAction(GpsTrackService.ACTION_STOP)
        )
    }
}
