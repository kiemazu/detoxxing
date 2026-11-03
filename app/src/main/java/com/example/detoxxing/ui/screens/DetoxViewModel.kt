package com.example.detoxxing.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.detoxxing.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DetoxSession(
    @SerialName("session_id") val sessionId: Long? = null,
    @SerialName("user_id") val userId: String,
    val status: String = "active",
    @SerialName("points_test") val pointsTest: Int = 0
)

class DetoxViewModel : ViewModel() {

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive

    private val _timeInSeconds = MutableStateFlow(0)
    val timeInSeconds: StateFlow<Int> = _timeInSeconds

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var currentSessionId: Long? = null

    fun startDetox() {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        println("DEBUG: Начало старта сессии для $userId")

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                println("DEBUG: Отправка запроса в БД...")
                val newSession = supabase.from("detox_session").insert(
                    DetoxSession(userId = userId, status = "active")
                ) {
                    select()
                }.decodeSingle<DetoxSession>()

                println("DEBUG: Запрос успешен, ID сессии: ${newSession.sessionId}")

                currentSessionId = newSession.sessionId
                _isSessionActive.value = true
                _timeInSeconds.value = 0

                launch {
                    runTimer()
                }

            } catch (e: Exception) {
                println("DEBUG: ОШИБКА: ${e.message}")
                _error.value = "Ошибка: ${e.message}"
            } finally {
                println("DEBUG: Выключение загрузки")
                _isLoading.value = false
            }
        }
    }

    private suspend fun runTimer() {
        while (_isSessionActive.value) {
            delay(1000L)
            _timeInSeconds.value += 1
        }
    }

    fun finishDetox() {
        val sessionId = currentSessionId ?: return
        val earnedPoints = _timeInSeconds.value

        _isSessionActive.value = false
        viewModelScope.launch {
            try {
                supabase.from("detox_session").update({
                    set("status", "finished")
                    set("points_test", earnedPoints)
                }) {
                    filter { eq("session_id", sessionId) }
                }

                _timeInSeconds.value = 0
                currentSessionId = null

            } catch (e: Exception) {
                _error.value = "Ошибка завершения: ${e.message}"
            }
        }
    }
    override fun onCleared() {
        super.onCleared()

        val sessionId = currentSessionId
        if (_isSessionActive.value && sessionId != null) {

            kotlinx.coroutines.GlobalScope.launch {
                try {
                    supabase.from("detox_session").update({
                        set("status", "finished")
                        set("points_test", _timeInSeconds.value)
                    }) {
                        filter { eq("session_id", sessionId) }
                    }
                } catch (e: Exception) {

                    println("Не удалось сохранить сессию при выходе: ${e.message}")
                }
            }
        }
    }
}