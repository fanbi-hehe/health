package com.example.health.ui.training

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.ExerciseLibrary
import com.example.health.data.local.entity.TrainingRecord
import com.example.health.data.preference.AppPreferences
import com.example.health.data.repository.AiRepository
import com.example.health.data.repository.ExerciseRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TrainingViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).trainingRecordDao()
    private val exerciseRepo = ExerciseRepository(application)
    private val prefs = AppPreferences(application)
    private val aiRepo = AiRepository(application)
    private val gson = Gson()

    // 动态获取当天日期（修复跨天不刷新问题）
    private val todayDate: String get() = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    private val now: Long get() = System.currentTimeMillis()

    // ── 今日训练记录 ──
    val todayRecords: StateFlow<List<TrainingRecord>> = dao.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── 动作库 ──
    val allExercises: StateFlow<List<ExerciseLibrary>> = exerciseRepo.getAllExercises()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── 按部位筛选的历史动作名 ──
    private val _historyExercises = MutableStateFlow<List<String>>(emptyList())
    val historyExercises: StateFlow<List<String>> = _historyExercises.asStateFlow()

    /**
     * 选择部位后查询该部位的历史动作。
     */
    fun loadHistoryExercises(bodyPart: String) {
        viewModelScope.launch {
            _historyExercises.value = dao.getDistinctExercisesByBodyPart(bodyPart)
        }
    }

    // ── 训练计划 ──
    val planJson: StateFlow<String> = prefs.trainingPlanJson
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    private val _planError = MutableStateFlow<String?>(null)
    val planError: StateFlow<String?> = _planError.asStateFlow()

    // ── 用户档案 ──
    val isOnboarded: StateFlow<Boolean> = prefs.userOnboarded
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun saveUserProfile(height: Int, weight: Double, goal: String, experience: String, equipment: String, days: Int) {
        viewModelScope.launch { prefs.setUserProfile(height, weight, goal, experience, equipment, days) }
    }

    fun completePlanExercise(name: String, sets: Int, reps: String, weightKg: Double) {
        viewModelScope.launch {
            val repsInt = reps.toIntOrNull() ?: 0
            val ts = now
            dao.insert(TrainingRecord(
                date = todayDate, timestamp = ts, bodyParts = "", exerciseName = name,
                sets = sets, reps = repsInt, weightKg = weightKg,
                notes = "计划完成"
            ))
        }
    }

    fun generatePlan(customPrompt: String = "") {
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val height: Int = prefs.userHeight.first()
                val weight: Double = prefs.userCurrentWeight.first()
                val goal: String = prefs.userGoal.first()
                val experience: String = prefs.userExperience.first()
                val equipment: String = prefs.userEquipment.first()
                val trainingDays: Int = prefs.userTrainingDays.first()

                val exercises = exerciseRepo.getAllExercises()
                    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value
                val history = dao.getAllRecords()
                    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()).value

                val exerciseNames = exercises.take(60).joinToString("、") { it.name }
                val recentTraining = history.take(15).joinToString("\n") {
                    "${it.date} ${it.bodyParts} ${it.exerciseName} ${it.sets}×${it.reps} ${it.weightKg}kg"
                }
                val today = LocalDate.now()
                val todayStr = today.format(DateTimeFormatter.ofPattern("MM月dd日"))
                val dayOfWeek = today.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.CHINESE)
                val fmt = DateTimeFormatter.ofPattern("MM-dd")

                val prompt = buildString {
                    append("你是一位专业健身教练。请根据以下用户信息制定7天训练计划。\n\n")
                    append("当前日期：${todayStr} ${dayOfWeek}\n\n")
                    append("用户档案：\n")
                    append("- 身高: ${height}cm\n")
                    append("- 体重: ${weight}kg\n")
                    append("- 目标: $goal\n")
                    append("- 训练经验: $experience\n")
                    append("- 可用器材: ${equipment.ifBlank { "哑铃、杠铃、自重" }}\n")
                    append("- 每周训练天数: $trainingDays\n\n")
                    if (customPrompt.isNotBlank()) {
                        append("用户自定义需求：$customPrompt\n\n")
                    }
                    if (recentTraining.isNotBlank()) {
                        append("最近训练记录：\n$recentTraining\n\n")
                    }
                    append("可选动作库：$exerciseNames\n\n")
                    append("要求：返回纯JSON数组，每天一个对象，7天。\n")
                    append("格式：[{\"day\":\"${today.format(fmt)} 周X\",\"date\":\"MM-dd\",\"focus\":\"训练部位\",\"exercises\":")
                    append("[{\"name\":\"动作名\",\"sets\":3,\"reps\":\"8-12\",\"notes\":\"备注\"}]}]\n")
                    append("day 字段格式必须为\"MM-dd 周X\"，如\"${today.format(fmt)} ${dayOfWeek.replace("星期", "周")}\"。\n")
                    append("date 字段为短日期如\"${today.format(fmt)}\"。\n")
                    append("休息日 focus 写\"休息日\"，exercises 为空数组。\n")
                    append("合理分配部位，符合每周${trainingDays}天训练的安排。从今天开始，连续7天。只返回JSON！")
                }

                val result = aiRepo.chatCompletion(prompt, null, emptyList(), maxTokens = 4096)
                var parsed = false
                result.fold(
                    onSuccess = { json ->
                        val cleaned = json
                            .replace(Regex("```\\w*\\n?"), "").replace("```", "").trim()
                        val start = cleaned.indexOf('[')
                        val end = cleaned.lastIndexOf(']')
                        val jsonStr = if (start >= 0 && end > start) cleaned.substring(start, end + 1) else cleaned
                        try {
                            val rawType = object : TypeToken<List<Map<String, Any>>>() {}.type
                            val raw: List<Map<String, Any>> = gson.fromJson(jsonStr, rawType)
                            val plans = raw.map { dayMap ->
                                val exs = (dayMap["exercises"] as? List<Map<String, Any>>)?.map { exMap ->
                                    PlanExercise(
                                        name = exMap["name"] as? String ?: "",
                                        sets = (exMap["sets"] as? Double)?.toInt() ?: 3,
                                        reps = exMap["reps"]?.toString() ?: "8-12",
                                        notes = exMap["notes"] as? String
                                    )
                                } ?: emptyList()
                                DayPlan(
                                    day = dayMap["day"] as? String ?: "",
                                    date = dayMap["date"] as? String ?: "",
                                    focus = dayMap["focus"] as? String ?: "",
                                    exercises = exs
                                )
                            }
                            prefs.setTrainingPlanJson(gson.toJson(plans))
                            _planError.value = null
                            parsed = true
                        } catch (parseEx: Exception) {
                            _planError.value = "AI 返回格式异常: ${parseEx.message?.take(50)}"
                        }
                    },
                    onFailure = { e ->
                        _planError.value = "AI 调用失败: ${e.message?.take(50)}"
                    }
                )
                // 兜底：AI 失败则使用内置标准计划
                if (!parsed) {
                    val fallback = buildFallbackPlan(trainingDays)
                    prefs.setTrainingPlanJson(gson.toJson(fallback))
                }
            } catch (ex: Exception) {
                _planError.value = "生成失败: ${ex.message?.take(60) ?: "未知错误"}"
            }
            _isGenerating.value = false
        }
    }

    private fun buildFallbackPlan(trainingDays: Int): List<DayPlan> {
        val now = LocalDate.now()
        val fmt = DateTimeFormatter.ofPattern("MM-dd")
        val all = mutableListOf<DayPlan>()

        val plan: List<Pair<String, List<Triple<String, Int, String>>>> = when {
            trainingDays >= 5 -> listOf(
                "胸+三头" to listOf(Triple("杠铃卧推", 4, "8-12"), Triple("上斜哑铃卧推", 3, "10-12"), Triple("哑铃飞鸟", 3, "12-15"), Triple("绳索下压", 3, "12-15"), Triple("窄距俯卧撑", 3, "力竭")),
                "背+二头" to listOf(Triple("引体向上", 4, "力竭"), Triple("杠铃划船", 3, "8-12"), Triple("哑铃弯举", 3, "10-12"), Triple("坐姿划船", 3, "10-12"), Triple("锤式弯举", 3, "12-15")),
                "休息日" to emptyList(),
                "腿+肩" to listOf(Triple("杠铃深蹲", 4, "8-12"), Triple("哑铃推举", 3, "10-12"), Triple("腿举", 3, "10-12"), Triple("侧平举", 3, "15-20"), Triple("小腿提踵", 3, "15-20")),
                "胸+背" to listOf(Triple("杠铃卧推", 4, "8-12"), Triple("引体向上", 4, "力竭"), Triple("上斜哑铃卧推", 3, "10-12"), Triple("坐姿划船", 3, "10-12"), Triple("俯身飞鸟", 3, "12-15")),
                "手臂+核心" to listOf(Triple("杠铃弯举", 3, "10-12"), Triple("窄距卧推", 3, "10-12"), Triple("绳索下压", 3, "12-15"), Triple("平板支撑", 3, "60秒"), Triple("悬垂举腿", 3, "15-20")),
                "休息日" to emptyList()
            )
            trainingDays >= 4 -> listOf(
                "胸+三头" to listOf(Triple("杠铃卧推", 4, "8-12"), Triple("上斜哑铃卧推", 3, "10-12"), Triple("哑铃飞鸟", 3, "12-15"), Triple("绳索下压", 3, "12-15")),
                "背+二头" to listOf(Triple("引体向上", 4, "力竭"), Triple("杠铃划船", 3, "8-12"), Triple("哑铃弯举", 3, "10-12"), Triple("坐姿划船", 3, "10-12")),
                "休息日" to emptyList(),
                "腿" to listOf(Triple("杠铃深蹲", 4, "8-12"), Triple("腿举", 3, "10-12"), Triple("罗马尼亚硬拉", 3, "10-12"), Triple("小腿提踵", 3, "15-20")),
                "肩+核心" to listOf(Triple("哑铃推举", 3, "10-12"), Triple("侧平举", 3, "15-20"), Triple("平板支撑", 3, "60秒"), Triple("悬垂举腿", 3, "15-20")),
                "休息日" to emptyList(),
                "休息日" to emptyList()
            )
            else -> listOf(
                "胸+三头" to listOf(Triple("杠铃卧推", 4, "8-12"), Triple("上斜哑铃卧推", 3, "10-12"), Triple("哑铃飞鸟", 3, "12-15"), Triple("绳索下压", 3, "12-15")),
                "休息日" to emptyList(),
                "背+二头" to listOf(Triple("引体向上", 4, "力竭"), Triple("杠铃划船", 3, "8-12"), Triple("哑铃弯举", 3, "10-12"), Triple("坐姿划船", 3, "10-12")),
                "休息日" to emptyList(),
                "腿+肩" to listOf(Triple("杠铃深蹲", 4, "8-12"), Triple("哑铃推举", 3, "10-12"), Triple("腿举", 3, "10-12"), Triple("侧平举", 3, "15-20")),
                "休息日" to emptyList(),
                "休息日" to emptyList()
            )
        }

        plan.forEachIndexed { i, (focus, exercises) ->
            val date = now.plusDays(i.toLong())
            val dayOfWeek = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.CHINESE).replace("星期", "周")
            val dayLabel = "${date.format(fmt)} $dayOfWeek"
            all.add(DayPlan(
                day = dayLabel,
                date = date.format(fmt),
                focus = focus,
                exercises = exercises.map { (n, s, r) -> PlanExercise(n, s, r, null) }
            ))
        }
        return all
    }

    // ── 手动编辑计划 ──
    fun addPlanExercise(dayIndex: Int) {
        viewModelScope.launch {
            val plans = parsePlan()
            if (dayIndex in plans.indices) {
                val updated = plans.toMutableList()
                val exs = updated[dayIndex].exercises.toMutableList()
                exs.add(PlanExercise("新动作", 3, "8-12", null))
                updated[dayIndex] = updated[dayIndex].copy(exercises = exs)
                if (updated[dayIndex].focus == "休息日") {
                    updated[dayIndex] = updated[dayIndex].copy(focus = "自定义")
                }
                prefs.setTrainingPlanJson(gson.toJson(updated))
            }
        }
    }

    fun removePlanExercise(dayIndex: Int, exerciseIndex: Int) {
        viewModelScope.launch {
            val plans = parsePlan()
            if (dayIndex in plans.indices) {
                val updated = plans.toMutableList()
                val exs = updated[dayIndex].exercises.toMutableList()
                if (exerciseIndex in exs.indices) {
                    exs.removeAt(exerciseIndex)
                    updated[dayIndex] = updated[dayIndex].copy(exercises = exs)
                    prefs.setTrainingPlanJson(gson.toJson(updated))
                }
            }
        }
    }

    fun updatePlanExercise(dayIndex: Int, exerciseIndex: Int, ex: PlanExercise) {
        viewModelScope.launch {
            val plans = parsePlan()
            if (dayIndex in plans.indices) {
                val updated = plans.toMutableList()
                val exs = updated[dayIndex].exercises.toMutableList()
                if (exerciseIndex in exs.indices) {
                    exs[exerciseIndex] = ex
                    updated[dayIndex] = updated[dayIndex].copy(exercises = exs)
                    prefs.setTrainingPlanJson(gson.toJson(updated))
                }
            }
        }
    }

    private fun parsePlan(): List<DayPlan> {
        val json = planJson.value
        if (json.isBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<DayPlan>>() {}.type
            gson.fromJson(json, type)
        } catch (_: Exception) { emptyList() }
    }

    fun clearPlanError() { _planError.value = null }

    fun clearHistoryExercises() {
        _historyExercises.value = emptyList()
    }

    /**
     * 保存一条训练记录。
     */
    fun saveRecord(
        bodyParts: List<String>,
        exerciseName: String,
        sets: Int,
        reps: Int,
        weightKg: Double,
        notes: String?
    ) {
        viewModelScope.launch {
            val ts = now
            dao.insert(
                TrainingRecord(
                    date = todayDate,
                    timestamp = ts,
                    bodyParts = bodyParts.joinToString(","),
                    exerciseName = exerciseName,
                    sets = sets,
                    reps = reps,
                    weightKg = weightKg,
                    notes = notes
                )
            )
        }
    }

    /**
     * 删除训练记录。
     */
    fun deleteRecord(record: TrainingRecord) {
        viewModelScope.launch {
            dao.delete(record)
        }
    }

    fun updateRecord(id: Long, bodyParts: List<String>, exerciseName: String,
                     sets: Int, reps: Int, weightKg: Double, notes: String?) {
        viewModelScope.launch {
            // 保留原始 date，使用 @Update 按主键更新
            val existing = dao.getRecordById(id)
            if (existing != null) {
                dao.update(existing.copy(
                    bodyParts = bodyParts.joinToString(","),
                    exerciseName = exerciseName,
                    sets = sets,
                    reps = reps,
                    weightKg = weightKg,
                    notes = notes
                ))
            }
        }
    }

    /**
     * 按动作名查找 ExerciseLibrary 详情（供详情页使用）。
     */
    suspend fun getExerciseByName(name: String): ExerciseLibrary? {
        return exerciseRepo.getAllExercises()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
            .value
            .firstOrNull { it.name == name }
    }
}
