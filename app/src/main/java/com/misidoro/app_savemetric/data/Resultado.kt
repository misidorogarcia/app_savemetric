package com.misidoro.app_savemetric.data

enum class Resultado(val id: Int, val key: String) {
    GOL(1, "gol"),
    PARADA_EFECTIVA(2, "parada_efectiva"),
    PARADA_NULA(3, "parada_nula"),
    NO_GOL_EFECTIVO(4, "no_gol_efectivo"),
    NO_GOL_NULO(5, "no_gol_nulo");

    companion object {
        fun fromId(id: Int): Resultado? = values().firstOrNull { it.id == id }
    }
}