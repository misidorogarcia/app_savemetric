package com.misidoro.app_savemetric.data

class AuthRepository(private val api: ApiService) {
    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val resp = api.login(LoginRequest(email = email, password = password))
            Result.success(resp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        name: String,
        apellidos: String?,
        email: String,
        password: String,
        passwordConfirmation: String
    ): Result<LoginResponse> {
        return try {
            val req = RegisterRequest(
                name = name,
                apellidos = apellidos,
                email = email,
                password = password,
                passwordConfirmation = passwordConfirmation
            )
            val resp = api.register(req)
            Result.success(resp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}