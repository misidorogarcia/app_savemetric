package com.misidoro.app_savemetric.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

object PartidoRepository {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Envía el partido al API sólo si el usuario es VIP.
     * - Si el usuario no es VIP devuelve false (no intenta el envío).
     * - Si ocurre cualquier error de red o la respuesta HTTP NO es 200 lanza IOException.
     * - Si la llamada devuelve 200 retorna true.
     */
    suspend fun enviarPartidoSiVip(partido: Partido): Boolean {
        val user = SessionManager.getUser()
        if (user?.vip != true) {
            // No es VIP: no se envía
            return false
        }

        val token = user.token.ifEmpty { throw IOException("Token de autenticación no disponible") }
        val authHeader = "Bearer $token"

        val fechaStr = try {
            if (partido.fecha.time > 0L) dateFmt.format(partido.fecha) else ""
        } catch (_: Throwable) {
            ""
        }

        val accionesReq = partido.acciones.map { a ->
            AccionRequest(
                portero = a.portero,
                tiempo = a.tiempo,
                posicion = a.posicion,
                direccion = a.direccion,
                resultado = a.resultado
            )
        }

        val request = PartidoRequest(
            categoria = partido.categoria,
            fecha = fechaStr,
            equipo = partido.equipo,
            rival = partido.rival,
            acciones = accionesReq
        )

        return withContext(Dispatchers.IO) {
            val response = RetrofitClient.api.postPartido(authHeader, request)

            // Considerar fallo cualquier código distinto de 200
            if (!response.isSuccessful || response.code() !in setOf(200, 201)) {
                val errorBody = runCatching { response.errorBody()?.string() }.getOrNull()
                throw IOException("Error enviando partido: HTTP ${response.code()} ${errorBody ?: response.message()}")
            }

            true
        }
    }
}