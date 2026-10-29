package com.example.detoxxing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.detoxxing.ui.theme.DarkBackground
import com.example.detoxxing.ui.theme.TextGray
import com.example.detoxxing.ui.theme.TextWhite

@Composable
fun DashboardScreen(
    username: String,
    navController: NavController,
    detoxViewModel: DetoxViewModel = viewModel()
) {
    val isActive by detoxViewModel.isSessionActive.collectAsState()
    val time by detoxViewModel.timeInSeconds.collectAsState()
    val isLoading by detoxViewModel.isLoading.collectAsState()
    val error by detoxViewModel.error.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(60.dp))

        Text("Привет, $username!", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Время очистить свой разум.", color = TextGray, fontSize = 16.sp)

        Spacer(Modifier.weight(1f))

        // Красивый круг с таймером
        Box(
            modifier = Modifier
                .size(250.dp)
                .background(if (isActive) Color(0xFF1E3A2A) else Color(0xFF2C2C2C), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formatTime(time),
                color = TextWhite,
                fontSize = 48.sp,
                fontWeight = FontWeight.Light
            )
        }

        Spacer(Modifier.weight(1f))

        if (error != null) {
            Text(error!!, color = Color(0xFFFF6B6B), fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
        }

        Button(
            onClick = { if (isActive) detoxViewModel.finishDetox() else detoxViewModel.startDetox() },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) Color(0xFFFF6B6B) else TextWhite,
                contentColor = if (isActive) TextWhite else Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)

        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            } else {
                Text(if (isActive) "Завершить детокс" else "Начать детокс", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(110.dp))
    }
}

// Функция для перевода секунд в формат 00:00:00
fun formatTime(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}