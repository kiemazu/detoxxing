package com.example.detoxxing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.detoxxing.services.AppBlockerService
import com.example.detoxxing.ui.screens.*
import com.example.detoxxing.ui.theme.DarkBackground
import com.example.detoxxing.ui.theme.KkTheme
import com.example.detoxxing.ui.theme.TextWhite
import io.github.jan.supabase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KkTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = DarkBackground) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash") {

        composable("splash") {
            LaunchedEffect(Unit) {
                var currentUser = supabase.auth.currentUserOrNull()
                var attempts = 0

                while (currentUser == null && attempts < 4) {
                    kotlinx.coroutines.delay(500)
                    currentUser = supabase.auth.currentUserOrNull()
                    attempts++
                }

                if (currentUser != null) {
                    val intent = android.content.Intent(
                        context,
                        AppBlockerService::class.java
                    )

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }

                    navController.navigate("dashboard") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
                else {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }
        }

        composable("login") {
            LoginScreen(
                navController = navController,
                onLoginSuccess = {
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
            MainScreen(username = username, mainNavController = navController)
        }
    }
}