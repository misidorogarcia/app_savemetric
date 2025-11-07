package com.misidoro.app_savemetric.data

enum class Posicion(val id: Int, val key: String, val abbr: String) {
    EDC(1, "extremo_derecho_cerrado", "EDC"),
    EDA(2, "extremo_derecho_abierto", "EDA"),
    LD(3, "lateral_derecho", "LD"),
    PIVOTE_6M(4, "pivote_6m", "6M"),
    LI(5, "lateral_izquierdo", "LI"),
    EIA(6, "extremo_izquierdo_abierto", "EIA"),
    EIC(7, "extremo_izquierdo_cerrado", "EIC"),
    M9(8, "9_metros", "9M"),
    M7(10, "7_metros", "7M"),
    CA(11, "contraataque", "CA"),
    ND(12, "no_definida", "ND")
}