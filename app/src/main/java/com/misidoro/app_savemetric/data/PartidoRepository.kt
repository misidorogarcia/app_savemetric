package com.misidoro.app_savemetric.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

object PartidoRepository {

    /**
     * Envía el partido al endpoint /api/partidos usando ApiService si el usuario es VIP.
     * Devuelve true si el envío fue exitoso o si el usuario no es VIP.
     */
    suspend fun enviarPartidoSiVip(partido: Partido): Boolean {
        val user = SessionManager.getUser()
        if (user?.vip != true) return true // no es VIP -> tratar como éxito

        val token = user.token ?: return false

        val fechaStr = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(partido.fecha)
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

        val req = PartidoRequest(
            categoria = partido.categoria,
            fecha = fechaStr,
            equipo = partido.equipo,
            rival = partido.rival,
            acciones = accionesReq
        )

        return try {
            withContext(Dispatchers.IO) {
                val api = RetrofitProvider.createService(ApiService::class.java)
                val resp = api.postPartido("Bearer $token", req)
                resp.isSuccessful
            }
        } catch (_: Throwable) {
            false
        }
    }
}