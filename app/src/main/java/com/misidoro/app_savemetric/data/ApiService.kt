package com.misidoro.app_savemetric.data

import com.google.gson.annotations.SerializedName as GsonSerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

data class RegisterRequest(
    @GsonSerializedName("name") val name: String,
    @GsonSerializedName("apellidos") val apellidos: String?,
    @GsonSerializedName("email") val email: String,
    @GsonSerializedName("password") val password: String,
    @GsonSerializedName("password_confirmation") val passwordConfirmation: String
)

interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): LoginResponse

    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("api/partidos")
    suspend fun postPartido(
        @Header("Authorization") authorization: String,
        @Body request: PartidoRequest
    ): Response<Unit>
}