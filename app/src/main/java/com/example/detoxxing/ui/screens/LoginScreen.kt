package com.example.detoxxing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.detoxxing.ui.components.ClickableTextLink
import com.example.detoxxing.ui.components.CustomTextField
import com.example.detoxxing.ui.theme.DarkBackground
import com.example.detoxxing.ui.theme.TextWhite

@Composable
fun LoginScreen(navController: NavController, authViewModel: AuthViewModel = viewModel() , onLoginSuccess: () -> Unit, ) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }

    // Подписываемся на состояния из ViewModel
    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()
    val loginSuccess by authViewModel.loginSuccess.collectAsState()

    // Слушаем успешный вход
    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            onLoginSuccess() // <-- ВЫЗЫВАЕМ ЛЯМБДУ ЗДЕСЬ
        }
    }

    Column(Modifier.fillMaxSize().background(DarkBackground).verticalScroll(rememberScrollState()).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(60.dp))
        Text("Log In", color = TextWhite, fontSize = 32.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(60.dp))

        CustomTextField("username", username, { username = it; authViewModel.clearError() })
        Spacer(Modifier.height(20.dp))
        CustomTextField("password", password, { password = it; authViewModel.clearError() }, isPassword = true)
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(rememberMe, { rememberMe = it }, colors = CheckboxDefaults.colors(checkedColor = TextWhite, uncheckedColor = TextWhite, checkmarkColor = Color.Black))
            Text("remember me", color = TextWhite, fontSize = 14.sp)
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(errorMessage!!, color = Color(0xFFFF6B6B), fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { authViewModel.login(username, password) }, // Вызываем функцию ViewModel
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = TextWhite, contentColor = Color.Black),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            } else {
                Text("Log In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(40.dp))
        ClickableTextLink("dont have an account? ", "Register") { navController.navigate("register") }
    }
}