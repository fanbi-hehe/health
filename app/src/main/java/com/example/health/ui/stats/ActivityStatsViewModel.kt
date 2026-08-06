package com.example.health.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.ActivityRecord
import com.example.health.data.local.entity.DailyStepCount
import com.example.health.util.StepCounterManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 运动统计页 ViewModel：步数统计 + 运动统计（今日/近7天/本周）。
 */
class ActivityStatsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)

    /** 每日步数（日期倒序）。 */
    val stepCounts: StateFlow<List<DailyStepCount>> = db.dailyStepCountDao().getAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 运动记录（开始时间倒序）。 */
    val records: StateFlow<List<ActivityRecord>> = db.activityRecordDao().getAllRecords()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    private val _todayStepCalories = MutableStateFlow(0)
    val todayStepCalories: StateFlow<Int> = _todayStepCalories.asStateFlow()

    private val _todayActivityCalories = MutableStateFlow(0)
    val todayActivityCalories: StateFlow<Int> = _todayActivityCalories.asStateFlow()

    data class WeekStats(
        val sessions: Int = 0,
        val minutes: Int = 0,
        val distanceMeters: Double = 0.0,
        val calories: Int = 0
    )

    private val _weekStats = MutableStateFlow(WeekStats())
    val weekStats: StateFlow<WeekStats> = _weekStats.asStateFlow()

    init {
        viewModelScope.launch {
            stepCounts.collect { list ->
                val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val today = list.firstOrNull { it.date == todayStr }
                _todaySteps.value = today?.steps ?: 0
                _todayStepCalories.value = today?.caloriesKcal ?: 0
            }
        }
        viewModelScope.launch {
            records.collect { list ->
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val weekStart = LocalDate.now().minusDays(6).format(DateTimeFormatter.ISO_LOCAL_DATE)

                _todayActivityCalories.value = list
                    .filter { sdf.format(Date(it.startTime)) == todayStr }
                    .sumOf { it.caloriesKcal }

                val weekRecords = list.filter {
                    val d = sdf.format(Date(it.startTime))
                    d in weekStart..todayStr
                }
                _weekStats.value = WeekStats(
                    sessions = weekRecords.size,
                    minutes = weekRecords.sumOf { it.durationMinutes },
                    distanceMeters = weekRecords.sumOf { it.distanceMeters },
                    calories = weekRecords.sumOf { it.caloriesKcal }
                )
            }
        }
    }

    /** 即时同步系统步数。 */
    fun syncSteps() {
        viewModelScope.launch {
            StepCounterManager.syncNow(getApplication())
        }
    }

    fun deleteRecord(record: ActivityRecord) {
        viewModelScope.launch {
            db.activityRecordDao().delete(record)
        }
    }
}
