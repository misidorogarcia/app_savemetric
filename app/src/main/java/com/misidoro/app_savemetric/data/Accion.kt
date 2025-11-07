package com.misidoro.app_savemetric.data

/**
 * Acción reutilizable construida continuamente por la app.
 * - portero: id del portero (Int)
 * - tiempo: tiempo en formato "HH:mm:ss" (String)
 * - posicion: posición (Int)
 * - direccion: dirección (Int)
 * - resultado: resultado (Int)
 *
 * NOTA: mutable para permitir reutilización desde un pool.
 */
class Accion(
    var portero: Int = 0,
    var tiempo: String = "00:00:00",
    var posicion: Int = 0,
    var direccion: Int = 0,
    var resultado: Int = 0
) {
    fun reset() {
        portero = 0
        tiempo = "00:00:00"
        posicion = 0
        direccion = 0
        resultado = 0
    }

    override fun toString(): String {
        return "Accion(portero=$portero, tiempo='$tiempo', posicion=$posicion, direccion=$direccion, resultado=$resultado)"
    }
}