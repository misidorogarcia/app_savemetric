package com.misidoro.app_savemetric.data

enum class Resultado(val id: Int, val key: String) {
    GOL(1, "Gol"),
    PARADA_EFECTIVA(2, "Parada y acción válida"),
    PARADA_NULA(3, "Parada y acción anulada"),
    NO_GOL_EFECTIVO(4, "Fuera o poste y acción válida"),
    NO_GOL_NULO(5, "Fuera o poste y acción anulada");

    companion object {
        fun fromId(id: Int): Resultado? = values().firstOrNull { it.id == id }
    }
}