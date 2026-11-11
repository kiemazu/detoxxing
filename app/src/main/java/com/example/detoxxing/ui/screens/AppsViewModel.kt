package com.example.detoxxing.ui.screens

import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.detoxxing.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BlockedApp(
    @SerialName("app_id") val appId: Long? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("app_name") val appName: String,
    @SerialName("package_name") val packageName: String
)

@Serializable
data class DailyLimit(
    @SerialName("user_id")
    val userId: String,

    @SerialName("app_id")
    val appId: Long,

    @SerialName("max_minutes_per_day")
    val maxMinutesPerDay: Long,

    @SerialName("is_active")
    val isActive: Boolean = true
)

@Serializable
data class ScreenTimeUsage(
    @SerialName("app_id")
    val appId: Long,

    @SerialName("minutes_used")
    val minutesUsed: Long,

    @SerialName("seconds_used")
    val secondsUsed: Long = 0L
)


@Serializable
data class DailyLimitRecord(
    @SerialName("app_id")
    val appId: Long,

    @SerialName("max_minutes_per_day")
    val maxMinutesPerDay: Long,

    @SerialName("is_active")
    val isActive: Boolean
)
data class InstalledApp(
    val appName: String,
    val packageName: String,
    val isBlocked: Boolean = false
)

class AppsViewModel : ViewModel() {

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps

    private val _dailyLimits = MutableStateFlow<Map<String, Long>>(emptyMap())
    val dailyLimits: StateFlow<Map<String, Long>> = _dailyLimits


    private val _remainingTimeSeconds =
        MutableStateFlow<Map<String, Long>>(emptyMap())

    val remainingTimeSeconds: StateFlow<Map<String, Long>> =
        _remainingTimeSeconds

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadApps(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val pm = context.packageManager
                val packages = withContext(Dispatchers.IO) {
                    pm.getInstalledApplications(PackageManager.GET_META_DATA)
                }

                val userId = supabase.auth.currentUserOrNull()?.id
                val blockedFromDb = if (userId != null) {
                    supabase.from("blocked_apps")
                        .select(columns = Columns.ALL) { filter { eq("user_id", userId) } }
                        .decodeList<BlockedApp>()
                } else emptyList()

                val blockedPackages = blockedFromDb.map { it.packageName }.toSet()

                val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }

                val launcherPackages = withContext(Dispatchers.IO) {
                    pm.queryIntentActivities(launcherIntent, 0)
                        .map { it.activityInfo.packageName }
                        .toSet()
                }

                val result = packages
                    .filter { it.packageName in launcherPackages }
                    .map { appInfo ->
                        val name = pm.getApplicationLabel(appInfo).toString()

                        InstalledApp(
                            appName = name,
                            packageName = appInfo.packageName,
                            isBlocked = blockedPackages.contains(appInfo.packageName)
                        )
                    }
                    .sortedBy { it.appName }

                _installedApps.value = result
            } catch (e: Exception) {
                println("Ошибка загрузки приложений: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadDailyLimits() {
        viewModelScope.launch {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id
                    ?: return@launch

                val today = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).format(Date())

                val blockedApps = supabase
                    .from("blocked_apps")
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("user_id", userId)
                        }
                    }
                    .decodeList<BlockedApp>()

                val limits = supabase
                    .from("daily_limits")
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("user_id", userId)
                            eq("is_active", true)
                        }
                    }
                    .decodeList<DailyLimitRecord>()

                val usage = supabase
                    .from("screen_time_log")
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("user_id", userId)
                            eq("date", today)
                        }
                    }
                    .decodeList<ScreenTimeUsage>()

                val usedSeconds = usage.associate {
                    it.appId to (it.minutesUsed * 60L + it.secondsUsed)
                }

                val newLimits = mutableMapOf<String, Long>()
                val newRemaining = mutableMapOf<String, Long>()

                blockedApps.forEach { app ->
                    val limit = limits.find {
                        it.appId == app.appId
                    }

                    if (limit != null) {
                        val packageName = app.packageName
                        val totalLimit = limit.maxMinutesPerDay * 60L
                        val used = usedSeconds[limit.appId] ?: 0L

                        newLimits[packageName] = limit.maxMinutesPerDay
                        newRemaining[packageName] =
                            (totalLimit - used).coerceAtLeast(0L)
                    }
                }

                _dailyLimits.value = newLimits
                _remainingTimeSeconds.value = newRemaining



            } catch (e: Exception) {
                println("Ошибка загрузки дневных лимитов: ${e.message}")
            }
        }
    }

    fun saveDailyLimit(app: InstalledApp, minutes: Long) {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: return

        viewModelScope.launch {
            try {
                val blockedApp = supabase
                    .from("blocked_apps")
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("user_id", userId)
                            eq("package_name", app.packageName)
                        }
                    }
                    .decodeList<BlockedApp>()
                    .firstOrNull()
                val appId = blockedApp?.appId
                if (appId == null) {
                    println("Не найден app_id для ${app.appName}")
                    return@launch
                }
                supabase
                    .from("daily_limits")
                    .delete {
                        filter {
                            eq("user_id", userId)
                            eq("app_id", appId)
                        }
                    }
                supabase
                    .from("daily_limits")
                    .insert(
                        DailyLimit(
                            userId = userId,
                            appId = appId,
                            maxMinutesPerDay = minutes,
                            isActive = true
                        )
                    )
                _dailyLimits.value = _dailyLimits.value +
                        (app.packageName to minutes)
                println("Лимит ${app.appName}: $minutes минут")
                loadDailyLimits()
            } catch (e: Exception) {
                println("Ошибка сохранения лимита: ${e.message}")
            }
        }
    }
    fun removeAppBlock(app: InstalledApp) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            try {
                supabase.from("blocked_apps").delete {
                    filter {
                        eq("user_id", userId)
                        eq("package_name", app.packageName)
                    }
                }

                _installedApps.value = _installedApps.value.map {
                    if (it.packageName == app.packageName) it.copy(isBlocked = false) else it
                }
            } catch (e: Exception) {
                println("Ошибка удаления: ${e.message}")
            }
        }
    }

    fun addAppBlock(app: InstalledApp) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            try {
                supabase.from("blocked_apps").insert(
                    BlockedApp(
                        userId = userId,
                        appName = app.appName,
                        packageName = app.packageName
                    )
                )

                _installedApps.value = _installedApps.value.map {
                    if (it.packageName == app.packageName) it.copy(isBlocked = true) else it
                }
            } catch (e: Exception) {
                println("Ошибка добавления: ${e.message}")
            }
        }
    }
}