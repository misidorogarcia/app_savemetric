package com.misidoro.app_savemetric.data

import com.google.gson.annotations.SerializedName

data class CreatePorteroRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("apellidos") val apellidos: String?,
    @SerializedName("fecha_nacimiento") val fecha_nacimiento: String // formato "yyyy-MM-dd"
)

