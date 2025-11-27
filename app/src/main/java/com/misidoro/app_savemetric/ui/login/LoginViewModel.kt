package com.misidoro.app_savemetric.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.misidoro.app_savemetric.data.AuthRepository
import com.misidoro.app_savemetric.data.SessionManager
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var successToken by mutableStateOf<String?>(null)
        private set

    fun onEmailChange(value: String) { email = value }
    fun onPasswordChange(value: String) { password = value }
    fun clearError() { errorMessage = null }

    fun login() {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Email y contraseña son obligatorios"
            return
        }
        if (password.length < 8) {
            errorMessage = "La contraseña debe tener al menos 8 caracteres"
            return
        }
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val result = repository.login(email, password)
            isLoading = false
            result.fold(
                onSuccess = { resp ->
                    val user = SessionManager.saveFromLoginResponse(resp)
                    if (user != null) {
                        successToken = user.token
                    } else if (resp.token != null) {
                        successToken = resp.token
                    } else {
                        errorMessage = resp.message ?: "Usuario y/o contraseña no válidos"
                    }
                },
                onFailure = { throwable ->
                    if (throwable is HttpException && throwable.code() == 401) {
                        errorMessage = "Usuario y/o contraseña no válidos"
                    } else {
                        errorMessage = throwable.message ?: "Error de red"
                    }
                }
            )
        }
    }
}

class LoginViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}