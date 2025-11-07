package com.misidoro.app_savemetric.data

enum class Direcciones(val id: Int, val key: String) {
    SI(1, "SI"),
    CS(2, "CS"),
    SD(3, "SD"),
    LI(4, "LI"),
    C(5, "C"),
    LD(6, "LD"),
    II(7, "II"),
    CI(8, "CI"),
    ID(9, "ID"),
    VA(10, "VA"),
    RS(11, "RS"),
    ND(12, "ND");

    companion object {
        fun fromId(id: Int): Direcciones? = values().firstOrNull { it.id == id }
    }
}