package com.misidoro.app_savemetric.data

/**
 * Pool sencillo para reutilizar instancias de Accion y reducir GC.
 * - MAX_CAPACITY controla cuantos objetos se guardan.
 * - Métodos synchronized para seguridad básica entre hilos.
 */
object AccionPool {
    private const val MAX_CAPACITY = 256
    private val items = arrayOfNulls<Accion>(MAX_CAPACITY)
    private var size = 0

    @Synchronized
    fun obtain(): Accion {
        return if (size > 0) {
            val idx = --size
            val a = items[idx]!!
            items[idx] = null
            a
        } else {
            Accion()
        }
    }

    @Synchronized
    fun release(accion: Accion) {
        accion.reset()
        if (size < MAX_CAPACITY) {
            items[size++] = accion
        }
        // si el pool está lleno, se deja que el GC recoja la instancia
    }
}

/*
Ejemplo de uso:

val accion = AccionPool.obtain()
accion.portero = 7
accion.tiempo = System.currentTimeMillis()
accion.posicion = 2
accion.direccion = 1
accion.resultado = 0

// procesar acción...

AccionPool.release(accion)
*/