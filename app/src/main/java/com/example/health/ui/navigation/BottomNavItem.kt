package com.example.health.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底部导航栏 Tab 定义。
 */
sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Diet : BottomNavItem(
        route = "diet",
        label = "饮食",
        icon = Icons.Default.Fastfood
    )

    data object Training : BottomNavItem(
        route = "training",
        label = "训练",
        icon = Icons.Default.FitnessCenter
    )

    data object ActivityStats : BottomNavItem(
        route = "activity_stats",
        label = "运动",
        icon = Icons.Default.DirectionsRun
    )

    data object Chat : BottomNavItem(
        route = "chat",
        label = "对话",
        icon = Icons.Default.ChatBubble
    )

    data object Dashboard : BottomNavItem(
        route = "dashboard",
        label = "看板",
        icon = Icons.Default.Dashboard
    )

    companion object {
        val items = listOf(Diet, Training, ActivityStats, Chat, Dashboard)
    }
}
