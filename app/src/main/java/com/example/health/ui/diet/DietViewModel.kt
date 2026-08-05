package com.example.health.ui.diet

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.health.data.local.AppDatabase
import com.example.health.data.local.entity.DietRecord
import com.example.health.data.local.entity.FoodLibrary
import com.example.health.data.remote.dto.FoodRecognitionResult
import com.example.health.data.remote.dto.RecognizedFood
import com.example.health.data.repository.AiRepository
import com.example.health.data.repository.FoodRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class DietViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).dietRecordDao()
    private val aiRepo = AiRepository(application)
    private val foodRepo = FoodRepository(application)

    // ── 今日记录 ──
    val todayRecords: StateFlow<List<DietRecord>> = dao.getAllRecords()
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
                        _recognitionState.value = RecognitionState.Idle
                        _lastRecognitionResult.value = foods
                        _navigateToConfirm.emit(foods)
                    },
                    onFailure = { error ->
                        // AI 失败 → 转手动录入（可传空结果）
                        _recognitionState.value = RecognitionState.Error(
                            error.message ?: "识别失败"
                        )
                        _lastRecognitionResult.value = FoodRecognitionResult()
                        _navigateToConfirm.emit(FoodRecognitionResult())
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
                    mealType = mealType,
                    timestamp = now,
                    imagePath = imagePath
                )
            }
            dao.insertAll(records)
        }
    }

    /**
     * 手动保存一条饮食记录。
     */
    fun saveManualRecord(name: String, weightG: Int, caloriesKcal: Int, mealType: String) {
        viewModelScope.launch {
            dao.insert(
                DietRecord(
                    foodName = name,
                    weightG = weightG,
                    caloriesKcal = caloriesKcal,
                    mealType = mealType,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateRecord(id: Long, name: String, weightG: Int, caloriesKcal: Int, mealType: String) {
        viewModelScope.launch {
            dao.insert(DietRecord(id = id, foodName = name, weightG = weightG,
                caloriesKcal = caloriesKcal, mealType = mealType,
                timestamp = System.currentTimeMillis(), imagePath = null))
        }
    }

    fun deleteRecord(record: DietRecord) {
        viewModelScope.launch { dao.delete(record) }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    // ── 新增：FoodLibrary 加载状态 ──
    val allFoods: StateFlow<List<FoodLibrary>> = foodRepo.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}

// ── AI 识别状态封装 ──
sealed class RecognitionState {
    data object Idle : RecognitionState()
    data object Compressing : RecognitionState()
    data object Recognizing : RecognitionState()
    data class Error(val message: String) : RecognitionState()
}
