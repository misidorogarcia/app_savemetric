package com.misidoro.app_savemetric.data

data class User(
    val id: Int,
    val name: String,
    val apellidos: String?,
    val email: String,
    val token: String,
    val vip: Boolean,
    val timestamp: Long? = null,
    val clave: String? = null
)

