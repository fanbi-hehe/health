package com.example.health.ui.diet

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.DietRecord
import com.example.health.data.local.entity.FoodLibrary
import com.example.health.data.local.entity.MealTemplate
import com.example.health.data.remote.dto.FoodRecognitionResult
import com.example.health.data.remote.dto.RecognizedFood
import com.example.health.data.repository.AiRepository
import com.example.health.data.repository.FoodRepository
import com.example.health.widget.CalorieWidget
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class DietViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).dietRecordDao()
    private val templateDao = AppDatabase.getInstance(application).mealTemplateDao()
    private val aiRepo = AiRepository(application)
    private val foodRepo = FoodRepository(application)
    private val gson = Gson()

    // ── 全部记录（按时间倒序，页面按选中日期过滤回看） ──
    val allRecords: StateFlow<List<DietRecord>> = dao.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── 食物搜索（手动录入自动补全） ──
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // ── AI 识别状态 ──
    private val _recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val recognitionState: StateFlow<RecognitionState> = _recognitionState.asStateFlow()

    // ── 导航事件（一次性） ──
    private val _navigateToConfirm = MutableSharedFlow<FoodRecognitionResult>()
    val navigateToConfirm = _navigateToConfirm.asSharedFlow()

    // ── 最新识别结果（供 FoodConfirmScreen 读取） ──
    private val _lastRecognitionResult = MutableStateFlow<FoodRecognitionResult?>(null)
    val lastRecognitionResult: StateFlow<FoodRecognitionResult?> = _lastRecognitionResult.asStateFlow()

    // ── 当前拍照图片路径（供确认页显示） ──
    private val _currentPhotoPath = MutableStateFlow<String?>(null)
    val currentPhotoPath: StateFlow<String?> = _currentPhotoPath.asStateFlow()

    // ── 临时的相机输出 URI ──
    private var tempPhotoUri: Uri? = null

    /**
     * 创建用于相机输出的临时文件 URI（通过 FileProvider）。
     */
    fun createTempPhotoUri(): Uri {
        val context = getApplication<Application>()
        val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        ).also {
            tempPhotoUri = it
            // 保存 file 路径供后续压缩使用
            _currentPhotoPath.value = file.absolutePath
        }
    }

    /**
     * 拍照完成后调用 —— 压缩图片并调用 AI 识别。
     */
    fun onPhotoTaken() {
        val path = _currentPhotoPath.value ?: return
        val context = getApplication<Application>()
        val file = File(path)

        _recognitionState.value = RecognitionState.Compressing

        viewModelScope.launch {
            try {
                // 1. 压缩（直接压缩文件）
                val compressed = com.example.health.util.ImageCompressor.compress(context, Uri.fromFile(file))
                _currentPhotoPath.value = compressed.absolutePath

                // 2. AI 识别
                _recognitionState.value = RecognitionState.Recognizing

                val result = aiRepo.recognizeFood(compressed)

                result.fold(
                    onSuccess = { foods ->
                        // 识别完成后，用语言模型估算宏量（失败则保持 0，不影响主流程）
                        val foodsWithMacros = foods.foods
                        val finalResult = FoodRecognitionResult(
                            foods = foodsWithMacros,
                            totalCalories = foods.totalCalories
                        )
                        _recognitionState.value = RecognitionState.Idle
                        _lastRecognitionResult.value = finalResult
                        _navigateToConfirm.emit(finalResult)
                    },
                    onFailure = { error ->
                        // AI 失败 → 留在主页显示错误，不跳转确认页
                        _recognitionState.value = RecognitionState.Error(
                            error.message ?: "识别失败，请重试或手动录入"
                        )
                    }
                )
            } catch (e: Exception) {
                _recognitionState.value = RecognitionState.Error(e.message ?: "未知错误")
            }
        }
    }

    fun clearRecognitionError() {
        _recognitionState.value = RecognitionState.Idle
    }

    /**
     * 保存确认后的饮食记录。
     */
    fun saveFoodRecords(
        foods: List<RecognizedFood>,
        mealType: String,
        imagePath: String?
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val records = foods.map { food ->
                DietRecord(
                    foodName = food.name,
                    weightG = food.weightG,
                    caloriesKcal = food.caloriesKcal,
                    proteinG = food.proteinG,
                    carbsG = food.carbsG,
                    fatG = food.fatG,
                    mealType = mealType,
                    timestamp = now,
                    imagePath = imagePath
                )
            }
            dao.insertAll(records)
            refreshCalorieWidget()
        }
    }

    /**
     * 手动保存一条饮食记录。
     */
    fun saveManualRecord(
        name: String,
        weightG: Int,
        caloriesKcal: Int,
        mealType: String,
        proteinG: Int = 0,
        carbsG: Int = 0,
        fatG: Int = 0,
        date: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    ) {
        viewModelScope.launch {
            dao.insert(
                DietRecord(
                    foodName = name,
                    weightG = weightG,
                    caloriesKcal = caloriesKcal,
                    proteinG = proteinG,
                    carbsG = carbsG,
                    fatG = fatG,
                    mealType = mealType,
                    timestamp = dateToTimestamp(date)
                )
            )
            refreshCalorieWidget()
        }
    }

    fun updateRecord(
        id: Long,
        name: String,
        weightG: Int,
        caloriesKcal: Int,
        mealType: String,
        proteinG: Int,
        carbsG: Int,
        fatG: Int
    ) {
        viewModelScope.launch {
            // 保留原始 timestamp 和 imagePath，使用 @Update 按主键更新
            val existing = dao.getRecordById(id)
            if (existing != null) {
                dao.update(existing.copy(
                    foodName = name,
                    weightG = weightG,
                    caloriesKcal = caloriesKcal,
                    mealType = mealType,
                    proteinG = proteinG,
                    carbsG = carbsG,
                    fatG = fatG
                ))
                refreshCalorieWidget()
            }
        }
    }

    fun deleteRecord(record: DietRecord) {
        viewModelScope.launch {
            dao.delete(record)
            refreshCalorieWidget()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    // ── 新增：FoodLibrary 加载状态 ──
    val allFoods: StateFlow<List<FoodLibrary>> = foodRepo.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ── 餐食模板 ──
    val mealTemplates: StateFlow<List<MealTemplate>> = templateDao.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * 保存当前食物组合为餐食模板。
     */
    fun saveMealTemplate(name: String, foods: List<RecognizedFood>) {
        viewModelScope.launch {
            templateDao.insert(
                MealTemplate(
                    templateName = name.trim(),
                    itemsJson = gson.toJson(foods)
                )
            )
        }
    }

    /**
     * 一键加载模板：解析食物项 → 作为识别结果填充确认页 → 跳转。
     */
    fun loadTemplate(template: MealTemplate) {
        viewModelScope.launch {
            val items = parseTemplateItems(template)
            _currentPhotoPath.value = null
            _lastRecognitionResult.value = FoodRecognitionResult(foods = items)
            _navigateToConfirm.emit(FoodRecognitionResult(foods = items))
        }
    }

    fun renameTemplate(template: MealTemplate, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            templateDao.update(template.copy(templateName = newName.trim()))
        }
    }

    fun deleteTemplate(template: MealTemplate) {
        viewModelScope.launch {
            templateDao.delete(template)
        }
    }

    private fun parseTemplateItems(template: MealTemplate): List<RecognizedFood> {
        if (template.itemsJson.isBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<RecognizedFood>>() {}.type
            gson.fromJson(template.itemsJson, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** 饮食数据变化后主动刷新桌面小组件。 */
    private suspend fun refreshCalorieWidget() {
        try {
            CalorieWidget.updateAll(getApplication())
        } catch (_: Exception) {
            // 小组件刷新失败不影响主流程
        }
    }

    /** "yyyy-MM-dd" → 当天 12:00 的时间戳（用于回看历史时补录）。 */
    private fun dateToTimestamp(date: String): Long {
        return LocalDate.parse(date)
            .atTime(12, 0)
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}

// ── AI 识别状态封装 ──
sealed class RecognitionState {
    data object Idle : RecognitionState()
    data object Compressing : RecognitionState()
    data object Recognizing : RecognitionState()
    data class Error(val message: String) : RecognitionState()
}
