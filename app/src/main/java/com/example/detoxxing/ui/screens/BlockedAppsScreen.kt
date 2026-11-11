package com.example.detoxxing.ui.screens

import kotlinx.coroutines.delay
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun BlockedAppsScreen(appsViewModel: AppsViewModel = viewModel()) {
    val context = LocalContext.current
    val allApps by appsViewModel.installedApps.collectAsState()
    val isLoading by appsViewModel.isLoading.collectAsState()
    val dailyLimits by appsViewModel.dailyLimits.collectAsState()

    val remainingTime by appsViewModel.remainingTimeSeconds.collectAsState()

    val blockedApps = allApps.filter { it.isBlocked }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedAppForLimit by remember {
        mutableStateOf<InstalledApp?>(null)
    }
    val hasOverlayPermission = Settings.canDrawOverlays(context)
    val hasUsagePermission = checkUsageStatsPermission(context)

    var showPermissionDialog by remember { mutableStateOf(!hasOverlayPermission || !hasUsagePermission) }

    LaunchedEffect(Unit) {
        appsViewModel.loadApps(context)
        appsViewModel.loadDailyLimits()
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            appsViewModel.loadDailyLimits()
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            containerColor = Color(0xFF2C2C2C),
            title = { Text("Нужны разрешения", color = TextWhite, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Для того чтобы приложение могло блокировать другие программы, ему нужны особые права доступа.",
                    color = TextGray
                )
            },
            confirmButton = {
                Column {
                    if (!hasOverlayPermission) {
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) { Text("Разрешить поверх окон") }
                    }
                    if (!hasUsagePermission) {
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Разрешить статистику") }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Позже", color = TextGray)
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Blocked apps",
                color = TextWhite,
                fontSize = 32.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(Modifier.height(24.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TextWhite)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(blockedApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2C2C2C), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.White, RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedAppForLimit = app
                                    }
                            ) {
                                Text(
                                    text = app.appName,
                                    color = TextWhite,
                                    fontSize = 16.sp
                                )


                                val limit = dailyLimits[app.packageName]
                                val remaining = remainingTime[app.packageName] ?: 0L

                                Text(
                                    text = if (limit != null) {
                                        if (remaining == 0L) {
                                            "Дневной лимит исчерпан"
                                        } else {
                                            "Осталось: ${formatRemainingTime(remaining)}\nЛимит: $limit мин."
                                        }
                                    } else {
                                        "Нажмите, чтобы установить лимит"
                                    },
                                    color = if (limit != null && remaining == 0L) {
                                        Color(0xFFFF6B6B)
                                    } else {
                                        TextGray
                                    },
                                    fontSize = 11.sp
                                )

                            }
                            IconButton(onClick = { appsViewModel.removeAppBlock(app) }) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Unlock",
                                    tint = TextWhite
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showAddDialog = true }
                                .background(Color(0xFF2C2C2C))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(0xFF3D3D3D), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", tint = TextWhite)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(text = "Add", color = TextWhite, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }


    if (showAddDialog) {
        var searchText by remember { mutableStateOf("") }
        val availableApps = allApps.filter { !it.isBlocked && it.appName.contains(searchText, ignoreCase = true) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color(0xFF2C2C2C),
            title = { Text("Выберите приложение", color = TextWhite) },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Поиск...", color = TextGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = TextWhite,
                            unfocusedBorderColor = TextGray
                        ),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )
                    if (availableApps.isEmpty()) {
                        Text("Ничего не найдено.", color = TextGray)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            items(availableApps) { app ->
                                Text(
                                    text = app.appName,
                                    color = TextWhite,
                                    fontSize = 16.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            appsViewModel.addAppBlock(app)
                                            showAddDialog = false
                                        }
                                        .padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Закрыть", color = TextWhite)
                }
            }
        )
    }
    if (selectedAppForLimit != null) {
        var limitText by remember(selectedAppForLimit) {
            mutableStateOf(
                dailyLimits[selectedAppForLimit!!.packageName]?.toString() ?: "60"
            )
        }

        AlertDialog(
            onDismissRequest = {
                selectedAppForLimit = null
            },
            containerColor = Color(0xFF2C2C2C),
            title = {
                Text(
                    "Дневной лимит",
                    color = TextWhite
                )
            },
            text = {
                Column {
                    Text(
                        text = selectedAppForLimit!!.appName,
                        color = TextGray
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = limitText,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() }) {
                                limitText = it
                            }
                        },
                        label = {
                            Text("Минут в день")
                        },
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Например: 60 — один час в день",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val minutes = limitText.toLongOrNull()

                        if (minutes != null && minutes > 0) {
                            appsViewModel.saveDailyLimit(
                                selectedAppForLimit!!,
                                minutes
                            )
                            selectedAppForLimit = null
                        }
                    }
                ) {
                    Text("Сохранить", color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedAppForLimit = null
                    }
                ) {
                    Text("Отмена", color = TextGray)
                }
            }
        )
    }
}

fun formatRemainingTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> "$hours ч. $minutes мин."
        minutes > 0 -> "$minutes мин. $secs сек."
        else -> "$secs сек."
    }
}


fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}