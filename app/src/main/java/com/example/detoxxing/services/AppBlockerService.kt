package com.example.detoxxing.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
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

@Serializable
data class BlockedAppCheck(@SerialName("package_name") val packageName: String)

@Serializable
data class ActiveSessionCheck(val status: String)

class AppBlockerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var windowManager: WindowManager? = null
    private var blockOverlayView: View? = null
    private var blockedPackages = setOf<String>()
    private var isSessionActive = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        println("DEBUG_BLOCKER: СЛУЖБА ЗАПУЩЕНА!")
        startForeground(1, createNotification())
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startMonitoring()
    }

    private fun createNotification(): Notification {
        val channelId = "app_blocker_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "App Blocker", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
                    updateBlockedListFromDb()
                    val currentApp = getForegroundAppPackage()

                    val shouldBlock = isSessionActive &&
                            currentApp != null &&
                            blockedPackages.contains(currentApp) &&
                            !currentApp.contains("launcher") &&
                            currentApp != "com.example.detoxxing"

                    withContext(Dispatchers.Main) {
                        if (shouldBlock) {
                            showBlockOverlay()
                        } else {
                            hideBlockOverlay()
                        }
                    }
                } catch (e: Exception) {
                    println("DEBUG_BLOCKER Ошибка: ${e.message}")
                }
                delay(500)
            }
        }
    }

    private suspend fun updateBlockedListFromDb() {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return

        val sessions = supabase.from("detox_session")
            .select(columns = Columns.list("status")) {
                filter {
                    eq("user_id", userId)
                    eq("status", "active")
                }
                limit(1)
            }.decodeList<ActiveSessionCheck>()

        isSessionActive = sessions.isNotEmpty()

        if (isSessionActive) {
            val apps = supabase.from("blocked_apps")
                .select(columns = Columns.list("package_name")) {
                    filter { eq("user_id", userId) }
                }.decodeList<BlockedAppCheck>()
            blockedPackages = apps.map { it.packageName }.toSet()
        }
    }

    private fun getForegroundAppPackage(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val currentTime = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_DAILY,
            currentTime - 1000 * 10,
            currentTime
        )
        if (stats == null || stats.isEmpty()) return null

        val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
        val topApp = sortedStats.firstOrNull()?.packageName

        if (topApp != null && topApp != "com.example.detoxxing") {
            println("DEBUG_BLOCKER: Текущее приложение на экране: $topApp")
        }

        return topApp
    }

    private fun showBlockOverlay() {
        if (blockOverlayView != null) return

        if (!Settings.canDrawOverlays(this)) {
            println("DEBUG_BLOCKER: НЕТ РАЗРЕШЕНИЯ НА ОВЕРЛЕЙ!")
            return
        }

        val view = TextView(this).apply {
            text = "Идёт цифровой детокс.\n\nЗакройте это приложение и вернитесь к своим делам."
            setTextColor(android.graphics.Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#121212"))
            setPadding(50, 50, 50, 50)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
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
            println("DEBUG_BLOCKER Ошибка оверлея: ${e.message}")
        }
    }

    private fun hideBlockOverlay() {
        if (blockOverlayView != null) {
            try {
                windowManager?.removeView(blockOverlayView)
            } catch (e: Exception) {

            }
            blockOverlayView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        if (blockOverlayView != null) {
            try { windowManager?.removeView(blockOverlayView) } catch (e: Exception) {}
            blockOverlayView = null
        }
    }
}