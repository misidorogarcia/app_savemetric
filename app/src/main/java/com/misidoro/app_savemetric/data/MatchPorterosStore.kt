package com.misidoro.app_savemetric.data

object MatchPorterosStore {
    private val porteros = mutableListOf<Portero>()

    fun setPorteros(list: List<Portero>) {
        porteros.clear()
        porteros.addAll(list)
    }

    fun getPorteros(): List<Portero> = porteros.toList()

    fun clear() {
        porteros.clear()
    }
}