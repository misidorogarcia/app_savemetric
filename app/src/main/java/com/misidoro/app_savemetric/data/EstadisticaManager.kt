package com.misidoro.app_savemetric.data

class EstadisticaManager {
    // totales por portero (key = porteroId, null = total)
    private val totals: MutableMap<Int?, Estadistica> = mutableMapOf()
    // por portero -> por posición
    private val byPorteroPos: MutableMap<Int?, MutableMap<Posicion, Estadistica>> = mutableMapOf()

    fun initForPorteros(porteros: List<Portero>?) {
        totals.clear()
        byPorteroPos.clear()
        val list = porteros ?: emptyList()

        // Inicializar para cada portero y para null (total)
        for (p in list) {
            totals[p.id] = Estadistica()
            byPorteroPos[p.id] = Posicion.values().associateWith { Estadistica() }.toMutableMap()
        }
        totals[null] = Estadistica()
        byPorteroPos[null] = Posicion.values().associateWith { Estadistica() }.toMutableMap()
    }

    /**
     * Registra acción para:
     *  - totales globales por portero (map totals)
     *  - estadística por portero y por posición (byPorteroPos)
     *  - la entrada null se usa como acumulado global (todos los porteros)
     */
    fun recordAccion(porteroId: Int?, pos: Posicion, resultadoId: Int) {
        // totales
        totals[null]?.let { aplicarResultadoAEstadistica(it, resultadoId) }
        if (porteroId != null) totals[porteroId]?.let { aplicarResultadoAEstadistica(it, resultadoId) }

        // por portero + posición
        byPorteroPos[null]?.get(pos)?.let { aplicarResultadoAEstadistica(it, resultadoId) }
        if (porteroId != null) {
            byPorteroPos[porteroId]?.get(pos)?.let { aplicarResultadoAEstadistica(it, resultadoId) }
        }
    }

    private fun aplicarResultadoAEstadistica(estad: Estadistica, resultadoId: Int) {
        estad.accionesTotales += 1
        val r = Resultado.fromId(resultadoId)
        val key = r?.key?.lowercase() ?: ""
        when (key) {
            "gol" -> {
                estad.goles += 1
                estad.accionesReales += 1
                estad.accionesEfectivas += 1
            }
            "parada_efectiva" -> {
                estad.paradasTotales += 1
                estad.paradasValidas += 1
                estad.accionesReales += 1
                estad.accionesEfectivas += 1
            }
            "parada_nula" -> {
                estad.paradasTotales += 1
                estad.accionesReales += 1
            }
            "no_gol_efectivo" -> {
                estad.noGolesTotales += 1
                estad.noGolesValidos += 1
            }
            "no_gol_nulo" -> {
                estad.noGolesTotales += 1
            }
            else -> { /* otros */ }
        }
    }

    fun getForPortero(porteroId: Int?): Estadistica? = totals[porteroId]

    fun getForPorteroAndPos(porteroId: Int?, pos: Posicion): Estadistica? =
        byPorteroPos[porteroId]?.get(pos)

    fun getTotal(): Estadistica? = totals[null]

    fun clear() {
        totals.values.forEach { it.reset() }
        byPorteroPos.values.forEach { m -> m.values.forEach { it.reset() } }
        totals.clear()
        byPorteroPos.clear()
    }
}