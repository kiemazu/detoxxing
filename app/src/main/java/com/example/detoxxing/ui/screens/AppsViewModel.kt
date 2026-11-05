package com.example.detoxxing.ui.screens

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

data class InstalledApp(
    val appName: String,
    val packageName: String,
    val isBlocked: Boolean = false
)

class AppsViewModel : ViewModel() {

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps

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

                val result = packages.mapNotNull { appInfo ->
                    if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                        val name = pm.getApplicationLabel(appInfo).toString()
                        InstalledApp(
                            appName = name,
                            packageName = appInfo.packageName,
                            isBlocked = blockedPackages.contains(appInfo.packageName)
                        )
                    } else null
                }.sortedBy { it.appName }

                _installedApps.value = result
            } catch (e: Exception) {
                println("Ошибка загрузки приложений: ${e.message}")
            } finally {
                _isLoading.value = false
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