package com.misidoro.app_savemetric.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface PorterosApi {
    @Headers("Content-Type: application/json")
    @POST("api/porteros/by-user")
    suspend fun getPorterosByUser(
        @Header("Authorization") authorization: String,
        @Body request: PorterosByUserRequest
    ): List<Portero>

    @Headers("Content-Type: application/json")
    @POST("api/porteros")
    suspend fun createPortero(
        @Header("Authorization") authorization: String,
        @Body request: CreatePorteroRequest
    ): Response<Portero>
}