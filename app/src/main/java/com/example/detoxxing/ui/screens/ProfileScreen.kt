package com.example.detoxxing.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.detoxxing.supabase
import com.example.detoxxing.ui.theme.DarkBackground
import com.example.detoxxing.ui.theme.TextGray
import com.example.detoxxing.ui.theme.TextWhite
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(username: String, navController: NavController) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // ВЕРХНЯЯ ПАНЕЛЬ (только для иконки выхода)
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

        // ЦЕНТРАЛЬНАЯ ЧАСТЬ С ПРОФИЛЕМ (теперь она строго по центру)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)// Занимает все свободное место
                .padding(bottom = 500.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Центрируем по вертикали
        ) {
            // Аватар с плюсиком
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
        }
    }
}