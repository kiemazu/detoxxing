package com.example.detoxxing.ui.screens

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.detoxxing.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class EmailLookup(val email: String? = null)

class AuthViewModel : ViewModel() {

    // Состояния для UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess
    private val _registerSuccess = MutableStateFlow(false)
    val registerSuccess: StateFlow<Boolean> = _registerSuccess

    fun register(username: String, email: String, password: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _errorMessage.value = "Заполните все поля"
            return
        }

        if (password.length < 6) {
            _errorMessage.value = "Пароль должен быть не менее 6 символов"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        _registerSuccess.value = false

        viewModelScope.launch {
            try {
                // Регистрация в Supabase
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    // Передаем username в метаданные, чтобы сработал триггер в базе
                    this.data = kotlinx.serialization.json.buildJsonObject {
                        put("username", username)
                    }
                }
                _registerSuccess.value = true
            }  catch (e: Exception) {
                val errorText = e.message ?: ""

                _errorMessage.value = when {
                    errorText.contains("user_already_exists") -> "Этот email уже зарегистрирован"
                    errorText.contains("weak_password") -> "Пароль слишком короткий (минимум 6 символов)"
                    errorText.contains("invalid format") || errorText.contains("validation_failed") -> "Неверный формат email (пример: name@mail.com)"
                    errorText.contains("network") || errorText.contains("timeout") -> "Ошибка сети. Проверьте интернет"
                    else -> "Ошибка регистрации. Попробуйте позже"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _errorMessage.value = "Заполните все поля"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        _loginSuccess.value = false

        viewModelScope.launch {
            try {
                // 1. Ищем email по нику
                val userRecord = supabase.from("user")
                    .select(columns = Columns.list("email")) {
                        filter { eq("username", username) }
                    }
                    .decodeSingleOrNull<EmailLookup>()

                if (userRecord?.email == null) {
                    _errorMessage.value = "Пользователь с таким ником не найден"
                    _isLoading.value = false
                    return@launch
                }

                // 2. Входим
                supabase.auth.signInWith(Email) {
                    this.email = userRecord.email
                    this.password = password
                }

                _loginSuccess.value = true // Даем сигнал экрану, что можно переходить

            } catch (e: Exception) {
                val errorText = e.message ?: ""

                // Переводим технические ошибки Supabase на человеческий язык
                _errorMessage.value = when {
                    errorText.contains("invalid_credentials") -> "Неверный логин или пароль"
                    errorText.contains("user_already_exists") -> "Этот email уже зарегистрирован"
                    errorText.contains("weak_password") -> "Пароль слишком короткий (минимум 6 символов)"
                    errorText.contains("Email not confirmed") -> "Пожалуйста, подтвердите вашу почту"
                    errorText.contains("network") || errorText.contains("timeout") -> "Ошибка сети. Проверьте интернет"
                    else -> "Ошибка входа. Попробуйте позже" // Для всех остальных случаев
                }

            } finally {
                _isLoading.value = false
            }
        }
    }

    // Очистка ошибки (например, когда юзер начинает печатать заново)
    fun clearError() {
        _errorMessage.value = null
    }
}