
package com.example.detoxxing.services

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.detoxxing.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class BlockedAppCheck(
    @SerialName("app_id") val appId: Long,
    @SerialName("package_name") val packageName: String
)



@Serializable
data class ScreenTimeLogInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("app_id") val appId: Long,
    val date: String,
    @SerialName("minutes_used") val minutesUsed: Long,
    @SerialName("seconds_used") val secondsUsed: Long
)

@Serializable
data class ScreenTimeLogUpdate(
    @SerialName("minutes_used") val minutesUsed: Long,
    @SerialName("seconds_used") val secondsUsed: Long
)


@Serializable
data class ActiveSessionCheck(
    val status: String
)

@Serializable
data class DailyLimitCheck(
    @SerialName("app_id") val appId: Long,
    @SerialName("max_minutes_per_day") val maxMinutesPerDay: Long,
    @SerialName("is_active") val isActive: Boolean
)

@Serializable
data class ScreenTimeLogCheck(
    @SerialName("log_id") val logId: Long? = null,
    @SerialName("minutes_used") val minutesUsed: Long,
    @SerialName("seconds_used") val secondsUsed: Long = 0L
)

@Serializable
data class DailyLogRecord(
    @SerialName("app_id") val appId: Long,
    @SerialName("minutes_used") val minutesUsed: Long,
    @SerialName("seconds_used") val secondsUsed: Long = 0L
)

class AppBlockerService : Service() {

    private val serviceScope =
        CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var windowManager: WindowManager? = null
    private var blockOverlayView: View? = null

    private var blockedPackages = setOf<String>()
    private var isSessionActive = false

    private var dailyLimitPackages = mapOf<String, Long>()
    private var appIds = mapOf<String, Long>()
    private val dailyUsedSeconds = mutableMapOf<Long, Long>()
    private var lastForegroundApp: String? = null

    private var lastUsageCheckTime = 0L
    private var lastConfigUpdateTime = 0L
    private var lastSaveTime = 0L

    private var currentLogDate = ""

    private fun todayDate(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(Date())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        println("СЛУЖБА ЗАПУЩЕНА!")

        startForeground(1, createNotification())

        windowManager =
            getSystemService(Context.WINDOW_SERVICE) as WindowManager

        startMonitoring()
    }

    private fun createNotification(): Notification {
        val channelId = "app_blocker_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Blocker",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Цифровой детокс активен")
            .setContentText("Следим за вашими приложениями...")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
    }

    private fun startMonitoring() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val now = System.currentTimeMillis()

                    if (now - lastConfigUpdateTime >= 30_000L) {
                        updateBlockedListFromDb()
                        lastConfigUpdateTime = now
                    }

                    val currentApp = getForegroundAppPackage()

                    trackAppUsage(currentApp)

                    if (now - lastSaveTime >= 10_000L) {
                        saveAllUsage()
                        lastSaveTime = now
                    }


                    val sessionShouldBlock =
                        isSessionActive &&
                                currentApp != null &&
                                blockedPackages.contains(currentApp) &&
                                !currentApp.contains("launcher") &&
                                currentApp != "com.example.detoxxing"

                    val dailyLimitReached =
                        currentApp != null &&
                                dailyLimitPackages[currentApp]?.let { limitMinutes ->
                                    val appId = appIds[currentApp]
                                    appId != null &&
                                            (dailyUsedSeconds[appId] ?: 0L) >= limitMinutes * 60L
                                } == true

                    val shouldBlock = sessionShouldBlock || dailyLimitReached

                    if (dailyLimitReached) {
                        println("Достигнут дневной лимит для $currentApp")
                    }


