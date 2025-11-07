package com.misidoro.app_savemetric.data

import java.util.Date

data class Partido(
    var categoria: String = "",
    var fecha: Date = Date(0),
    var equipo: String = "",
    var rival: String = "",
    val acciones: MutableList<Accion> = mutableListOf()
) {
    fun addAccion(accion: Accion) {
        acciones.add(accion)
    }

    fun clearAcciones() {
        // liberar acciones al pool y vaciar la lista
        for (a in acciones) {
            AccionPool.release(a)
        }
        acciones.clear()
    }

    fun reset() {
        clearAcciones()
        categoria = ""
        fecha = Date(0)
        equipo = ""
        rival = ""
    }
}