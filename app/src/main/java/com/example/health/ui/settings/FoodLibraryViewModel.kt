package com.example.health.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.health.data.local.entity.FoodLibrary
import com.example.health.data.repository.FoodRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 自定义食物库管理页 ViewModel。
 * 展示内置 + 自定义食物（自定义优先），支持添加 / 编辑 / 删除。
 */
class FoodLibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = FoodRepository(application)

    /** 全部食物，DAO 按 isCustom DESC, name ASC 排序（自定义优先） */
    val allFoods: StateFlow<List<FoodLibrary>> = repo.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addFood(name: String, caloriesPer100g: Int) {
        viewModelScope.launch { repo.insertCustomFood(name.trim(), caloriesPer100g) }
    }

    fun updateFood(food: FoodLibrary) {
        viewModelScope.launch { repo.updateFood(food) }
    }

    fun deleteFood(food: FoodLibrary) {
        viewModelScope.launch { repo.deleteFood(food) }
    }
}