                    withContext(Dispatchers.Main) {
                        if (shouldBlock) {
                            showBlockOverlay()
                        } else {
                            hideBlockOverlay()
                        }
                    }

                } catch (e: Exception) {
                    println(
                        "Ошибка: ${e.message}"
                    )
                }

                delay(1000L)
            }
        }
    }

    private suspend fun updateBlockedListFromDb() {
        val userId = supabase.auth.currentUserOrNull()?.id

        if (userId == null) {
            isSessionActive = false
            blockedPackages = emptySet()
            dailyLimitPackages = emptyMap()
            appIds = emptyMap()
            dailyUsedSeconds.clear()
            lastForegroundApp = null
            return
        }

        try {
            val today = todayDate()

            if (currentLogDate != today) {
                dailyUsedSeconds.clear()
                currentLogDate = today
            }

            val sessions = supabase.from("detox_session")
                .select(columns = Columns.list("status")) {
                    filter {
                        eq("user_id", userId)
                        eq("status", "active")
                    }
                    limit(1)
                }
                .decodeList<ActiveSessionCheck>()

            isSessionActive = sessions.isNotEmpty()

            val apps = supabase.from("blocked_apps")
                .select(
                    columns = Columns.list(
                        "app_id",
                        "package_name"
                    )
                ) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<BlockedAppCheck>()

            appIds = apps
                .filter { it.appId > 0 }
                .associate { it.packageName to it.appId }

            blockedPackages = if (isSessionActive) {
                apps.map { it.packageName }.toSet()
            } else {
                emptySet()
            }

            val limits = supabase.from("daily_limits")
                .select(
                    columns = Columns.list(
                        "app_id",
                        "max_minutes_per_day",
                        "is_active"
                    )
                ) {
                    filter {
                        eq("user_id", userId)
                        eq("is_active", true)
                    }
                }
                .decodeList<DailyLimitCheck>()

            dailyLimitPackages = apps.mapNotNull { app ->
                val limit = limits.find {
                    it.appId == app.appId
                }

                if (limit != null && limit.maxMinutesPerDay > 0) {
                    app.packageName to limit.maxMinutesPerDay
                } else {
                    null
                }
            }.toMap()

            val logs = supabase.from("screen_time_log")
                .select(
                    columns = Columns.list(
                        "app_id",
                        "minutes_used",
                        "seconds_used"
                    )
                ) {
                    filter {
                        eq("user_id", userId)
                        eq("date", today)
                    }
                }
                .decodeList<DailyLogRecord>()
            if (dailyUsedSeconds.isEmpty()) {
                logs.forEach { log ->
                    dailyUsedSeconds[log.appId] =
                        log.minutesUsed * 60L + log.secondsUsed
                }
            }

            println(
                "Детокс активен: $isSessionActive"
            )

            println(
                "Дневные лимиты: $dailyLimitPackages"
            )

        } catch (e: Exception) {
            println(
                "Ошибка загрузки данных: ${e.message}"
            )
        }
    }



    private fun getForegroundAppPackage(): String? {
        try {
            val powerManager =
                getSystemService(Context.POWER_SERVICE) as PowerManager

            if (!powerManager.isInteractive) return null

            val usageStatsManager =
                getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            val currentTime = System.currentTimeMillis()

            val events = usageStatsManager.queryEvents(
                currentTime - 24 * 60 * 60 * 1000L,
                currentTime
            )

            val event = UsageEvents.Event()

            var latestPackage: String? = null
            var latestIsForeground = false

            while (events.hasNextEvent()) {
                events.getNextEvent(event)

                val eventType = event.eventType

                val isForegroundEvent =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        eventType == UsageEvents.Event.ACTIVITY_RESUMED
                    } else {
                        eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                    }

                val isBackgroundEvent =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        eventType == UsageEvents.Event.ACTIVITY_PAUSED
                    } else {
                        eventType == UsageEvents.Event.MOVE_TO_BACKGROUND
                    }

                if (isForegroundEvent) {
                    latestPackage = event.packageName
                    latestIsForeground = true
                } else if (
                    isBackgroundEvent &&
                    event.packageName == latestPackage
                ) {
                    latestIsForeground = false
                }
            }

            if (!latestIsForeground) return null

            if (latestPackage == "com.example.detoxxing") return null

            println("Текущее приложение на экране: $latestPackage")

            return latestPackage

        } catch (e: Exception) {
            println("Ошибка определения приложения: ${e.message}")
            return null
        }
    }



    private suspend fun trackAppUsage(currentApp: String?) {
        val now = System.currentTimeMillis()

        if (lastUsageCheckTime == 0L) {
            lastUsageCheckTime = now
            lastForegroundApp = currentApp
            return
        }

        val elapsedSeconds =
            ((now - lastUsageCheckTime) / 1000L).coerceAtMost(5L)

        lastUsageCheckTime = now
        if (currentApp == null) {
            println(
                "Приложение временно не определено. " +
                        "Предыдущее: $lastForegroundApp"
            )
            return
        }

        val previousApp = lastForegroundApp
        if (
            previousApp != null &&
            dailyLimitPackages.containsKey(previousApp)
        ) {
            val appId = appIds[previousApp]

            if (appId != null) {
                dailyUsedSeconds[appId] =
                    (dailyUsedSeconds[appId] ?: 0L) + elapsedSeconds
            }
        }
        if (currentApp != previousApp) {
            println(
                "Смена приложения: " +
                        "$previousApp -> $currentApp"
            )

            if (previousApp != null) {
                saveUsageForPackage(previousApp)
            }

            lastForegroundApp = currentApp
        }

        val currentId = appIds[currentApp]

        if (
            currentId != null &&
            dailyLimitPackages.containsKey(currentApp)
        ) {
            val seconds = dailyUsedSeconds[currentId] ?: 0L

            println(
                "$currentApp — " +
                        "всего использовано секунд: $seconds"
            )
        }
    }

    private suspend fun saveAllUsage() {
        for (packageName in dailyLimitPackages.keys) {
            saveUsageForPackage(packageName)
        }
    }


    private suspend fun saveUsageForPackage(packageName: String) {
        val appId = appIds[packageName] ?: return
        val totalSeconds = dailyUsedSeconds[appId] ?: return
        val userId = supabase.auth.currentUserOrNull()?.id ?: return

        val today = todayDate()

        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L

        try {
            val existing = supabase.from("screen_time_log")
                .select(
                    columns = Columns.list(
                        "log_id",
                        "minutes_used",
                        "seconds_used"
                    )
                ) {
                    filter {
                        eq("user_id", userId)
                        eq("app_id", appId)
                        eq("date", today)
                    }
                    limit(1)
                }
                .decodeList<ScreenTimeLogCheck>()

            if (existing.isEmpty()) {
                supabase.from("screen_time_log").insert(
                    ScreenTimeLogInsert(
                        userId = userId,
                        appId = appId,
                        date = today,
                        minutesUsed = minutes,
                        secondsUsed = seconds
                    )
                )
            } else {
                supabase.from("screen_time_log").update(
                    ScreenTimeLogUpdate(
                        minutesUsed = minutes,
                        secondsUsed = seconds
                    )
                ) {
                    filter {
                        eq("user_id", userId)
                        eq("app_id", appId)
                        eq("date", today)
                    }
                }
            }

            println(
                "Сохранено для $packageName: " +
                        "$minutes мин. $seconds сек."
            )

        } catch (e: Exception) {
            println(
                "Ошибка сохранения времени: ${e.message}"
            )
        }
    }


    private fun showBlockOverlay() {
        if (blockOverlayView != null) return

        if (!Settings.canDrawOverlays(this)) {
            println(
                "НЕТ РАЗРЕШЕНИЯ НА ОВЕРЛЕЙ!"
            )
            return
        }

        val view = TextView(this).apply {
            text =
                "Идёт цифровой детокс.\n\n" +
                        "Закройте это приложение и вернитесь к своим делам."
            setTextColor(android.graphics.Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER

            setBackgroundColor(
                android.graphics.Color.parseColor("#121212")
            )

            setPadding(50, 50, 50, 50)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },

            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,

            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.CENTER

        try {
            windowManager?.addView(view, params)
            blockOverlayView = view

        } catch (e: Exception) {
            println(
                "Ошибка оверлея: ${e.message}"
            )
        }
    }

    private fun hideBlockOverlay() {
        if (blockOverlayView != null) {
            try {
                windowManager?.removeView(blockOverlayView)
            } catch (e: Exception) {
                println(
                    "Ошибка удаления оверлея: ${e.message}"
                )
            }

            blockOverlayView = null
        }
    }

    override fun onDestroy() {

        serviceScope.launch {
            try {
                saveAllUsage()
            } catch (e: Exception) {
                println(
                    "Ошибка финального сохранения: ${e.message}"
                )
            }
        }

        super.onDestroy()

        if (blockOverlayView != null) {
            try {
                windowManager?.removeView(blockOverlayView)
            } catch (e: Exception) {
            }

            blockOverlayView = null
        }

        serviceScope.cancel()
    }
}
