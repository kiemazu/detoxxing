package com.example.detoxxing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.detoxxing.ui.theme.DarkBackground
import com.example.detoxxing.ui.theme.TextGray
import com.example.detoxxing.ui.theme.TextWhite

@Composable
fun StatsScreen(statsViewModel: StatsViewModel = viewModel()) {
    val context = LocalContext.current
    val stats by statsViewModel.usageStats.collectAsState()
    val isLoading by statsViewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        statsViewModel.loadStats(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Statistics",
            color = TextWhite,
            fontSize = 32.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(Modifier.height(20.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TextWhite)
            }
        } else if (stats.isEmpty()) {
            Text("Нет данных за сегодня. Пользуйтесь телефоном!", color = TextGray)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(stats) { stat ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = stat.appName, color = TextWhite, fontSize = 16.sp)
                            Text(text = "${stat.timeInMinutes} мин", color = TextWhite, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { stat.timeInMinutes.toFloat() / stats.first().timeInMinutes },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = Color(0xFF4CAF50),
                            trackColor = Color(0xFF2C2C2C),
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}