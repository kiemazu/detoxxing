package com.example.detoxxing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.detoxxing.supabase
import com.example.detoxxing.ui.theme.DarkBackground
import com.example.detoxxing.ui.theme.TextGray
import com.example.detoxxing.ui.theme.TextWhite
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Composable
fun ProfileScreen(username: String, navController: NavController) {
    val scope = rememberCoroutineScope()


    var totalPoints by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val userId = supabase.auth.currentUserOrNull()?.id
        if (userId != null) {
            try {
                val sessions = supabase.from("detox_session")
                    .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("points_test")) {
                        filter {
                            eq("user_id", userId)
                            eq("status", "finished")
                        }
                    }.decodeList<SessionPoints>()
                totalPoints = sessions.sumOf { it.points ?: 0 }
            } catch (e: Exception) {
                println("Ошибка загрузки баллов: ${e.message}")
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = {
                    scope.launch {
                        try {
                            supabase.auth.signOut()
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            println("Ошибка выхода: ${e.message}")
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Log Out",
                    tint = TextWhite
                )
            }
        }


        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 500.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Box(contentAlignment = Alignment.BottomEnd) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Avatar",
                    tint = TextWhite,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                )

                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "Add photo",
                    tint = TextWhite,
                    modifier = Modifier
                        .size(28.dp)
                        .background(DarkBackground, CircleShape)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "@$username",
                color = TextGray,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Баллов: $totalPoints",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@kotlinx.serialization.Serializable
data class SessionPoints(
    @kotlinx.serialization.SerialName("points_test") val points: Int? = 0
)