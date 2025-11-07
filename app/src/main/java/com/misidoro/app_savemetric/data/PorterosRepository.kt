package com.misidoro.app_savemetric.data

import retrofit2.Response

class PorterosRepository(
    private val api: PorterosApi = RetrofitProvider.create()
) {
    suspend fun getPorterosForUser(user: User): Result<List<Portero>> {
        return try {
            val req = PorterosByUserRequest(userId = user.id)
            val auth = "Bearer ${user.token}"
            val resp = api.getPorterosByUser(auth, req)
            Result.success(resp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPorteroForUser(user: User, req: CreatePorteroRequest): Result<Response<Portero>> {
        return try {
            val auth = "Bearer ${user.token}"
            val resp = api.createPortero(auth, req)
            Result.success(resp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}