package com.misidoro.app_savemetric.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object EstadisticaStore {
    private val _global = MutableStateFlow(Estadistica())
    val global: StateFlow<Estadistica> = _global.asStateFlow()

    // versión que incrementamos para forzar recomposición cuando hay cambios
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    private val byPosition = mutableMapOf<Posicion, MutableStateFlow<Estadistica>>()

    private fun snapshot(e: Estadistica): Estadistica {
        return Estadistica(
            accionesTotales = e.accionesTotales,
            goles = e.goles,
            paradasTotales = e.paradasTotales,
            paradasValidas = e.paradasValidas,
            noGolesTotales = e.noGolesTotales,
            noGolesValidos = e.noGolesValidos,
            accionesReales = e.accionesReales,
            accionesEfectivas = e.accionesEfectivas
        )
    }

    @Synchronized
    fun getGlobal(): Estadistica = _global.value

    @Synchronized
    fun getForPosition(pos: Posicion): Estadistica = byPosition.getOrPut(pos) { MutableStateFlow(Estadistica()) }.value

    /**
     * Registra una acción: actualiza la estadística de la posición y la global.
     * Emite nuevos objetos para que StateFlow notifique cambios a la UI.
     */
    @Synchronized
    fun recordAccion(pos: Posicion, accion: Accion) {
        val posFlow = byPosition.getOrPut(pos) { MutableStateFlow(Estadistica()) }
        val posStat = snapshot(posFlow.value)
        val globalStat = snapshot(_global.value)

        // incrementos base
        posStat.accionesTotales += 1
        val newGlobal = snapshot(globalStat).also { it.accionesTotales += 1 }

        val r = Resultado.fromId(accion.resultado)
        val key = r?.key?.lowercase() ?: ""

        when (key) {
            "gol" -> {
                posStat.goles += 1
                newGlobal.goles += 1

                posStat.accionesReales += 1
                posStat.accionesEfectivas += 1
                newGlobal.accionesReales += 1
                newGlobal.accionesEfectivas += 1
            }
            "parada_efectiva" -> {
                posStat.paradasTotales += 1
                posStat.paradasValidas += 1
                newGlobal.paradasTotales += 1
                newGlobal.paradasValidas += 1

                posStat.accionesReales += 1
                posStat.accionesEfectivas += 1
                newGlobal.accionesReales += 1
                newGlobal.accionesEfectivas += 1
            }
            "parada_nula" -> {
                posStat.paradasTotales += 1
                newGlobal.paradasTotales += 1

                posStat.accionesReales += 1
                newGlobal.accionesReales += 1
            }
            "no_gol_efectivo" -> {
                posStat.noGolesTotales += 1
                posStat.noGolesValidos += 1
                newGlobal.noGolesTotales += 1
                newGlobal.noGolesValidos += 1
            }
            "no_gol_nulo" -> {
                posStat.noGolesTotales += 1
                newGlobal.noGolesTotales += 1
            }
            else -> {
                // otros: sólo contabilizados en accionesTotales (ya incrementadas)
            }
        }

        // emitir nuevos objetos para notificación
        posFlow.value = posStat
        _global.value = newGlobal

        // incrementar versión para forzar recomposición donde se observe
        _version.value = _version.value + 1
    }

    @Synchronized
    fun clearPosition(pos: Posicion) {
        byPosition.remove(pos)
        _version.value = _version.value + 1
    }

    @Synchronized
    fun clearAll() {
        _global.value = Estadistica()
        byPosition.values.forEach { it.value = Estadistica() }
        byPosition.clear()
        _version.value = _version.value + 1
    }
}