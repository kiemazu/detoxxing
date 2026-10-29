package com.example.detoxxing.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
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
    // 0 - Session, 1 - Profile (по умолчанию открываем Session)
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkBackground,
                contentColor = TextWhite
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Timer, contentDescription = "Session") },
                    label = { Text("session", fontSize = 12.sp) },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextWhite, selectedTextColor = TextWhite,
                        unselectedIconColor = TextGray, unselectedTextColor = TextGray,
                        indicatorColor = Color.Transparent // Убираем серый овал при нажатии
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("profile", fontSize = 12.sp) },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TextWhite, selectedTextColor = TextWhite,
                        unselectedIconColor = TextGray, unselectedTextColor = TextGray,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }
    ) { paddingValues ->
        Modifier.padding(paddingValues)

        // Переключаем экраны в зависимости от выбранной вкладки
        when (selectedTab) {
            0 -> DashboardScreen(username = username, navController = mainNavController)
            1 -> ProfileScreen(username = username, navController = mainNavController)
        }
    }
}