package com.example.health.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.health.ui.chat.ChatScreen
import com.example.health.ui.dashboard.DashboardScreen
import com.example.health.ui.diet.DietScreen
import com.example.health.ui.diet.DietViewModel
import com.example.health.ui.diet.FoodConfirmScreen
import com.example.health.ui.settings.SettingsScreen
import com.example.health.ui.training.TrainingScreen

/**
 * 应用主导航骨架 —— 底部导航栏 + 各 Tab 内容区域。
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // Activity 级别的共享 ViewModel，DietScreen 和 FoodConfirmScreen 共用
    val dietViewModel: DietViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

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
            composable(BottomNavItem.Training.route) { TrainingScreen() }
            composable(BottomNavItem.Chat.route) { ChatScreen() }
            composable(BottomNavItem.Dashboard.route) { DashboardScreen() }
            composable("settings") {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
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
