package com.misidoro.app_savemetric.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

object PartidoRepository {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun enviarPartidoSiVip(partido: Partido): Boolean {
        val user = SessionManager.getUser()
        if (user?.vip != true) {
            return false
        }

        val token = user.token.ifEmpty { throw IOException("Token de autenticación no disponible") }
        val clave = user.clave?.takeIf { it.isNotBlank() } ?: throw IOException("Clave de usuario no disponible")
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
            clave = clave,
            acciones = accionesReq
        )

        return withContext(Dispatchers.IO) {
            val response = RetrofitClient.api.postPartido(authHeader, request)

            if (!response.isSuccessful || response.code() !in setOf(200, 201)) {
                val errorBody = runCatching { response.errorBody()?.string() }.getOrNull()
                throw IOException("Error enviando partido: HTTP ${response.code()} ${errorBody ?: response.message()}")
            }

            true
        }
    }
}