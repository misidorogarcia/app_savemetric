package com.misidoro.app_savemetric.data

class Estadistica(
    var accionesTotales: Int = 0,
    var goles: Int = 0,
    var paradasTotales: Int = 0,
    var paradasValidas: Int = 0,
    var noGolesTotales: Int = 0,
    var noGolesValidos: Int = 0,
    // nuevos campos
    var accionesReales: Int = 0,
    var accionesEfectivas: Int = 0
) {
    fun reset() {
        accionesTotales = 0
        goles = 0
        paradasTotales = 0
        paradasValidas = 0
        noGolesTotales = 0
        noGolesValidos = 0
        accionesReales = 0
        accionesEfectivas = 0
    }

    fun merge(other: Estadistica) {
        accionesTotales += other.accionesTotales
        goles += other.goles
        paradasTotales += other.paradasTotales
        paradasValidas += other.paradasValidas
        noGolesTotales += other.noGolesTotales
        noGolesValidos += other.noGolesValidos
        accionesReales += other.accionesReales
        accionesEfectivas += other.accionesEfectivas
    }

    fun recordGol(amount: Int = 1) {
        accionesTotales += amount
        goles += amount
        // gol cuenta como real y efectiva
        accionesReales += amount
        accionesEfectivas += amount
    }

    fun recordParada(valid: Boolean = true, amount: Int = 1) {
        accionesTotales += amount
        paradasTotales += amount
        if (valid) paradasValidas += amount
        // toda parada cuenta como acción real
        accionesReales += amount
        // solo si es válida (efectiva) incrementa accionesEfectivas
        if (valid) accionesEfectivas += amount
    }

    fun recordNoGol(valid: Boolean = true, amount: Int = 1) {
        accionesTotales += amount
        noGolesTotales += amount
        if (valid) noGolesValidos += amount
        // no se cuentan en accionesReales/efectivas según especificación
    }

    override fun toString(): String {
        return "Estadistica(accionesTotales=$accionesTotales, goles=$goles, paradasTotales=$paradasTotales, paradasValidas=$paradasValidas, noGolesTotales=$noGolesTotales, noGolesValidos=$noGolesValidos, accionesReales=$accionesReales, accionesEfectivas=$accionesEfectivas)"
    }
}