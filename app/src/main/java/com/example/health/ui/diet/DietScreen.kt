package com.example.health.ui.diet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 饮食记录 Tab —— 占位页面。
 */
@Composable
fun DietScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🍽️ 饮食记录",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}
