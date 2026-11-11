package com.example.detoxxing.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.detoxxing.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime


@Serializable
data class DetoxSession(
    @SerialName("session_id") val sessionId: Long? = null,
    @SerialName("user_id") val userId: String,
    val status: String = "active",
    @SerialName("points_test") val pointsTest: Int = 0
)
@Serializable
data class ActiveDetoxSession(
    @SerialName("session_id")
    val sessionId: Long,
    @SerialName("start_time")
    val startTime: String
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
    private var timerJob: Job? = null
    fun startDetox(onStarted: () -> Unit) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return

        if (_isLoading.value || _isSessionActive.value) return

        println("DEBUG: Начало старта сессии для $userId")

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {

                supabase.from("detox_session").update({
                    set("status", "finished")
                }) {
                    filter {
                        eq("user_id", userId)
                        eq("status", "active")
                    }
                }

                println("DEBUG: Старые сессии завершены")

                val newSession = supabase.from("detox_session").insert(
                    DetoxSession(userId = userId, status = "active")
                ) {
                    select()
                }.decodeSingle<DetoxSession>()

                println("DEBUG: Новая сессия создана: ${newSession.sessionId}")

                currentSessionId = newSession.sessionId
                _isSessionActive.value = true
                _timeInSeconds.value = 0

                onStarted()

                timerJob?.cancel()
                timerJob = viewModelScope.launch {
                    runTimer()
                }

            } catch (e: Exception) {
                println("DEBUG: ОШИБКА: ${e.message}")
                _error.value = "Ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun finishDetox(onFinished: () -> Unit) {
        val sessionId = currentSessionId ?: return
        val earnedPoints = _timeInSeconds.value / 60

        if (_isLoading.value) return

        _isLoading.value = true

        viewModelScope.launch {
            try {
                supabase.from("detox_session").update({
                    set("status", "finished")
                    set("points_test", earnedPoints)
                }) {
                    filter {
                        eq("session_id", sessionId)
                    }
                }
                timerJob?.cancel()
                timerJob = null

                _isSessionActive.value = false
                _timeInSeconds.value = 0
                currentSessionId = null


                onFinished()

            } catch (e: Exception) {
                _error.value = "Ошибка завершения: ${e.message}"
                println("DEBUG: Ошибка завершения: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun restoreActiveSession(onRestored: () -> Unit) {
        println("RESTORE: Начинаем восстановление")

        val userId = supabase.auth.currentUserOrNull()?.id

        if (userId == null) {
            println("RESTORE: Пользователь не авторизован!")
            return
        }

        if (_isLoading.value || _isSessionActive.value) return

        viewModelScope.launch {
            try {
                println("RESTORE: Ищем активную сессию для $userId")

                val sessions = supabase.from("detox_session")
                    .select(
                        columns = Columns.list("session_id", "start_time")
                    ) {
                        filter {
                            eq("user_id", userId)
                            eq("status", "active")
                        }
                    }.decodeList<ActiveDetoxSession>()

                println("RESTORE: Найдено сессий: ${sessions.size}")

                val session = sessions.firstOrNull()

                if (session == null) {
                    println("RESTORE: Активных сессий нет")
                    return@launch
                }

                println("RESTORE: Найдена сессия ${session.sessionId}")
                println("RESTORE: Время начала ${session.startTime}")

                currentSessionId = session.sessionId

                val startTime = OffsetDateTime.parse(
                    session.startTime
                        .replace(" ", "T")
                        .replace(Regex("([+-]\\d{2})$"), "$1:00")
                ).toInstant()

                val elapsed = Duration.between(
                    startTime,
                    Instant.now()
                ).seconds.coerceAtLeast(0)

                _timeInSeconds.value = elapsed
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()

                _isSessionActive.value = true

                timerJob?.cancel()
                timerJob = viewModelScope.launch {
                    runTimer()
                }

                println("RESTORE: Сессия восстановлена!")
                println("RESTORE: Время таймера ${_timeInSeconds.value}")

                onRestored()

            } catch (e: Exception) {
                println("RESTORE: ОШИБКА: ${e.message}")
                _error.value = "Ошибка восстановления: ${e.message}"
            }
        }
    }
    private suspend fun runTimer() {
        while (_isSessionActive.value) {
            delay(1000L)
            _timeInSeconds.value += 1
        }
    }


}