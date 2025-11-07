package com.misidoro.app_savemetric.data

import com.google.gson.annotations.SerializedName

data class PorterosByUserRequest(
    @SerializedName("userId") val userId: Int
)

