package com.example.detoxxing.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.detoxxing.ui.theme.DarkBackground
import com.example.detoxxing.ui.theme.TextGray
import com.example.detoxxing.ui.theme.TextWhite

@Composable
fun MainScreen(username: String, mainNavController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = DarkBackground, contentColor = TextWhite) {
                // 0. SESSION
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Timer, contentDescription = "Session") },
                    label = { Text("session", fontSize = 11.sp) },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = TextWhite, selectedTextColor = TextWhite, unselectedIconColor = TextGray, unselectedTextColor = TextGray, indicatorColor = Color.Transparent)
                )
                // 1. STATS
                NavigationBarItem(
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
                    label = { Text("stats", fontSize = 11.sp) },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = TextWhite, selectedTextColor = TextWhite, unselectedIconColor = TextGray, unselectedTextColor = TextGray, indicatorColor = Color.Transparent)
                )
                // 2. APPS
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Apps, contentDescription = "Apps") },
                    label = { Text("apps", fontSize = 11.sp) },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = TextWhite, selectedTextColor = TextWhite, unselectedIconColor = TextGray, unselectedTextColor = TextGray, indicatorColor = Color.Transparent)
                )
                // 3. PROFILE
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("profile", fontSize = 11.sp) },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = TextWhite, selectedTextColor = TextWhite, unselectedIconColor = TextGray, unselectedTextColor = TextGray, indicatorColor = Color.Transparent)
                )
            }
        }
    ) { paddingValues ->
        Modifier.padding(paddingValues)

        when (selectedTab) {
            0 -> DashboardScreen(username = username, navController = mainNavController)
            1 -> StatsScreen()
            2 -> BlockedAppsScreen()
            3 -> ProfileScreen(username = username, navController = mainNavController)
        }
    }
}