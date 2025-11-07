package com.misidoro.app_savemetric.data

import android.content.Context
import android.preference.PreferenceManager

object MatchCategoriasStore {
    private const val KEY_CATEGORIA = "categoria"

    fun getCategoria(context: Context): String? {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(KEY_CATEGORIA, null)
    }

    fun setCategoria(context: Context, value: String?) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (value == null) {
            prefs.edit().remove(KEY_CATEGORIA).apply()
        } else {
            prefs.edit().putString(KEY_CATEGORIA, value).apply()
        }
    }

    fun hasCategoria(context: Context): Boolean = getCategoria(context) != null
}