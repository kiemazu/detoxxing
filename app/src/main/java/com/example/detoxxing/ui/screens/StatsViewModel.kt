package com.example.detoxxing.ui.screens

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class AppUsageStat(
    val appName: String,
    val packageName: String,
    val timeInMinutes: Long
)

class StatsViewModel : ViewModel() {
    private val _usageStats = MutableStateFlow<List<AppUsageStat>>(emptyList())
    val usageStats: StateFlow<List<AppUsageStat>> = _usageStats

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadStats(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val stats = withContext(Dispatchers.IO) {
                    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                    val calendar = Calendar.getInstance()
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    val startTime = calendar.timeInMillis
                    val endTime = System.currentTimeMillis()

                    val statsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
                    val pm = context.packageManager
                    val result = mutableListOf<AppUsageStat>()

                    statsList?.forEach { stat ->
                        val timeInMinutes = stat.totalTimeInForeground / 1000 / 60
                        if (timeInMinutes > 0) {
                            val appInfo = pm.getApplicationInfo(stat.packageName, 0)
                            val appName = pm.getApplicationLabel(appInfo).toString()
                            result.add(AppUsageStat(appName, stat.packageName, timeInMinutes))
                        }
                    }

                    result.sortedByDescending { it.timeInMinutes }.take(10)
                }
                _usageStats.value = stats
            } catch (e: Exception) {
                println("Ошибка статистики: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
}