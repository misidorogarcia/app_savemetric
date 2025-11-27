package com.misidoro.app_savemetric.data

enum class Direcciones(val id: Int, val key: String, val desc: String) {
    SI(1, "SI", desc="Escuadra superior izquierda"),
    CS(2, "CS", desc="Centro superior"),
    SD(3, "SD", desc="Escuadra superior derecha"),
    LI(4, "LI", desc="Medio lateral izquierda"),
    C(5, "C", desc="Centro medio"),
    LD(6, "LD", desc="Medio lateral derecha"),
    II(7, "II", desc="Escuadra inferior izquierda"),
    CI(8, "CI", desc="Centro inferior"),
    ID(9, "ID", desc="Escuadra inferior derecha"),
    VA(10, "VA", desc="Vaselina"),
    RS(11, "RS", desc="Rosca"),
    ND(12, "ND", desc="No definida");

    companion object {
        fun fromId(id: Int): Direcciones? = values().firstOrNull { it.id == id }
    }
}