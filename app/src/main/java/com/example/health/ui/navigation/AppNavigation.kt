package com.example.health.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.health.ui.chat.ChatScreen
import com.example.health.ui.activity.ActivityScreen
import com.example.health.ui.dashboard.DashboardScreen
import com.example.health.ui.dashboard.DashboardViewModel
import com.example.health.ui.diet.DietScreen
import com.example.health.ui.diet.DietViewModel
import com.example.health.ui.diet.FoodConfirmScreen
import com.example.health.ui.settings.SettingsScreen
import com.example.health.ui.settings.FoodLibraryScreen
import com.example.health.ui.stats.ActivityStatsScreen
import com.example.health.ui.training.ExerciseDetailScreen
import com.example.health.ui.training.TrainingScreen
import com.example.health.ui.training.TrainingViewModel

/**
 * 应用主导航骨架 —— 底部导航栏 + 各 Tab 内容区域。
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    // 键盘可见状态（缓存为 derivedStateOf，减少无关重组）
    val imeVisible by remember {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }
    val dietViewModel: DietViewModel = viewModel()
    val trainingViewModel: TrainingViewModel = viewModel()
    // 看板 ViewModel 提升到 Activity 级：数据常驻预热，切换 Tab 不再重建/重查
    val dashboardViewModel: DashboardViewModel = viewModel()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val isChat = currentDestination?.hierarchy?.any { it.route == BottomNavItem.Chat.route } == true
            // 聊天页：点击输入框（键盘弹出）时隐藏底栏，平时正常显示
            val hideForChatKeyboard = isChat && imeVisible
            if (!hideForChatKeyboard) {
                NavigationBar {
                    BottomNavItem.items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Diet.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Diet.route) {
                DietScreen(
                    viewModel = dietViewModel,
                    onNavigateToConfirm = {
                        navController.navigate("food_confirm")
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }
            composable(BottomNavItem.Training.route) {
                TrainingScreen(
                    viewModel = trainingViewModel,
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToExerciseDetail = { name ->
                        navController.navigate("exercise_detail/$name")
                    }
                )
            }
            composable("exercise_detail/{exerciseName}") { backStackEntry ->
                val exerciseName = backStackEntry.arguments?.getString("exerciseName") ?: ""
                val allExercises by trainingViewModel.allExercises.collectAsState()
                val exercise = allExercises.firstOrNull { it.name == exerciseName }
                if (exercise != null) {
                    ExerciseDetailScreen(
                        exercise = exercise,
                        onBack = { navController.popBackStack() }
                    )
                } else {
                    // 找不到则直接返回
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }
            composable(BottomNavItem.Chat.route) {
                ChatScreen(onNavigateToSettings = { navController.navigate("settings") })
            }
            composable(BottomNavItem.ActivityStats.route) {
                ActivityStatsScreen(
                    onNavigateToActivity = { navController.navigate("activity") },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable(BottomNavItem.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToSettings = { navController.navigate("settings") },
                    onNavigateToActivity = { navController.navigate("activity") }
                )
            }
            composable("activity") {
                ActivityScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToFoodLibrary = { navController.navigate("food_library") }
                )
            }
            composable("food_library") {
                FoodLibraryScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("food_confirm") {
                FoodConfirmScreen(
                    viewModel = dietViewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
        }
    }
}
