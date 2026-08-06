package com.example.health.ui.training

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.health.worker.RestTimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 组间休息倒计时控制器。
 *
 * UI 与 [RestTimerService] 之间的桥梁：
 * - UI 只通过这里发命令（开始/暂停/继续/调整/重置/结束）
 * - Service 通过 [updateState] 回写状态，UI 订阅 [state] 渲染
 * - Service 常驻后台，退出页面/锁屏后计时仍继续
 */
object RestTimerController {

    data class State(
        val running: Boolean = false,
        val remainingSeconds: Int = 60,
        val totalSeconds: Int = 60
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    /** 仅供 RestTimerService 内部回写状态。 */
    internal fun updateState(newState: State) {
        _state.value = newState
    }

    fun start(context: Context, totalSeconds: Int) {
        ContextCompat.startForegroundService(
            context,
            Intent(context, RestTimerService::class.java)
                .setAction(RestTimerService.ACTION_START)
                .putExtra(RestTimerService.EXTRA_SECONDS, totalSeconds)
        )
    }

    fun pause(context: Context) {
        context.startService(
            Intent(context, RestTimerService::class.java)
                .setAction(RestTimerService.ACTION_PAUSE)
        )
    }

    fun resume(context: Context) {
        context.startService(
            Intent(context, RestTimerService::class.java)
                .setAction(RestTimerService.ACTION_RESUME)
        )
    }

    fun adjust(context: Context, delta: Int, currentTotal: Int) {
        val newTotal = (currentTotal + delta).coerceIn(15, 180)
        context.startService(
            Intent(context, RestTimerService::class.java)
                .setAction(RestTimerService.ACTION_ADJUST)
                .putExtra(RestTimerService.EXTRA_SECONDS, newTotal)
        )
    }

    fun reset(context: Context) {
        context.startService(
            Intent(context, RestTimerService::class.java)
                .setAction(RestTimerService.ACTION_RESET)
        )
    }

    fun stop(context: Context) {
        context.startService(
            Intent(context, RestTimerService::class.java)
                .setAction(RestTimerService.ACTION_STOP)
        )
    }
}
