package com.example.detoxxing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.detoxxing.ui.screens.DashboardScreen
import com.example.detoxxing.ui.screens.LoginScreen
import com.example.detoxxing.ui.screens.MainScreen
import com.example.detoxxing.ui.screens.ProfileScreen
import com.example.detoxxing.ui.screens.RegisterScreen
import com.example.detoxxing.ui.theme.DarkBackground
import com.example.detoxxing.ui.theme.KkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KkTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) { AppNavigation() }
            }
        }
    }
}
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                navController = navController,
                onLoginSuccess = {
                    // При успехе чистим стек и идем на главный экран
                    navController.navigate("dashboard") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("register") {
            RegisterScreen(
                navController = navController,
                onRegisterSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }

        composable("dashboard") {
            var username by remember { mutableStateOf("") }
            LaunchedEffect(Unit) { username = fetchUsername() ?: "user" }

            // Передаем новый контейнер
            MainScreen(username = username, mainNavController = navController)
        }

        composable("profile") {
            var username by remember { mutableStateOf("") }
            LaunchedEffect(Unit) { username = fetchUsername() ?: "unknown" }
            ProfileScreen(username = username, navController = navController)
        }
    }
}