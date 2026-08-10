package com.example.calculator.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calculator.CalcOperator
import com.example.calculator.CalculatorEngine
import com.example.calculator.HistoryItem
import com.example.calculator.data.CalculatorPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val expression: String = "",
    val display: String = "0",
    val isError: Boolean = false,
    val accentIndex: Int = 0,
    val history: List<HistoryItem> = emptyList(),
    val showHistory: Boolean = false,
    val showSettings: Boolean = false,
)

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = CalculatorPrefs(application)
    private val engine = CalculatorEngine()

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.observe().collect { data ->
                _uiState.update {
                    it.copy(history = data.history, accentIndex = data.accentIndex)
                }
            }
        }
    }

    private fun refresh() {
        val snapshot = engine.snapshot()
        _uiState.update {
            it.copy(
                expression = snapshot.expression,
                display = snapshot.display,
                isError = snapshot.isError,
            )
        }
    }

    fun onDigit(digit: Int) {
        engine.inputDigit(digit)
        refresh()
    }

    fun onDot() {
        engine.inputDot()
        refresh()
    }

    fun onOperator(operator: CalcOperator) {
        engine.inputOperator(operator)
        refresh()
    }

    fun onEquals() {
        engine.equals()?.let { item ->
            val items = (listOf(item) + _uiState.value.history).take(MAX_HISTORY)
            _uiState.update { it.copy(history = items) }
            viewModelScope.launch { prefs.saveHistory(items) }
        }
        refresh()
    }

    fun onClear() {
        engine.clear()
        refresh()
    }

    fun onBackspace() {
        engine.backspace()
        refresh()
    }

    fun onUseHistory(item: HistoryItem) {
        engine.loadResult(item.result)
        _uiState.update { it.copy(showHistory = false) }
        refresh()
    }

    fun onDeleteHistory(item: HistoryItem) {
        val items = _uiState.value.history.filterNot {
            it.expression == item.expression &&
                it.result == item.result &&
                it.timestamp == item.timestamp
        }
        _uiState.update { it.copy(history = items) }
        viewModelScope.launch { prefs.saveHistory(items) }
    }

    fun onClearHistory() {
        _uiState.update { it.copy(history = emptyList()) }
        viewModelScope.launch { prefs.saveHistory(emptyList()) }
    }

    fun onToggleHistory() {
        _uiState.update { it.copy(showHistory = !it.showHistory) }
    }

    fun onDismissHistory() {
        _uiState.update { it.copy(showHistory = false) }
    }

    fun onToggleSettings() {
        _uiState.update { it.copy(showSettings = !it.showSettings) }
    }

    fun onDismissSettings() {
        _uiState.update { it.copy(showSettings = false) }
    }

    fun onSelectAccent(index: Int) {
        _uiState.update { it.copy(accentIndex = index) }
        viewModelScope.launch { prefs.saveAccent(index) }
    }

    private companion object {
        const val MAX_HISTORY = 200
    }
}
