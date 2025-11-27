package com.misidoro.app_savemetric.data

import android.content.Context
import android.preference.PreferenceManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PartidoStore {
    private var partido: Partido? = null

    fun createFromSession(context: Context): Partido {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val categoria = MatchCategoriasStore.getCategoria(context) ?: ""
        val equipo = prefs.getString("equipo", "") ?: ""
        val rival = prefs.getString("rival", "") ?: ""

        val fechaMillis: Long = run {
            val raw = prefs.all["fecha"]
            when (raw) {
                is Long -> raw
                is Int -> raw.toLong()
                is Float -> raw.toLong()
                is Double -> raw.toLong()
                is String -> {
                    // intentar parsear como número (millis)
                    raw.toLongOrNull()
                        // si no es número: intentar parsear como "yyyy-MM-dd" o "yyyy-MM-dd HH:mm"
                        ?: runCatching {
                            val formats = listOf("yyyy-MM-dd", "yyyy-MM-dd HH:mm", "dd/MM/yyyy", "dd-MM-yyyy")
                            var parsed: Long? = null
                            for (f in formats) {
                                try {
                                    val sdf = SimpleDateFormat(f, Locale.getDefault())
                                    val d = sdf.parse(raw)
                                    if (d != null) {
                                        parsed = d.time
                                        break
                                    }
                                } catch (_: Throwable) { /* seguir con el siguiente formato */ }
                            }
                            parsed ?: 0L
                        }.getOrDefault(0L)
                }
                null -> 0L
                else -> {
                    // Fallback seguro: intenta getLong evitando excepción
                    runCatching { prefs.getLong("fecha", 0L) }.getOrDefault(0L)
                }
            }
        }

        partido = Partido(
            categoria = categoria,
            fecha = Date(fechaMillis),
            equipo = equipo,
            rival = rival
        )

        return partido!!
    }

    fun getPartido(): Partido? = partido

    fun clear() {
        partido = null
    }
}