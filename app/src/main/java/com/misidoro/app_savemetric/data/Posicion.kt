package com.misidoro.app_savemetric.data

enum class Posicion(val id: Int, val key: String, val abbr: String) {
    EDC(1, "extremo derecho cerrado", "EDC"),
    EDA(2, "extremo derecho abierto", "EDA"),
    LD(3, "lateral derecho", "LD"),
    PIVOTE_6M(4, "pivote 6m", "6M"),
    LI(5, "lateral izquierdo", "LI"),
    EIA(6, "extremo izquierdo abierto", "EIA"),
    EIC(7, "extremo izquierdo cerrado", "EIC"),
    M9(8, "9 metros", "9M"),
    M7(10, "7 metros", "7M"),
    CA(11, "contraataque", "CA"),
    ND(12, "no definida", "ND")
}