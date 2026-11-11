package com.example.detoxxing.ui.screens

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.navigation.NavController
import com.example.detoxxing.services.AppBlockerService
import com.example.detoxxing.supabase
import com.example.detoxxing.ui.theme.DarkBackground
import com.example.detoxxing.ui.theme.TextGray
import com.example.detoxxing.ui.theme.TextWhite
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

@Composable
fun DashboardScreen(
    username: String,
    navController: NavController,
    detoxViewModel: DetoxViewModel = viewModel()
) {
    val context = LocalContext.current
    val isActive by detoxViewModel.isSessionActive.collectAsState()
    val time by detoxViewModel.timeInSeconds.collectAsState()
    val isLoading by detoxViewModel.isLoading.collectAsState()
    val error by detoxViewModel.error.collectAsState()

    var totalPoints by remember { mutableStateOf(0) }


    LaunchedEffect(Unit) {
        val userId = supabase.auth.currentUserOrNull()?.id
        if (userId != null) {
            try {
                val userProfile = supabase.from("user")
                    .select(columns = Columns.list("total_points")) {
                        filter { eq("id", userId) }
                    }.decodeSingleOrNull<UserPoints>()
                totalPoints = userProfile?.totalPoints ?: 0
            } catch (e: Exception) {
                println("Ошибка загрузки баллов: ${e.message}")
            }
        }
    }
    LaunchedEffect(Unit) {
        detoxViewModel.restoreActiveSession {
            val serviceIntent = Intent(
                context,
                AppBlockerService::class.java
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Привет, $username!", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text("Время очистить свой разум.", color = TextGray, fontSize = 16.sp)

            Spacer(Modifier.height(12.dp))

            Text(
                text = "У тебя $totalPoints баллов",
                color = Color(0xFF4CAF50),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.weight(1f))

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

        Spacer(Modifier.weight(2f))

        if (error != null) {
            Text(error!!, color = Color(0xFFFF6B6B), fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
        }

        Button(
            onClick = {

                if (isActive) {
                    detoxViewModel.finishDetox {
                        context.stopService(
                            Intent(context, AppBlockerService::class.java)
                        )
                    }
                } else {
                    detoxViewModel.startDetox {
                        val serviceIntent = Intent(
                            context,
                            AppBlockerService::class.java
                        )

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    }
                }
            },
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) Color(0xFFFF6B6B) else TextWhite,
                contentColor = if (isActive) TextWhite else Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            } else {
                Text(if (isActive) "Завершить детокс" else "Начать детокс", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(200.dp))
    }
}

fun formatTime(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}


@Serializable
data class UserPoints(
    @SerialName("total_points") val totalPoints: Int? = 0
)