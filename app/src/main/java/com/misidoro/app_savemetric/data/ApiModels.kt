// kotlin
package com.misidoro.app_savemetric.data

import com.google.gson.annotations.SerializedName as GsonSerializedName

// Representa el objeto `user` que viene en la respuesta JSON
data class ApiUser(
    val id: Int,
    val name: String,
    val email: String,
    val apellidos: String?,
    val email_verified_at: String?,
    val created_at: String?,
    val updated_at: String?
)

// Respuesta completa del login según el ejemplo proporcionado
data class LoginResponse(
    val message: String?,
    val user: ApiUser?,
    val token: String?,
    val vip: Boolean?,
    val timestamp: Long?,
    val clave: String?
)

data class AccionRequest(
    @GsonSerializedName("portero") val portero: Int,
    @GsonSerializedName("tiempo") val tiempo: String,
    @GsonSerializedName("posicion") val posicion: Int,
    @GsonSerializedName("direccion") val direccion: Int,
    @GsonSerializedName("resultado") val resultado: Int
)

data class PartidoRequest(
    @GsonSerializedName("categoria") val categoria: String,
    @GsonSerializedName("fecha") val fecha: String, // "yyyy-MM-dd"
    @GsonSerializedName("equipo") val equipo: String,
    @GsonSerializedName("rival") val rival: String,
    @GsonSerializedName("clave") val clave: String,
    @GsonSerializedName("acciones") val acciones: List<AccionRequest>
)