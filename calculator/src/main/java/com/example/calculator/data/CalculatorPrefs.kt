package com.example.calculator.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.calculator.HistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.calcDataStore: DataStore<Preferences> by preferencesDataStore(name = "calc_settings")

class CalculatorPrefs(private val context: Context) {

    data class Snapshot(
        val history: List<HistoryItem>,
        val accentIndex: Int,
    )

    fun observe(): Flow<Snapshot> = context.calcDataStore.data.map { prefs ->
        Snapshot(
            history = decode(prefs[KEY_HISTORY] ?: ""),
            accentIndex = prefs[KEY_ACCENT] ?: 0,
        )
    }

    suspend fun saveHistory(items: List<HistoryItem>) {
        context.calcDataStore.edit { prefs ->
            prefs[KEY_HISTORY] = encode(items)
        }
    }

    suspend fun saveAccent(index: Int) {
        context.calcDataStore.edit { prefs ->
            prefs[KEY_ACCENT] = index
        }
    }

    private fun encode(items: List<HistoryItem>): String = items.joinToString("\n") {
        "${it.expression}\t${it.result}\t${it.timestamp}"
    }

    private fun decode(raw: String): List<HistoryItem> {
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            val parts = line.split("\t")
            if (parts.size != 3) return@mapNotNull null
            val timestamp = parts[2].toLongOrNull() ?: return@mapNotNull null
            HistoryItem(parts[0], parts[1], timestamp)
        }.toList()
    }

    private companion object {
        val KEY_HISTORY = stringPreferencesKey("history")
        val KEY_ACCENT = intPreferencesKey("accent_index")
    }
}
