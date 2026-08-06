package com.example.health.ui.activity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.ActivityRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 运动记录页 ViewModel：
 * GPS 记录状态桥接 + 今日消耗汇总 + 手动补录 + 历史管理。
 */
class ActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).activityRecordDao()

    /** GPS 记录实时状态（由 GpsTrackService 驱动）。 */
    val trackState: StateFlow<GpsTrackController.TrackState> = GpsTrackController.state

    /** 全部运动记录（按开始时间倒序）。 */
    val records: StateFlow<List<ActivityRecord>> = dao.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** 今日运动消耗（kcal）。 */
    private val _todayCalories = MutableStateFlow(0)
    val todayCalories: StateFlow<Int> = _todayCalories.asStateFlow()

    init {
        viewModelScope.launch {
            records.collect { list ->
                val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                _todayCalories.value = list
                    .filter { sdf.format(java.util.Date(it.startTime)) == todayStr }
                    .sumOf { it.caloriesKcal }
            }
        }
    }

    fun startTracking(type: String) {
        GpsTrackController.start(getApplication(), type)
    }

    fun stopTracking() {
        GpsTrackController.stop(getApplication())
    }

    /** 手动补录运动消耗。 */
    fun addManualRecord(durationMinutes: Int, caloriesKcal: Int, note: String?) {
        viewModelScope.launch {
            dao.insert(
                ActivityRecord(
                    type = "manual",
                    startTime = System.currentTimeMillis(),
                    durationMinutes = durationMinutes,
                    caloriesKcal = caloriesKcal,
                    source = "manual",
                    note = note
                )
            )
        }
    }

    fun deleteRecord(record: ActivityRecord) {
        viewModelScope.launch { dao.delete(record) }
    }
}
